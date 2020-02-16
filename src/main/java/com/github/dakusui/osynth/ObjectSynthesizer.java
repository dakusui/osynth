package com.github.dakusui.osynth;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;

import static com.github.dakusui.osynth.Messages.*;
import static com.github.dakusui.osynth.MethodHandler.equalsHandler;
import static com.github.dakusui.osynth.MethodHandler.hashCodeHandler;
import static com.github.dakusui.osynth.Utils.rethrow;
import static java.util.Objects.requireNonNull;

public class ObjectSynthesizer {
  private final List<Class<?>> interfaces = new LinkedList<>();
  private List<Object> handlerObjects = new LinkedList<>();
  private List<MethodHandler> handlers = new LinkedList<>();

  public ObjectSynthesizer() {
  }

  public ObjectSynthesizer addInterface(Class<?> anInterface) {
    if (!requireNonNull(anInterface).isInterface())
      throw new IllegalArgumentException(notAnInterface(anInterface));
    this.interfaces.add(anInterface);
    return this;
  }

  public static MethodHandler.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandler.builderByNameAndParameterTypes(requireNonNull(methodName), requireNonNull(parameterTypes));
  }

  public ObjectSynthesizer addHandlerObject(Object handlerObject) {
    this.handlerObjects.add(requireNonNull(handlerObject));
    return this;
  }

  public ObjectSynthesizer handle(MethodHandler handler) {
    this.handlers.add(requireNonNull(handler));
    return this;
  }

  public Object synthesize() {
    return this.synthesize(Object.class);
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize(Class<T> anInterface) {
    requireNonNull(anInterface);
    if (this.interfaces.stream().noneMatch(anInterface::isAssignableFrom)) {
      throw new IllegalArgumentException(noMatchingInterface(anInterface, interfaces));
    }
    return (T) this.createProxyFactory().create();
  }

  private ProxyFactory createProxyFactory() {
    this.handlerObjects.add(new Object());
    ProxyDescriptor descriptor = new ProxyDescriptor(interfaces.toArray(new Class<?>[0]), new ArrayList<>(handlers), handlerObjects);
    this.handle(hashCodeHandler(descriptor));
    this.handle(equalsHandler(descriptor));
    return new ProxyFactory(descriptor);
  }

  public static class ProxyDescriptor {
    private final Class<?>[] interfaces;
    private final List<? extends MethodHandler> handlers;
    private final List<Object> handlerObjects;

    public ProxyDescriptor(Class<?>[] interfaces, List<? extends MethodHandler> handlers, List<Object> handlerObjects) {
      this.interfaces = interfaces;
      this.handlers = handlers;
      this.handlerObjects = handlerObjects;
    }

    @Override
    public int hashCode() {
      return handlers.hashCode();
    }

    @Override
    public boolean equals(Object anotherObject) {
      if (!(anotherObject instanceof ProxyDescriptor))
        return false;
      ProxyDescriptor another = (ProxyDescriptor) anotherObject;
      return this == another ||
          (Arrays.equals(this.interfaces, another.interfaces) &&
              this.handlers.equals(another.handlers) &&
              this.handlerObjects.equals(another.handlerObjects));
    }
  }

  private static class ProxyFactory {
    private final ProxyDescriptor descriptor;
    private final Map<Method, BiFunction<Object, Object[], Object>> methodHandlersCache;
    private final Map<Class<?>, MethodHandles.Lookup> lookups;

    private ProxyFactory(ProxyDescriptor descriptor) {
      this.descriptor = descriptor;
      this.methodHandlersCache = new HashMap<>();
      this.lookups = new HashMap<>();
      Arrays.stream(this.descriptor.interfaces).forEach(this::lookup);
    }

    private MethodHandles.Lookup lookup(Class<?> anInterface) {
      return this.lookups.computeIfAbsent(anInterface, ProxyFactory::createLookup);
    }

    private static MethodHandles.Lookup createLookup(Class<?> anInterface) {
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

    @SuppressWarnings({"Convert2MethodRef"})
    Object create() {
      return Proxy.newProxyInstance(
          ProxyFactory.class.getClassLoader(),
          this.descriptor.interfaces,
          (proxy, method, args) -> handleMethodCall(proxy, method, args)
      );
    }

    private Object handleMethodCall(Object proxy, Method method, Object[] args) {
      return lookUpMethodCallHandler(method).apply(proxy, args);
    }

    private BiFunction<Object, Object[], Object> lookUpMethodCallHandler(Method method) {
      if (!this.methodHandlersCache.containsKey(method)) {
        this.methodHandlersCache.put(method, createMethodCallHandler(method));
      }
      return this.methodHandlersCache.get(method);
    }

    private BiFunction<Object, Object[], Object> createMethodCallHandler(Method method) {
      return this.descriptor.handlers.stream()
          .filter(handler -> handler.test(method))
          .map(handler -> (BiFunction<Object, Object[], Object>) handler)
          .findFirst()
          .orElseGet(
              () -> this.descriptor.handlerObjects.stream()
                  .filter(h -> hasMethod(h.getClass(), method))
                  .map(h -> (BiFunction<Object, Object[], Object>) (Object o, Object[] o2) -> invokeMethod(h, method, o2))
                  .findFirst()
                  .orElseGet(() -> {
                    if (method.isDefault())
                      return defaultMethodInvoker(method);
                    throw new IllegalArgumentException(incompatibleFallbackObject(this.descriptor.handlerObjects, method));
                  }));
    }

    private boolean hasMethod(Class<?> aClass, Method method) {
      return Arrays.stream(aClass.getMethods())
          .anyMatch(
              m -> m.getName().equals(method.getName()) &&
                  Arrays.equals(m.getParameterTypes(), method.getParameterTypes()));
    }

    private BiFunction<Object, Object[], Object> defaultMethodInvoker(Method method) {
      return (self, args) -> invokeDefaultMethodInDeclaringInterface(self, method, args);
    }

    private Object invokeDefaultMethodInDeclaringInterface(Object proxy, Method method, Object[] args) {
      Class<?> declaringInterface = method.getDeclaringClass();
      try {
        return lookup(declaringInterface)
            .in(declaringInterface)
            .unreflectSpecial(method, declaringInterface)
            .bindTo(proxy)
            .invokeWithArguments(args);
      } catch (Throwable throwable) {
        throw rethrow(throwable);
      }
    }

    private static Object invokeMethod(Object object, Method method, Object[] args) {
      try {
        try {
          return method.invoke(object, args);
        } catch (InvocationTargetException e) {
          throw e.getTargetException();
        }
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}
