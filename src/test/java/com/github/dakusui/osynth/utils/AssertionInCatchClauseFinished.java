package com.github.dakusui.osynth.utils;

public class AssertionInCatchClauseFinished extends RuntimeException {
  public static AssertionInCatchClauseFinished assertionInCatchClauseFinished() {
    throw new AssertionInCatchClauseFinished();
  }
}
