package com.github.dakusui.osynth2.core.utils;

import com.github.dakusui.osynth2.core.MethodSignature;
import com.github.dakusui.osynth2.core.SynthesizedObject;

import static java.lang.String.format;

public enum MessageUtils {
  ;

  public static String failedToInstantiate(Class<?> anInterface) {
    return format(
        "Failed to create a method handles lookup for interface:'%s'. Probably, it is prohibited for the interface by your platform.",
        anInterface.getCanonicalName());
  }

  public static String formatMessageForMissingMethodHandler(MethodSignature methodSignature, SynthesizedObject synthesizedObject, NoSuchMethodException e) {
    return format("An appropriate method handler/implementation for '%s' was not found in '%s': %s", methodSignature, synthesizedObject, e.getMessage());
  }
}
