package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.pcond.Fluents;
import com.github.dakusui.pcond.forms.Predicates;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.Fluents.*;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.core.printable.ExplainablePredicate.explainableStringIsEqualTo;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.thincrest_pcond.functions.Functions.size;
import static java.util.Arrays.asList;

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

  @Test
  public void modifyAutoLoggingBehaviorBy$compose$_thenModifiedBehaviorObserved() {
    List<String> out = new LinkedList<>();
    ObjectSynthesizer osynth = new ObjectSynthesizer()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackTo(newObjectImplementingAForFallback())
        .enableAutoLoggingWritingTo(s -> {
          System.out.println(s);
          out.add(s);
        })
        .addInterface(A.class);
    osynth.methodHandlerDecorator(osynth.methodHandlerDecorator()
        .compose((method, methodHandler) -> (synthesizedObject, args) -> {
          String message = "returnType:" + method.getReturnType().getSimpleName();
          System.out.println(message);
          out.add(message);
          return methodHandler.handle(synthesizedObject, args);
        }));
    A givenObject = osynth.synthesize().castTo(A.class);
    //    System.out.println(givenObject.aMethod());
    assertThat(
        list(givenObject, out),
        allOf(
            whenValueAt(0, (A) value()).
                applyFunction(A::aMethod)
                .thenAsString()
                .isEqualTo("handler:aMethod").verify(),
            whenValueAt(1, (List<String>) value())
                .then().allOf(
                    transform(size()).check(isEqualTo(3)),
                    Predicates.findElements(
                        startsWith("ENTER:"),
                        explainableStringIsEqualTo("returnType:String"),
                        startsWith("LEAVE:")
                    )).verify()
        ));
  }

  @Test
  public void modifyAutoLoggingBehaviorBy$andThen$_thenModifiedBehaviorObserved() {
    ObjectSynthesizer osynth = new ObjectSynthesizer()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackTo(newObjectImplementingAForFallback())
        .enableAutoLogging()
        .addInterface(A.class);
    osynth.methodHandlerDecorator(osynth.methodHandlerDecorator()
        .andThen((method, methodHandler) -> (synthesizedObject, args) -> {
          System.out.println("returnType:" + method.getReturnType().getSimpleName());
          return methodHandler.handle(synthesizedObject, args);
        }));
    A aobj = osynth.synthesize().castTo(A.class);
    System.out.println(aobj.aMethod());
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
