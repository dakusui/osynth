package com.github.dakusui.objsynth;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ObjectSynthesizer<T> {
  private final Class<T>            anInterface;
  private       Object              fallbackObject;
  private       List<MethodHandler> handlers = new LinkedList<>();

  public ObjectSynthesizer(Class<T> anInterface) {
    this.anInterface = anInterface;
  }

  public static <T> ObjectSynthesizer<T> create(Class<T> anInterface) {
    return new ObjectSynthesizer<>(anInterface);
  }

  public static MethodHandler.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandler.builderByNameAndParameterTypes(methodName, parameterTypes);
  }

  public ObjectSynthesizer<T> fallbackTo(Object fallbackObject) {
    this.fallbackObject = fallbackObject;
    return this;
  }

  public ObjectSynthesizer<T> handle(MethodHandler handler) {
    handlers.add(handler);
    return this;
  }

  public T synthesize() {
    return build().synthesize();
  }

  protected ObjectFactory<T> build() {
    this.handle(
        // a default for 'equals' method.
        methodCall("equals", Object.class).with(
            (self, objects) -> self == objects[0] || fallbackObject.equals(objects[0])
        ));
    return new ObjectFactory<>(this.anInterface, new ArrayList<>(handlers), fallbackObject);
  }

  private static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    if (e instanceof Error)
      throw (Error) e;
    throw new RuntimeException(e);
  }

  /**
   * A factory class to synthesize an implementation of a given interface (semi-)automatically.
   *
   * @param <T> A class of an interface for which an implementation is to be synthesized.
   */
  public static class ObjectFactory<T> {
    private final Class<T>                                          anInterface;
    private final List<? extends MethodHandler>                     handlers;
    private final Map<Method, BiFunction<Object, Object[], Object>> handlersCache;
    private final Object                                            fallbackObject;
    private final Map<Class<?>, MethodHandles.Lookup>               lookups;

    public T synthesize() {
      return createProxy();
    }

    protected ObjectFactory(Class<T> anInterface, List<? extends MethodHandler> handlers, Object fallbackObject) {
      this.anInterface = Objects.requireNonNull(anInterface);
      this.handlers = handlers;
      this.handlersCache = new HashMap<>();
      this.fallbackObject = fallbackObject;
      this.lookups = new HashMap<>();
      this.lookup(anInterface);
    }

    private MethodHandles.Lookup lookup(Class<? extends T> anInterface) {
      return this.lookups.computeIfAbsent(anInterface, ObjectFactory::createLookup);
    }

    private static MethodHandles.Lookup createLookup(Class<?> anInterface) {
      try {
        Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(anInterface);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        throw rethrow(e);
      }
    }

    @SuppressWarnings({ "unchecked", "Convert2MethodRef" })
    private T createProxy() {
      return (T) Proxy.newProxyInstance(
          anInterface.getClassLoader(),
          new Class[] { anInterface },
          (proxy, method, args) -> handleMethodCall(proxy, method, args)
      );
    }

    private Object handleMethodCall(Object proxy, Method method, Object[] args) {
      return lookUpMethodCallHandler(method).orElseThrow(UnsupportedOperationException::new).apply(proxy, args);
    }

    private Optional<? extends BiFunction<Object, Object[], Object>> lookUpMethodCallHandler(Method method) {
      if (this.handlersCache.containsKey(method)) {
        return Optional.of(this.handlersCache.get(method));
      }
      Optional<? extends BiFunction<Object, Object[], Object>> ret = createMethodCallingFunction(method);
      if (ret.isPresent())
        this.handlersCache.put(method, ret.get());
      else
        this.handlersCache.put(method, null);
      return ret;
    }

    private Optional<? extends BiFunction<Object, Object[], Object>> createMethodCallingFunction(Method method) {
      Optional<? extends BiFunction<Object, Object[], Object>> ret = handlers.stream().filter(handler -> handler.test(method)).findFirst();
      return ret.isPresent() ?
          ret :
          Optional.of(createMethodCallingFunctionOnFallback(method));
    }

    private BiFunction<Object, Object[], Object> createMethodCallingFunctionOnFallback(Method method) {
      return (self, args) -> invokeMethod(self, fallbackObject, method, args);
    }

    @SuppressWarnings("unchecked")
    private Object invokeMethod(Object proxy, Object fallbackObject, Method method, Object[] args) {
      try {
        boolean wasAccessible = method.isAccessible();
        method.setAccessible(true);
        try {
          Class<?> declaringClass = method.getDeclaringClass();
          if (method.isDefault() && !isOverriddenIn(method, fallbackObject.getClass())) {
            return lookup((Class<? extends T>) declaringClass)
                .in(declaringClass)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
          }
          return method.invoke(fallbackObject, args);
        } finally {
          method.setAccessible(wasAccessible);
        }
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }

    private static boolean isOverriddenIn(Method method, Class<?> aClass) {
      if (method.getDeclaringClass() == aClass)
        return false;
      try {
        return aClass.getDeclaredMethod(method.getName(), method.getParameterTypes()) != null;
      } catch (NoSuchMethodException e) {
        return Stream.concat(
            Stream.of(aClass.getSuperclass()),
            Stream.of(aClass.getInterfaces()))
            .filter(Objects::nonNull)
            .anyMatch(k -> isOverriddenIn(method, k));
      }
    }
  }
}
