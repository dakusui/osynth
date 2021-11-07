package com.github.dakusui.osynth.core;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface MethodHandler extends BiFunction<SynthesizedObject, Object[], Object> {
  interface Factory extends Supplier<MethodHandler> {
  }
}
