package com.github.dakusui.osynth.ut.core.utils;

import com.github.dakusui.osynth.core.utils.MethodUtils;
import com.github.dakusui.osynth.exceptions.OsynthException;
import org.junit.Test;

import java.lang.reflect.Method;

import static com.github.dakusui.pcond.Fluents.value;
import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.isInstanceOf;

public class MethodUtilsTest extends UtBase {
  @Test(expected = OsynthException.class)
  public void testExecuteMethod$whenBlockThrowsCheckedException$thenOsynthExceptionRethrown() {
    class TestCheckedException extends Exception {
    }
    MethodUtils.FailableSupplier<String> block = () -> {
      throw new TestCheckedException();
    };
    try {
      MethodUtils.execute(block);
    } catch (OsynthException e) {
      e.printStackTrace();
      assertThat(e.getCause(), isInstanceOf(TestCheckedException.class));
      throw e;
    }
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetMethodFromClass$whenMethodNotFound$thenUnsupportedOperationExceptionRethrown() {
    try {
      MethodUtils.getMethodFromClass(Object.class, "toString", String.class);
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      assertThat(e,
          when().as((UnsupportedOperationException) value())
              .exercise(Throwable::getMessage)
              .then().asString()
              .isEqualTo("An appropriate method handler/implementation for 'toString(String)' was not found in 'class java.lang.Object': java.lang.Object.toString(java.lang.String)"));
      throw e;
    }
  }
}
