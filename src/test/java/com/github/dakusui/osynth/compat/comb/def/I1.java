package com.github.dakusui.osynth.compat.comb.def;

public interface I1 extends I {
  @Override
  default String apply0_1() {
    return String.format("apply0_1:I1:%s", this.getClass().getCanonicalName());
  }

  @Override
  default String apply0_both() {
    return String.format("apply0_both:I1:%s", this.getClass().getCanonicalName());
  }
}
