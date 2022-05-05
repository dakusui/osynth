package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.Synthesized;
import com.github.dakusui.osynth.utils.InternalUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.utils.InternalUtils.rethrow;
import static com.github.dakusui.osynth2.core.utils.MessageUtils.messageForInstantiationFailure;

public class ProxyFactory {
  public static final Method                                            DESCRIPTOR_METHOD = retrieveDescriptorMethod();
  private final       ProxyDescriptor                                   descriptor;
  private final       Map<Method, BiFunction<Object, Object[], Object>> methodHandlersCache;
  private final       Map<Class<?>, MethodHandles.Lookup>               lookups;

  public ProxyFactory(ProxyDescriptor descriptor) {
    this.descriptor = descriptor;
    this.methodHandlersCache = new HashMap<>();
    this.lookups = new HashMap<>();
    this.descriptor.interfaces().forEach(anInterface -> lookupObjectFor(anInterface, this.lookups));
  }

  private static Method retrieveDescriptorMethod() {
    try {
      return Synthesized.class.getMethod("osynthProxyDescriptor");
    } catch (NoSuchMethodException e) {
      throw rethrow(e);
    }
  }

  private static MethodHandles.Lookup lookupObjectFor(Class<?> anInterface, Map<Class<?>, MethodHandles.Lookup> lookups) {
    return lookups.computeIfAbsent(anInterface, ProxyFactory::createMethodHandlesLookupFor);
  }

  public static synchronized MethodHandles.Lookup createMethodHandlesLookupFor(Class<?> anInterfaceClass) {
    Constructor<MethodHandles.Lookup> constructor;
    try {
      constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
      constructor.setAccessible(true);
      try {
        return constructor.newInstance(anInterfaceClass);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    } catch (Throwable e) {
      throw new RuntimeException(messageForInstantiationFailure(anInterfaceClass), e);
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
      this.methodHandlersCache.put(method, createMethodHandlerFor(method));
    }
    return this.methodHandlersCache.get(method);
  }

  private BiFunction<Object, Object[], Object> createMethodHandlerFor(Method method) {
    return this.descriptor.streamAllHandlers()
        .filter((MethodHandler methodHandler) -> methodHandler.test(method))
        .map(handler -> (BiFunction<Object, Object[], Object>) handler)
        .findFirst()
        .orElseGet(
            () -> this.descriptor.handlerObjects().stream()
                .filter((Object handlerObject) -> classHasMethod(handlerObject.getClass(), method))
                .map((Object handlerObject) -> createMethodHandlerForMethod(method, handlerObject))
                .findFirst()
                .orElseGet(() -> createMethodHandlerForMethodFromLookups(method, this.lookups, this.descriptor.interfaces(), this.descriptor)));
  }

  private static BiFunction<Object, Object[], Object> createMethodHandlerForMethod(Method method, Object handlerObject) {
    return (Object o, Object[] args) -> invokeMethod(handlerObject, method, args);
  }

  private static BiFunction<Object, Object[], Object> createMethodHandlerForMethodFromLookups(Method method, Map<Class<?>, MethodHandles.Lookup> lookups, List<Class<?>> interfaces, ProxyDescriptor descriptor) {
    try {
      Optional<Method> methodOptional = interfaces
          .stream()
          .map((Class<?> eachClass) -> InternalUtils.getMethodFrom(method, eachClass))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(Method::isDefault)
          .findFirst();
      if (methodOptional.isPresent())
        return defaultMethodInvoker(methodOptional.get(), lookups);
      return invokeFallbackHandler(method, descriptor);
    } catch (Throwable t) {
      throw rethrow(t);
    }
  }

  private static BiFunction<Object, Object[], Object> invokeFallbackHandler(Method method, ProxyDescriptor descriptor) {
    return descriptor.fallbackMethodHandlerFor(method);
  }

  private static boolean classHasMethod(Class<?> aClass, Method method) {
    return Arrays.stream(aClass.getMethods())
        .anyMatch(
            m -> m.getName().equals(method.getName()) &&
                Arrays.equals(m.getParameterTypes(), method.getParameterTypes()));
  }

  private static BiFunction<Object, Object[], Object> defaultMethodInvoker(Method method, Map<Class<?>, MethodHandles.Lookup> lookups) throws IllegalAccessException {
    MethodHandle preparedMethodHandle = prepareMethodHandle(method, lookups);
    return (self, args) -> invokeDefaultMethodInDeclaringInterface(self, args, preparedMethodHandle);
  }

  private static MethodHandle prepareMethodHandle(Method method, Map<Class<?>, MethodHandles.Lookup> lookups) throws IllegalAccessException {
    Class<?> declaringInterface = method.getDeclaringClass();
    return lookupObjectFor(declaringInterface, lookups)
        .in(declaringInterface)
        .unreflectSpecial(method, declaringInterface);
  }

  private static Object invokeDefaultMethodInDeclaringInterface(Object proxy, Object[] args, MethodHandle preparedMethodHandle) {
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
    if (method.getDeclaringClass().isInstance(object))
      return method;
    method = object.getClass().getMethod(method.getName(), method.getParameterTypes());
    return method;
  }
}
