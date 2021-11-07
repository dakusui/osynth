package com.github.dakusui.osynth.compat.core;

import com.github.dakusui.osynth.compat.Synthesized;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.utils.InternalUtils.rethrow;
import static com.github.dakusui.osynth.utils.Messages.failedToInstantiate;

public class ProxyFactory {
  public static final Method                                            DESCRIPTOR_METHOD = retrieveDescriptorMethod();
  private final       ProxyDescriptor                                   descriptor;
  private final       Map<Method, BiFunction<Object, Object[], Object>> methodHandlersCache;
  private final       Map<Class<?>, MethodHandles.Lookup>               lookups;

  public ProxyFactory(ProxyDescriptor descriptor) {
    this.descriptor = descriptor;
    this.methodHandlersCache = new HashMap<>();
    this.lookups = new HashMap<>();
    this.descriptor.interfaces().forEach(this::lookupObjectFor);
  }

  private static Method retrieveDescriptorMethod() {
    try {
      return Synthesized.class.getMethod("osynthProxyDescriptor");
    } catch (NoSuchMethodException e) {
      throw rethrow(e);
    }
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
  public Object create() {
    return Proxy.newProxyInstance(
        ProxyFactory.class.getClassLoader(),
        Stream.concat(this.descriptor.interfaces().stream(), Stream.of(Synthesized.class)).distinct().toArray(Class<?>[]::new),
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

  private static Object invokeDefaultMethodInDeclaringInterface(Object proxy, Object[] args, MethodHandle methodHandle) {
    try {
      return methodHandle.bindTo(proxy).invokeWithArguments(args);
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
    if (method.getDeclaringClass().isInstance(object))
      return method;
    method = object.getClass().getMethod(method.getName(), method.getParameterTypes());
    return method;
  }
}
