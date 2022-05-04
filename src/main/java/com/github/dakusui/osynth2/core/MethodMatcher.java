package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;

public interface MethodMatcher {
  boolean matches(Method m);
}
