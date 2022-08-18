package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.pcond.fluent.MoreFluents;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.osynth.ut.AutoLoggingTest.TestFunctions.listJoinByLinBreak;
import static com.github.dakusui.osynth.ut.AutoLoggingTest.TestFunctions.objectToString;
import static com.github.dakusui.osynth.utils.TestForms.joinByLineBreak;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.core.printable.ExplainablePredicate.explainableStringIsEqualTo;
import static com.github.dakusui.pcond.fluent.MoreFluents.assertWhen;
import static com.github.dakusui.pcond.fluent.MoreFluents.valueOf;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.pcond.forms.Printables.function;
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

  final List<String> out = new LinkedList<>();

  @Test(expected = TestException.class)
  public void enableAutoLogging() {
    A aobj = autologgingEnabledTestObject();

    List<String> out = new LinkedList<>();
    try {
      out.add(aobj.aMethod());
      out.add(aobj.bMethod());
      out.add(aobj.cMethod());
      out.add(aobj.eMethod()); // This throws an exception
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
  public void enableAutoLogging$whenNormalMethodA$thenDoesntBreak() {
    A aobj = autologgingEnabledTestObject();

    printTestObjectAndCurrentOutputToErr(aobj);

    assertWhen(
        valueOf(aobj).asObject().exercise(A::aMethod).then().asString().isEqualTo("handler:aMethod"),
        valueOf(out).asListOf((String) MoreFluents.value()).then().findElementsInOrderBy(asList(startsWith("ENTER"), startsWith("LEAVE")))
    );
  }

  private void printTestObjectAndCurrentOutputToErr(A aobj) {
    System.err.println(aobj.aMethod());
    out.forEach(System.err::println);
  }

  @Test
  public void givenAutoLoggingEnabled_whenDefaultMethod$bMethod$_thenDoesntBreak() {
    A aobj = autologgingEnabledTestObject();

    System.err.println(aobj.bMethod());
    out.forEach(System.err::println);

    assertThat(
        joinByLineBreak(out),
        findSubstrings(
            "ENTER", "method:<bMethod()>", "arguments:<[]>",
            "LEAVE", "method:<bMethod()>", "return:<\"default:bMethod\">"
        )
    );
  }

  @Test
  public void givenEnableAutoLogging_whenFallback$cMethod$_thenDoesntBreak() {
    A aobj = autologgingEnabledTestObject();

    System.err.println(aobj.cMethod());

    assertThat(
        joinByLineBreak(out),
        findSubstrings(
            "ENTER", "method:<cMethod()>", "arguments:<[]>",
            "LEAVE", "method:<cMethod()>", "return:<\"fallback:cMethod\">"
        )
    );
  }

  @Test
  public void givenAutoLoggingEnabledOverridingBuiltInMethod$whenToString$thenNotLoggedAndDoesntBreak() {
    A aobj = autologgingEnabledTestObject(
        methodCall("toString").with((v, args) -> "HELLO"));
    assertWhen(
        valueOf(aobj)
            .exercise(objectToString())
            .then()
            .isEqualTo("HELLO"),
        valueOf(out)
            .then().intoStringWith(listJoinByLinBreak())
            .isEmpty());
  }

  private A autologgingEnabledTestObject() {
    return autologgingEnabledTestObject(
        methodCall("aMethod")
            .with((synthesizedObject, args) -> "handler:aMethod"));
  }

  public enum TestFunctions {
    ;

    public static Function<Object, String> objectToString() {
      return function("toString", Object::toString);
    }

    @SuppressWarnings("unchecked")
    public static <T> Function<List<T>, String> listJoinByLinBreak() {
      return function("joinByLineBreak", v -> joinByLineBreak((List<String>) v));
    }
  }

  private A autologgingEnabledTestObject(MethodHandlerEntry aMethod) {
    return new ObjectSynthesizer()
        //        /*
        .enableAutoLoggingWritingTo(s -> {
          loggingToStdoutAndOutList(out, s);
        })
        //         */
        //        .enableAutoLogging()
        .handle(aMethod)
        .fallbackTo(newObjectImplementingAForFallback())
        .addInterface(A.class)
        .synthesize()
        .castTo(A.class);
  }

  @Test
  public void modifyAutoLoggingBehaviorBy$compose$_thenModifiedBehaviorObserved() {
    List<String> out = new LinkedList<>();
    ObjectSynthesizer osynth = new ObjectSynthesizer()
        .enableAutoLoggingWritingTo(s -> loggingToStdoutAndOutList(out, s))
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackTo(newObjectImplementingAForFallback())
        .addInterface(A.class);
    osynth.methodHandlerDecorator(osynth
        .methodHandlerDecorator()
        .compose((method, methodHandler) -> (synthesizedObject, args) -> handleMethodWithLoggingReturnTypeToStdoutAndOutList(synthesizedObject, method, args, methodHandler, out)));
    A givenObject = osynth
        .synthesize()
        .castTo(A.class);
    assertWhen(
        valueOf(givenObject).applyFunction(function("A::aMethod", A::aMethod))
            .then()
            .asString()
            .isEqualTo("handler:aMethod"),
        valueOf(out)
            .asListOfClass(String.class)
            .then()
            .verifyWith(allOf(
                transform(size()).check(isEqualTo(3)),
                findElements(startsWith("ENTER:"),
                    explainableStringIsEqualTo("returnType:String"),
                    startsWith("LEAVE:")))));
  }

  private static void loggingToStdoutAndOutList(List<String> out, String s) {
    System.out.println(s);
    out.add(s);
  }

  private static Object handleMethodWithLoggingReturnTypeToStdoutAndOutList(SynthesizedObject synthesizedObject, Method method, Object[] args, MethodHandler methodHandler, List<String> out) throws Throwable {
    String message = "returnType:" + method.getReturnType().getSimpleName();
    loggingToStdoutAndOutList(out, message);
    return methodHandler.handle(synthesizedObject, args);
  }

  @Test
  public void modifyAutoLoggingBehaviorBy$andThen$_thenModifiedBehaviorObserved() {
    ObjectSynthesizer osynth = new ObjectSynthesizer()
        .enableAutoLogging()
        .handle(methodCall("aMethod").with((synthesizedObject, args) -> "handler:aMethod"))
        .fallbackTo(newObjectImplementingAForFallback())
        .addInterface(A.class);
    osynth.methodHandlerDecorator(osynth.methodHandlerDecorator()
        .andThen((method, methodHandler) -> (synthesizedObject, args) -> {
          System.out.println("returnType:" + method.getReturnType().getSimpleName());
          return methodHandler.handle(synthesizedObject, args);
        }));
    A aobj = osynth
        .synthesize()
        .castTo(A.class);
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
