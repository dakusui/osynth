package com.github.dakusui.osynth;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.Messages.*;
import static com.github.dakusui.osynth.MethodHandler.*;
import static com.github.dakusui.osynth.Utils.rethrow;
import static com.github.dakusui.pcond.Preconditions.requireArgument;
import static com.github.dakusui.pcond.functions.Predicates.isInstanceOf;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class ObjectSynthesizer {
  public static final  FallbackHandlerFactory DEFAULT_FALLBACK_HANDLER_FACTORY = desc -> (Method method) -> Optional.empty();
  private static final Method                 DESCRIPTOR_METHOD                = retrieveDescriptorMethod();
  private final        List<Class<?>>         interfaces                       = new LinkedList<>();
  private              List<Object>           handlerObjects                   = new LinkedList<>();
  private              List<MethodHandler>    handlers                         = new LinkedList<>();
  private              FallbackHandlerFactory fallbackHandlerFactory;

  public ObjectSynthesizer() {
    this.fallbackHandlerFactory(DEFAULT_FALLBACK_HANDLER_FACTORY);
  }

  public ObjectSynthesizer addInterface(Class<?> anInterface) {
    if (!requireNonNull(anInterface).isInterface())
      throw new IllegalArgumentException(notAnInterface(anInterface));
    if (!this.interfaces.contains(anInterface))
      this.interfaces.add(anInterface);
    return this;
  }

  /**
   * In a situation, where a method in the {@code handlerObject} calls another method in the interface directly or indirectly
   * and you intend to override the callee method's behavior by the {@link ObjectSynthesizer}, it results in a counter-intuitive
   * behavior.
   * <p>
   * Since implementation of a default method is "synthesized" in an implementation class when the implementing class is compiled,
   * call on the callee method from the caller is invoked directly on "this", not through the dynamic proxy.
   * Hence the method handler is ignored even if it is given to the object synthesize for the callee method.
   * <p>
   * Best practice to avoid this is not to add a handler object that implements an interface that has default methods.
   * That is,
   * 1. Create a separated interface S that contains only no default methods and extend it by your original interface.
   * 2. Instantiate an object that implements S and add it to the synthesizer through this method.
   * Another approach is,
   * Create an object that has a public method (or methods) you desire to override its behavior and add it to the synthesizer through this method.
   * The second approach is easier but a bit more error-prone since it cannot rely on the compiler for the correctness of the method's signature.
   *
   * @param handlerObject An object that handles method invocation
   * @return This object
   */
  public ObjectSynthesizer addHandlerObject(Object handlerObject) {
    this.handlerObjects.add(requireNonNull(handlerObject));
    return this;
  }

  public ObjectSynthesizer handle(MethodHandler handler) {
    this.handlers.add(requireNonNull(handler));
    return this;
  }

  public ObjectSynthesizer fallbackHandlerFactory(FallbackHandlerFactory fallbackHandlerFactory) {
    this.fallbackHandlerFactory = requireNonNull(fallbackHandlerFactory);
    return this;
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize() {
    return (T) this.synthesize(Object.class);
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize(Class<T> anInterface) {
    requireNonNull(anInterface);
    if (this.interfaces.stream().noneMatch(anInterface::isAssignableFrom)) {
      throw new IllegalArgumentException(noMatchingInterface(anInterface, interfaces));
    }
    return (T) this.createProxyFactory(this.createProxyDescriptor()).create();
  }

  @SuppressWarnings("unchecked")
  public <T> T resynthesizeFrom(T base) {
    Synthesized synthesized = (Synthesized) requireArgument(base, isNotNull().and(isInstanceOf(Synthesized.class)));
    return (T) createProxyFactory(
        synthesized.descriptor()
            .overrideWith(this.createProxyDescriptor()))
        .create();
  }

  private ProxyFactory createProxyFactory(ProxyDescriptor proxyDescriptor) {
    return new ProxyFactory(proxyDescriptor);
  }

  private ProxyDescriptor createProxyDescriptor() {
    return createProxyDescriptor(interfaces, handlers, handlerObjects, fallbackHandlerFactory);
  }

  protected ProxyDescriptor createProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects, FallbackHandlerFactory fallbackHandlerFactory) {
    return new ProxyDescriptor(
        interfaces,
        handlers,
        handlerObjects,
        fallbackHandlerFactory);
  }

  public static ObjectSynthesizer create(boolean auto) {
    return auto ?
        new ObjectSynthesizer() {
          @Override
          public ObjectSynthesizer addHandlerObject(Object handlerObject) {
            requireNonNull(handlerObject);
            for (Class<?> eachInterface : handlerObject.getClass().getInterfaces())
              addInterface(eachInterface);
            return super.addHandlerObject(handlerObject);
          }
        } :
        new ObjectSynthesizer();
  }

  public static MethodHandler.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandler.builderByNameAndParameterTypes(requireNonNull(methodName), requireNonNull(parameterTypes));
  }


  private static Method retrieveDescriptorMethod() {
    try {
      return Synthesized.class.getMethod("descriptor");
    } catch (NoSuchMethodException e) {
      throw rethrow(e);
    }
  }

  @FunctionalInterface
  public interface FallbackHandlerFactory extends Function<ProxyDescriptor, Function<Method, Optional<BiFunction<Object, Object[], Object>>>> {
    default FallbackHandlerFactory compose(FallbackHandlerFactory before) {
      return proxyDescriptor -> method -> {
        Optional<BiFunction<Object, Object[], Object>> ret = before.apply(proxyDescriptor).apply(method);
        if (ret.isPresent())
          return ret;
        return FallbackHandlerFactory.this.apply(proxyDescriptor).apply(method);
      };
    }
  }

  public static class ProxyDescriptor {
    private final List<Class<?>>         interfaces;
    private final List<MethodHandler>    handlers;
    private final List<MethodHandler>    builtInHandlers;
    private final List<Object>           handlerObjects;
    private final FallbackHandlerFactory fallbackHandlerFactory;

    public ProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects, FallbackHandlerFactory fallbackHandlerFactory) {
      this.interfaces = interfaces;
      this.handlers = handlers;
      this.handlerObjects = handlerObjects;
      this.interfaces.add(Describable.class);
      this.builtInHandlers = asList(
          hashCodeHandler(this),
          equalsHandler(this, describeIfPossible()),
          toStringHandler(this, v -> "proxy:" + v.toString()),
          builderByNameAndParameterTypes("describe").with((self, args) -> this)
      );
      this.fallbackHandlerFactory = fallbackHandlerFactory;
    }

    private Function<Object, Object> describeIfPossible() {
      return v -> v instanceof Describable ?
          ((Describable) v).describe() :
          v;
    }

    Stream<MethodHandler> streamAllHandlers() {
      return Stream.concat(this.handlers.stream(), this.builtInHandlers.stream());
    }

    @Override
    public int hashCode() {
      return handlers.hashCode();
    }

    @Override
    public boolean equals(Object anotherObject) {
      if (this == anotherObject)
        return true;
      ProxyDescriptor another;
      if ((anotherObject instanceof ProxyDescriptor)) {
        another = (ProxyDescriptor) anotherObject;
        return this.interfaces().equals(another.interfaces()) &&
            this.handlers().equals(another.handlers()) &&
            this.handlerObjects().equals(another.handlerObjects()) &&
            this.fallbackHandlerFactory.equals(another.fallbackHandlerFactory);
      }
      return false;
    }

    @Override
    public String toString() {
      return "osynth:" + this.getClass().getCanonicalName() + "@" + System.identityHashCode(this);
    }

    protected List<Class<?>> interfaces() {
      return unmodifiableList(this.interfaces);
    }

    protected List<MethodHandler> handlers() {
      return unmodifiableList(this.handlers);
    }

    protected List<Object> handlerObjects() {
      return unmodifiableList(this.handlerObjects);
    }

    public BiFunction<Object, Object[], Object> fallbackHandler(Method method) {
      return this.fallbackHandlerFactory.apply(this)
          .apply(method)
          .orElseThrow(() -> new IllegalArgumentException(noHandlerFound(this.handlerObjects, method)));
    }

    public ProxyDescriptor overrideWith(ProxyDescriptor proxyDescriptor) {
      return new ProxyDescriptor(
          Stream.concat(proxyDescriptor.interfaces().stream(), this.interfaces().stream()).distinct().collect(toList()),
          Stream.concat(proxyDescriptor.handlers().stream(), this.handlers().stream()).distinct().collect(toList()),
          Stream.concat(proxyDescriptor.handlerObjects().stream(), this.handlerObjects().stream()).distinct().collect(toList()),
          this.fallbackHandlerFactory.compose(proxyDescriptor.fallbackHandlerFactory));
    }
  }

  public interface Describable {
    ProxyDescriptor describe();
  }

  private static class ProxyFactory {
    private final ProxyDescriptor                                   descriptor;
    private final Map<Method, BiFunction<Object, Object[], Object>> methodHandlersCache;
    private final Map<Class<?>, MethodHandles.Lookup>               lookups;

    private ProxyFactory(ProxyDescriptor descriptor) {
      this.descriptor = descriptor;
      this.methodHandlersCache = new HashMap<>();
      this.lookups = new HashMap<>();
      this.descriptor.interfaces.forEach(this::lookupObjectFor);
    }

    private MethodHandles.Lookup lookupObjectFor(Class<?> anInterface) {
      return this.lookups.computeIfAbsent(anInterface, ProxyFactory::createLookup);
    }

    private static synchronized MethodHandles.Lookup createLookup(Class<?> anInterface) {
      Constructor<MethodHandles.Lookup> constructor;
      try {
        constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
        constructor.setAccessible(true);
        try {
          return constructor.newInstance(anInterface);
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        }
      } catch (Throwable e) {
        throw new RuntimeException(failedToInstantiate(anInterface), e);
      }
    }

    @SuppressWarnings({ "Convert2MethodRef" })
    Object create() {
      return Proxy.newProxyInstance(
          ProxyFactory.class.getClassLoader(),
          Stream.concat(this.descriptor.interfaces.stream(), Stream.of(Synthesized.class)).distinct().toArray(Class<?>[]::new),
          (proxy, method, args) -> handleMethodCall(proxy, method, args)
      );
    }

    private Object handleMethodCall(Object proxy, Method method, Object[] args) {
      if (DESCRIPTOR_METHOD.equals(method))
        return this.descriptor;
      return lookUpMethodCallHandler(method).apply(proxy, args);
    }

    private BiFunction<Object, Object[], Object> lookUpMethodCallHandler(Method method) {
      if (!this.methodHandlersCache.containsKey(method)) {
        method.setAccessible(true);
        this.methodHandlersCache.put(method, createMethodCallHandler(method));
      }
      return this.methodHandlersCache.get(method);
    }

    private BiFunction<Object, Object[], Object> createMethodCallHandler(Method method) {
      return this.descriptor.streamAllHandlers()
          .filter(handler -> handler.test(method))
          .map(handler -> (BiFunction<Object, Object[], Object>) handler)
          .findFirst()
          .orElseGet(
              () -> this.descriptor.handlerObjects().stream()
                  .filter(h -> hasMethod(h.getClass(), method))
                  .map(h -> (BiFunction<Object, Object[], Object>) (Object o, Object[] o2) -> invokeMethod(h, method, o2))
                  .findFirst()
                  .orElseGet(() -> {
                    try {
                      Optional<Method> methodOptional = this.descriptor.interfaces()
                          .stream()
                          .map(each -> getMethodFrom(method, each))
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .filter(Method::isDefault)
                          .findFirst();
                      if (methodOptional.isPresent())
                        return defaultMethodInvoker(methodOptional.get());
                      return invokeFallbackHandler(method, descriptor);
                    } catch (Throwable t) {
                      throw rethrow(t);
                    }
                  }));
    }

    private Optional<Method> getMethodFrom(Method method, Class<?> each) {
      try {
        return Optional.of(each.getMethod(method.getName(), method.getParameterTypes()));
      } catch (NoSuchMethodException e) {
        return Optional.empty();
      }
    }

    private static BiFunction<Object, Object[], Object> invokeFallbackHandler(Method method, ProxyDescriptor descriptor) {
      return descriptor.fallbackHandler(method);
    }

    private boolean hasMethod(Class<?> aClass, Method method) {
      return Arrays.stream(aClass.getMethods())
          .anyMatch(
              m -> m.getName().equals(method.getName()) &&
                  Arrays.equals(m.getParameterTypes(), method.getParameterTypes()));
    }

    private BiFunction<Object, Object[], Object> defaultMethodInvoker(Method method) throws IllegalAccessException {
      MethodHandle preparedMethodHandle = prepareMethodHandle(method);
      return (self, args) -> invokeDefaultMethodInDeclaringInterface(self, args, preparedMethodHandle);
    }

    private MethodHandle prepareMethodHandle(Method method) throws IllegalAccessException {
      Class<?> declaringInterface = method.getDeclaringClass();
      return lookupObjectFor(declaringInterface)
          .in(declaringInterface)
          .unreflectSpecial(method, declaringInterface);
    }

    private Object invokeDefaultMethodInDeclaringInterface(Object proxy, Object[] args, MethodHandle preparedMethodHandle) {
      try {
        return preparedMethodHandle.bindTo(proxy).invokeWithArguments(args);
      } catch (Throwable throwable) {
        throw rethrow(throwable);
      }
    }

    private static Object invokeMethod(Object object, Method method, Object[] args) {
      try {
        try {
          method = findMethodInObjectIfNecessary(object, method);
          boolean wasAccessible = method.isAccessible();
          method.setAccessible(true);
          try {
            return method.invoke(object, args);
          } finally {
            method.setAccessible(wasAccessible);
          }
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        }
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }

    private static Method findMethodInObjectIfNecessary(Object object, Method method) throws NoSuchMethodException {
      assert object.getClass().getMethod(method.getName(), method.getParameterTypes()) != null : String.format("Method:%s presence check in %s should have been done already", method, object.getClass());
      if (method.getDeclaringClass().isInstance(object))
        return method;
      method = object.getClass().getMethod(method.getName(), method.getParameterTypes());
      return method;
    }
  }
}
