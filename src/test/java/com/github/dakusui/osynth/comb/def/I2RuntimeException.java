package com.github.dakusui.osynth.comb.def;

import com.github.dakusui.osynth.comb.model.ExceptionType;

public interface I2RuntimeException extends I2 {
  @Override
  default String implementorName() {
    throw new ExceptionType.IntentionalRuntimeException("I2RuntimeException");
  }
}
