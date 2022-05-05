package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;

public interface MethodMatcher {
  default boolean matches(Method m) {
    return matches(MethodSignature.create(m));
  }

  boolean matches(MethodSignature s);
}
