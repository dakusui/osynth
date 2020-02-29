package com.github.dakusui.osynth.comb.def;

public interface I1E extends I {
  @Override
  default String implementorName() {
    throw new RuntimeException("I1E");
  }
}
