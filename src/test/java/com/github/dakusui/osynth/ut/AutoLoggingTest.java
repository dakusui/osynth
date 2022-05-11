package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.compat.utils.UtBase;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Matchers.findElements;
import static com.github.dakusui.pcond.forms.Predicates.isEqualTo;

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
        .fallbackTo(newObjectImplementingAForFallback())
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
          findElements(
              isEqualTo("handler:aMethod"),
              isEqualTo("default:bMethod"),
              isEqualTo("fallback:cMethod")
          ));
      throw e;
    }
  }

  @Test(expected = TestException.class)
  public void disableAutoLogging() {
    A aobj = new ObjectSynthesizer()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackTo(newObjectImplementingAForFallback())
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
          findElements(
              isEqualTo("handler:aMethod"),
              isEqualTo("default:bMethod"),
              isEqualTo("fallback:cMethod")
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
