package com.github.dakusui.osynth;

import java.lang.reflect.Method;
import java.util.List;

import static java.lang.String.format;

public enum Messages {
  ;

  static String incompatibleFallbackObject(Object fallbackObject, Method method) {
    return format(
        "Fallback object:'%s' is not assignable to '%s', which declares requested method:'%s'",
        fallbackObject,
        method.getDeclaringClass().getCanonicalName(),
        method);
  }

  static <T> String noMatchingInterface(Class<T> anInterface, List<Class<?>> interfaces) {
    return format("No matching interface was found for '%s' in '%s'.",
        anInterface.getCanonicalName(),
        interfaces);
  }

  public static String failedToInstantiate(Class<?> anInterface) {
    return format(
        "Failed to create a method handles lookup for interface:'%s'. Probably, it is prohibited on the interface.",
        anInterface.getCanonicalName());
  }

  public static String notAnInterface(Class<?> anInterface) {
    return format("A given class:'%s' is not an interface.", anInterface);
  }
}
