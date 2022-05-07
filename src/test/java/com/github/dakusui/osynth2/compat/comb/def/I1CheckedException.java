package com.github.dakusui.osynth2.compat.comb.def;

import com.github.dakusui.osynth2.compat.comb.model.ExceptionType;

public interface I1CheckedException extends I1 {
  @Override
  default String implementorName() throws ExceptionType.IntentionalCheckedException {
    throw new ExceptionType.IntentionalCheckedException("I2CheckedException");
  }
}
