package com.github.dakusui.osynth.utils;

import java.lang.reflect.Method;
import java.util.Optional;

public enum InternalUtils {
  ;

  public static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
      throw (RuntimeException) e;
    if (e instanceof Error)
      throw (Error) e;
    throw new RuntimeException(e);
  }

  public static Optional<Method> getMethodFrom(Method method, Class<?> klass) {
    try {
      return Optional.of(klass.getMethod(method.getName(), method.getParameterTypes()));
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }
}
