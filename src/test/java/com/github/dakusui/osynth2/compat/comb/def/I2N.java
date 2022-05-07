package com.github.dakusui.osynth2.compat.comb.def;

public interface I2N extends I2 {
  @Override
  default String implementorName() {
    return "I2N";
  }
}
