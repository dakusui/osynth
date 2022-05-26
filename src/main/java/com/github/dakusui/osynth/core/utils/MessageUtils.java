package com.github.dakusui.osynth.core.utils;

import com.github.dakusui.osynth.core.MethodSignature;
import com.github.dakusui.osynth.core.SynthesizedObject;

import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public enum MessageUtils {
  ;

  public static String messageForMissingMethodHandler(MethodSignature methodSignature, Object object, NoSuchMethodException e) {
    return format("An appropriate method handler/implementation for '%s' was not found in '%s': %s", methodSignature, object, e.getMessage());
  }
  public static String messageForMissingMethodHandler(String methodName, Class<?>[] parameterTypes, Object object, NoSuchMethodException e) {
    return messageForMissingMethodHandler(MethodSignature.create(methodName, parameterTypes), object, e);
  }

  public static <T> String messageForAttemptToCastToUnavailableInterface(Class<T> classInUse, List<Class<?>> interfaces) {
    return format("Tried to cast to '%s' but available interfaces are only: %s", classInUse, interfaces);
  }

  public static String messageForReservedMethodOverridingValidationFailure(List<Object> methodSignatures) {
    return String.format("Reserved methods cannot be overridden. : %n%s",
        methodSignatures.stream()
            .map(Object::toString)
            .collect(joining(format("%n- "), "- ", format("%n"))));
  }
}
