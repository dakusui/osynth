package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.compat.utils.AssertionInCatchClauseFinished;
import com.github.dakusui.osynth.compat.utils.UtBase;
import com.github.dakusui.osynth.compat.utils.UtUtils;
import com.github.dakusui.osynth.compat.utils.InternalUtils;
import org.junit.Test;

import java.io.IOException;

import static com.github.dakusui.crest.Crest.asObject;
import static com.github.dakusui.crest.Crest.assertThat;

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
          UtUtils.rootCause(e),
          asObject()
              .equalTo(checkedException)
              .$());
      AssertionInCatchClauseFinished.assertionInCatchClauseFinished();
    }
  }
}
