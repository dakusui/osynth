package com.github.dakusui.osynth;

public class AssertionInCatchClauseFinished extends RuntimeException {
  static AssertionInCatchClauseFinished assertionInCatchClauseFinished() {
    throw new AssertionInCatchClauseFinished();
  }
}
