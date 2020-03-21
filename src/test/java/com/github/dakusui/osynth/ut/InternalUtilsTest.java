package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.utils.AssertionInCatchClauseFinished;
import com.github.dakusui.osynth.utils.InternalUtils;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;

import java.io.IOException;

import static com.github.dakusui.crest.Crest.asObject;
import static com.github.dakusui.crest.Crest.assertThat;
import static com.github.dakusui.osynth.utils.AssertionInCatchClauseFinished.assertionInCatchClauseFinished;
import static com.github.dakusui.osynth.utils.UtUtils.rootCause;

public class InternalUtilsTest extends UtBase {
  @Test(expected = Error.class)
  public void givenError$whenRethrown$thenErrorIsThrown() {
    throw InternalUtils.rethrow(new Error());
  }

  @Test(expected = RuntimeException.class)
  public void givenCheckedException$whenRethrown$thenRuntimeExceptionThrown() {
    throw InternalUtils.rethrow(new IOException());
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void givenCheckedException$whenRethrown$thenCheckedExceptionWrapped() {
    Exception checkedException = new IOException();
    try {
      throw InternalUtils.rethrow(checkedException);
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
