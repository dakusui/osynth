package com.github.dakusui.osynth.comb.def;

public interface I2 extends I {
  @Override
  default String apply0_2() {
    return String.format("apply0_2:%s", this.getClass().getCanonicalName());
  }

  @Override
  default String apply0_both() {
    return String.format("apply0_both:%s", this.getClass().getCanonicalName());
  }
}
