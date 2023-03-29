package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.exceptions.OsynthException;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.fluent.Fluents.objectValue;
import static com.github.dakusui.pcond.forms.Printables.predicate;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;
import static com.github.dakusui.thincrest.TestFluents.assertStatement;

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
      assertStatement(
          objectValue(e).toObject(v -> v)
              .transform(tx -> tx.toObject(v -> (Throwable) v).toObject(throwableGetCause())
                  .then()
                  .checkWithPredicate(objectIsSameReferenceAs(original)).done())
              .transform(tx -> tx.toObject(v -> (Throwable) v).toString(throwableGetMessage())
                  .then()
                  .isEqualTo(new IOException().toString()).done())); // By definition of Throwable#<init>(Throwable), this is the expected string.
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
      assertStatement(
          objectValue(e)
              .toObject(Throwable::getCause)
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
      assertStatement(objectValue(e)
          .asObject().toObject(v -> (Throwable) v)
          .toObject(Throwable::getCause)
          .then()
          .isInstanceOf(TestException.class));
      throw e;
    }
  }
}
