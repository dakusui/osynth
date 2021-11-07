package com.github.dakusui.osynth.utils;

import com.github.dakusui.osynth.core.MethodHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static com.github.dakusui.osynth.utils.Messages.failedToInstantiate;

public enum InternalUtils {
  ;

  public static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    if (e instanceof Error)
      throw (Error) e;
    throw new RuntimeException(e);
  }

  public static Optional<MethodHandle> toMethodHandle(Method method) {
    if (Modifier.isAbstract(method.getModifiers()))
      return Optional.empty();
    Class<?> declaringInterface = method.getDeclaringClass();
    try {
      return Optional.of(createMethodHandleLookup(declaringInterface)
          .in(declaringInterface)
          .unreflectSpecial(method, declaringInterface));
    } catch (IllegalAccessException e) {
      throw rethrow(e);
    }
  }

  public static MethodHandles.Lookup createMethodHandleLookup(Class<?> anInterface) {
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

  public static Object invokeMethod(Method method, Object object, Object[] args) {
    try {
      return method.invoke(object, (Object[]) args);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw rethrow(e);
    }
  }

  public static MethodHandler createMethodHandlerFor(final Object object, Method method) {
    return (synthesizedObject, objects) -> invokeMethod(method, object, objects);
  }

  public static MethodHandler toMethodHandler(MethodHandle methodHandle) {
    return (synthesizedObject, objects) -> {
      try {
        return methodHandle.bindTo(synthesizedObject)
            .invoke(objects);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    };
  }
}
