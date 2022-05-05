package com.github.dakusui.osynth2.core;

import java.util.function.BiFunction;

@FunctionalInterface
public interface MethodHandler {

  Object apply(SynthesizedObject synthesizedObject, Object[] args) throws Throwable;
}
