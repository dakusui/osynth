package com.github.dakusui.osynth.comb.def;

import com.github.dakusui.osynth.comb.model.ExceptionType;

public interface I2CheckedException extends I2 {
  @Override
  default String implementorName() throws ExceptionType.IntentionalCheckedException {
    throw new ExceptionType.IntentionalCheckedException("I2CheckedExceptipon");
  }
}
