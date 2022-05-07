package com.github.dakusui.osynth.compat.utils;

public class AssertionInCatchClauseFinished extends RuntimeException {
  public static AssertionInCatchClauseFinished assertionInCatchClauseFinished() {
    throw new AssertionInCatchClauseFinished();
  }
}
