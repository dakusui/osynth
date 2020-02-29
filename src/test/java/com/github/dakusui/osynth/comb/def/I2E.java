package com.github.dakusui.osynth.comb.def;

public interface I2E extends I {
  @Override
  default String implementorName() {
    throw new RuntimeException("I2E");
  }
}
