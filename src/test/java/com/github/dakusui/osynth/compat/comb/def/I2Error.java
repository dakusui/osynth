package com.github.dakusui.osynth.compat.comb.def;

import com.github.dakusui.osynth.compat.comb.model.ExceptionType;

public interface I2Error extends I2 {
  @Override
  default String implementorName() {
    throw new ExceptionType.IntentionalError("I2Error");
  }
}
