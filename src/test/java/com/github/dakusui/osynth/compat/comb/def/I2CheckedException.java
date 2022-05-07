package com.github.dakusui.osynth.compat.comb.def;

import com.github.dakusui.osynth.compat.comb.model.ExceptionType;

public interface I2CheckedException extends I2 {
  @Override
  default String implementorName() throws ExceptionType.IntentionalCheckedException {
    throw new ExceptionType.IntentionalCheckedException("I2CheckedExceptipon");
  }
}
