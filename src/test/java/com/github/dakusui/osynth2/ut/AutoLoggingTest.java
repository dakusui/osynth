package com.github.dakusui.osynth2.ut;

import com.github.dakusui.osynth2.compat.utils.UtBase;
import com.github.dakusui.osynth2.ObjectSynthesizer;
import com.github.dakusui.pcond.forms.Functions;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.osynth2.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class AutoLoggingTest extends UtBase {
  static class TestException extends RuntimeException {
    TestException(String message) {
      super(message);
    }
  }

  interface A {
    String aMethod();

    default String bMethod() {
      return "default:bMethod";
    }

    String cMethod();

    default String eMethod() {
      throw new TestException("Hello, RuntimeException");
    }
  }

  @Test(expected = TestException.class)
  public void enableAutoLogging() {
    A aobj = new ObjectSynthesizer()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackObject(newObjectImplementingAForFallback())
        .enableAutoLogging()
        .addInterface(A.class)
        .synthesize()
        .castTo(A.class);

    List<String> out = new LinkedList<>();
    try {
      out.add(aobj.aMethod());
      out.add(aobj.bMethod());
      out.add(aobj.cMethod());
      out.add(aobj.eMethod());
    } catch (TestException e) {
      e.printStackTrace();
      out.forEach(System.err::println);
      assertThat(
          out,
          allOf(
              transform(Functions.elementAt(0)).check(isEqualTo("handler:aMethod")),
              transform(Functions.elementAt(1)).check(isEqualTo("default:bMethod")),
              transform(Functions.elementAt(2)).check(isEqualTo("fallback:cMethod"))
          ));
      throw e;
    }
  }

  @Test(expected = TestException.class)
  public void disableAutoLogging() {
    A aobj = new ObjectSynthesizer()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackObject(newObjectImplementingAForFallback())
        .disableMethodHandlerDecorator()
        .addInterface(A.class)
        .synthesize()
        .castTo(A.class);

    List<String> out = new LinkedList<>();
    try {
      out.add(aobj.aMethod());
      out.add(aobj.bMethod());
      out.add(aobj.cMethod());
      out.add(aobj.eMethod());
    } catch (TestException e) {
      e.printStackTrace();
      out.forEach(System.err::println);
      assertThat(
          out,
          allOf(
              transform(Functions.elementAt(0)).check(isEqualTo("handler:aMethod")),
              transform(Functions.elementAt(1)).check(isEqualTo("default:bMethod")),
              transform(Functions.elementAt(2)).check(isEqualTo("fallback:cMethod"))
          ));
      throw e;
    }
  }

  private static A newObjectImplementingAForFallback() {
    return new A() {

      @Override
      public String aMethod() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String cMethod() {
        return "fallback:cMethod";
      }
    };
  }
}
