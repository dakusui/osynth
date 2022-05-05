package com.github.dakusui.osynth2.core;

@FunctionalInterface
public interface MethodHandler {

  Object handle(SynthesizedObject synthesizedObject, Object[] args) throws Throwable;
}
