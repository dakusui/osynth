package com.github.dakusui.osynth.comb.def;

import com.github.dakusui.osynth.comb.model.ExceptionType;

public interface I1Error extends I1 {
  @Override
  default String implementorName() {
    throw new ExceptionType.IntentionalError("I1Error");
  }
}
