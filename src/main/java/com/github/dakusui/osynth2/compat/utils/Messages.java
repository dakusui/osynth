package com.github.dakusui.osynth2.compat.utils;

import java.lang.reflect.Method;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public enum Messages {
  ;

  public static String noHandlerFound(Object fallbackObject, Method method) {
    return format(
        "No appropriate handler for the requested method:'%s' was found in fallback object:'%s'",
        method,
        fallbackObject);
  }

  public static String notAnInterface(Class<?> anInterface) {
    return format("A given class:'%s' is not an interface.", anInterface);
  }
}
