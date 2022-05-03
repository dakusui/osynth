package com.github.dakusui.osynth.utils;

import com.github.dakusui.osynth3.core.MethodSignature;
import com.github.dakusui.osynth3.core.SynthesizedObject;

import java.lang.reflect.Method;
import java.util.List;

import static java.lang.String.format;

public enum Messages {
  ;

  public static String noHandlerFound(Object fallbackObject, Method method) {
    return format(
        "No appropriate handler for the requested method:'%s' was found in fallback object:'%s'",
        method,
        fallbackObject);
  }

  public static <T> String noMatchingInterface(Class<T> anInterface, List<Class<?>> interfaces) {
    return format("No matching interface was found for '%s' in '%s'.",
        anInterface.getCanonicalName(),
        interfaces);
  }

  public static String failedToInstantiate(Class<?> anInterface) {
    return format(
        "Failed to create a method handles lookup for interface:'%s'. Probably, it is prohibited for the interface by your platform.",
        anInterface.getCanonicalName());
  }

  public static String notAnInterface(Class<?> anInterface) {
    return format("A given class:'%s' is not an interface.", anInterface);
  }

  public static String formatMessageForMissingMethodHandler(MethodSignature methodSignature, SynthesizedObject synthesizedObject, NoSuchMethodException e) {
    return format("An appropriate method handler/implementation for '%s' was not found in '%s': %s", methodSignature, synthesizedObject, e.getMessage());
  }
}
