package com.github.dakusui.osynth2.compat.utils;

public class AssertionInCatchClauseFinished extends RuntimeException {
  public static AssertionInCatchClauseFinished assertionInCatchClauseFinished() {
    throw new AssertionInCatchClauseFinished();
  }
}
