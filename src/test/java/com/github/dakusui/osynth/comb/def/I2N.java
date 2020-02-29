package com.github.dakusui.osynth.comb.def;

public interface I2N extends I {
  @Override
  default String implementorName() {
    return "I2N";
  }
}
