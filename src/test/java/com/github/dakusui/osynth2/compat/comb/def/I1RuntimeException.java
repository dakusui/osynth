package com.github.dakusui.osynth2.compat.comb.def;

import com.github.dakusui.osynth2.compat.comb.model.ExceptionType;

public interface I1RuntimeException extends I1 {
  @Override
  default String implementorName() {
    throw new ExceptionType.IntentionalRuntimeException("I1RuntimeException");
  }
}
