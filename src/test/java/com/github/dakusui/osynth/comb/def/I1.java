package com.github.dakusui.osynth.comb.def;

public interface I1 extends I {
  @Override
  default String apply0_1() {
    return String.format("apply0_1:%s", this.getClass().getCanonicalName());
  }

  @Override
  default String apply0_both() {
    return String.format("apply0_both:%s", this.getClass().getCanonicalName());
  }
}
