package com.github.dakusui.osynth;

import org.junit.Test;

import java.io.IOException;

import static com.github.dakusui.crest.Crest.asObject;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.AssertionInCatchClauseFinished.assertionInCatchClauseFinished;
import static com.github.dakusui.osynth.UtUtils.rootCause;

public class UtilsTest {
  @Test(expected = Error.class)
  public void givenError$whenRethrown$thenErrorIsThrown() {
    throw Utils.rethrow(new Error());
  }

  @Test(expected = RuntimeException.class)
  public void givenCheckedException$whenRethrown$thenRuntimeExceptionThrown() {
    throw Utils.rethrow(new IOException());
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void givenCheckedException$whenRethrown$thenCheckedExceptionWrapped() {
    Exception checkedException = new IOException();
    try {
      throw Utils.rethrow(checkedException);
    } catch (RuntimeException e) {
      assertThat(
          rootCause(e),
          asObject()
              .equalTo(checkedException)
              .$());
      assertionInCatchClauseFinished();
    }
  }
}
