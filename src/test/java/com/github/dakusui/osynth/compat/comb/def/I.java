package com.github.dakusui.osynth.compat.comb.def;

import static java.lang.String.format;

public interface I {
  default String apply0() throws Throwable {
    return format("apply0() on %s", implementorName());
  }

  default String apply1(int a0) throws Throwable {
    return format("apply1(%s) on %s", a0, implementorName());
  }

  default String apply2(int a0, int a1) throws Throwable {
    return format("apply2(%s,%s) on %s", a0, a1, implementorName());
  }

  String apply0_1() throws Throwable;
  String apply0_2() throws Throwable;
  String apply0_both() throws Throwable;

  String implementorName() throws Throwable;
}
