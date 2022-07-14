package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.exceptions.OsynthException;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.Fluents.*;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.pcond.forms.Printables.predicate;

public class OsynthExceptionTest extends UtBase {
  @Test
  public void whenFromMethodWithOsynthException$thenTheSameInstanceRethrown() {
    OsynthException original = new OsynthException(null, null);
    try {
      throw OsynthException.from(null, original);
    } catch (OsynthException e) {
      e.printStackTrace();
      assertThat(e, predicate("isSameObject[System.identityHash->" + System.identityHashCode(original) + "]", v -> v == original));
    }
  }

  @Test
  public void whenFromMethodWithCheckedException$thenTheSameInstanceRethrown() {
    IOException original = new IOException();
    try {
      throw OsynthException.from(null, original);
    } catch (OsynthException e) {
      e.printStackTrace();
      assertThat(e,
          when().asObject().thenWith(allOf(
              valueOfClass(Throwable.class).asObject()
                  .exercise(throwableGetCause())
                  .then()
                  .verifyWith(objectIsSameReferenceAs(original)),
              valueOfClass(Throwable.class).asObject()
                  .exercise(throwableGetMessage())
                  .then().asString()
                  .isEqualTo(new IOException().toString())))); // By definition of Throwable#<init>(Throwable), this is the expected string.
    }
  }

  @Test(expected = OsynthException.class)
  public void givenInvocationTargetException$whenOsynthExceptionFromIsCalled$thenOsynthExceptionHoldingTargetExceptionIsThrown() {
    class TestException extends Exception {
    }
    try {
      throw OsynthException.from("helloWorld", new InvocationTargetException(new TestException()));
    } catch (OsynthException e) {
      e.printStackTrace();
      assertThat(e, when().asObject().cast(OsynthException.class)
          .exercise(Throwable::getCause)
          .then()
          .isInstanceOf(TestException.class));
      throw e;
    }
  }

  @Test(expected = OsynthException.class)
  public void givenOsynthExceptionHoldingNonNullCause$whenOsynthExceptionFromIsCalled$thenOsynthExceptionHoldingOriginalCauseIsThrown() {
    class TestException extends Exception {
    }
    try {
      throw OsynthException.from("helloWorld", new OsynthException(new TestException()));
    } catch (OsynthException e) {
      e.printStackTrace();
      assertThat(e, when().asObject().cast(OsynthException.class)
          .exercise(Throwable::getCause)
          .then()
          .isInstanceOf(TestException.class));
      throw e;
    }
  }
}
