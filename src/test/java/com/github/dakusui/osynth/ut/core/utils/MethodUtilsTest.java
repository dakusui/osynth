package com.github.dakusui.osynth.ut.core.utils;

import com.github.dakusui.osynth.core.utils.MethodUtils;
import com.github.dakusui.osynth.exceptions.OsynthException;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.github.dakusui.osynth.core.utils.MethodUtils.prettierToString;
import static com.github.dakusui.pcond.Fluents.value;
import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.*;

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

  @Test
  public void givenAnonymousClassObject$whenPrettierToString$then_anonymous$$$_() {
    Object v = new Object() {
    };

    System.out.println(MethodUtils.simpleClassNameOf(v.getClass()));

    assertThat(
        v,
        when().asObject()
            .exercise(MethodUtils::prettierToString)
            .then().asString()
            .startsWith("anonymous:()@"));
  }

  @Test
  public void givenObjectToStringOverridden$whenPrettierToString$thenOverridingMethdChosen() {
    Object v = new Object() {
      @Override
      public String toString() {
        return "HelloWorld";
      }
    };

    System.out.println(MethodUtils.simpleClassNameOf(v.getClass()));

    assertThat(
        v,
        when().asObject()
            .exercise(MethodUtils::prettierToString)
            .then().asString()
            .startsWith("HelloWorld"));
  }

  @Test
  public void givenNull$whenPrettierToString$then_null_() {
    Object v = null;

    assertThat(
        v,
        when().asObject()
            .exercise(MethodUtils::prettierToString)
            .then().asString()
            .isEqualTo("null"));
  }

  @Test
  public void givenLambda$whenPrettierString$thenExplained() {
    Function<Object, Object> v = x -> x;
    assertThat(
        v,
        when().asObject()
            .exercise(MethodUtils::prettierToString)
            .then().asString()
            .startsWith("lambda:(Function):declared in com.github.dakusui.osynth.ut.core.utils.MethodUtilsTest"));
  }

  @Test
  public void givenLambdaInAnonymous$whenPrettierString$thenExplained() {
    AtomicReference<Object> atomicReference = new AtomicReference<>();
    Object v = new Object() {
      @Override
      public String toString() {
        Function<Object, Object> v = x -> x;
        atomicReference.set(v);
        return prettierToString(v);
      }
    };
    System.out.println((Function<Object,Object>)x -> v);
    System.out.println(v.toString());
    System.out.println(atomicReference.get());

    assertThat(v.toString(), startsWith("lambda:(Function):declared in"));
  }
}
