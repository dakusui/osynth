package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer;
import com.github.dakusui.osynth.compat.utils.AssertionInCatchClauseFinished;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.MethodSignature;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.osynth.ut.core.utils.UtUtils;
import com.github.dakusui.pcond.TestAssertions;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.ObjectSynthesizer.*;
import static com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer.methodCall;
import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.Fluents.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotNull;

public class ObjectSynthesizerTest extends UtBase {
  interface A {
    @SuppressWarnings("unused") // Called through reflection
    default String aMethod() {
      return "aMethod";
    }
  }

  interface B {
    String bMethod();
  }

  interface X extends A, B {
  }

  interface E {
    class EException extends RuntimeException {
    }

    default String eMethod() {
      throw new EException();
    }
  }

  private final Object fallbackObject = new Object();

  @Test
  public void givenHandlerForMethodInB$whenMethodInBCalled$thenHandlerIsRun() {
    Object x = new LegacyObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackTo(fallbackObject)
        .synthesize();
    assertThat(x,
        allOf(
            asString(call("aMethod").$()).equalTo("aMethod").$(),
            asString(call("bMethod").$()).equalTo("b is called").$()
        ));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void givenNoHandlerForMethodInB$whenMethodInBCalled$thenUnsupportedOperationException() {
    B x = new LegacyObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .fallbackTo(fallbackObject)
        .synthesize()
        .castTo(B.class);
    try {
      System.out.println(x.bMethod());
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      // No appropriate handler for the requested method:'public abstract java.lang.String com.github.dakusui.osynth.ut.ObjectSynthesizerTest$B.bMethod()' was found in fallback object:'[java.lang.Object@39ba5a14]'
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("An appropriate method")
                      .after("handler")
                      .after("implementation")
                      .after("for")
                      .after("bMethod\\(\\)")
                      .after("was not found in")
                      .after("osynth\\(A,B,SynthesizedObject")
                      .$(),
                  UtUtils.nonEmptyString())
              .containsString("bMethod").$());
      throw e;
    }
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void whenSynthesizedWithUnregisteredInterface$thenClassCastExceptionThrown() {
    try {
      Serializable x = new LegacyObjectSynthesizer()
          .addInterface(A.class)
          .addInterface(B.class)
          .fallbackTo(fallbackObject)
          .synthesize()
          .castTo(Serializable.class);
      System.out.println(x);
    } catch (ClassCastException e) {
      e.printStackTrace();
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("Tried to cast to")
                      .after(Serializable.class.getName())
                      .after("but available interfaces are only")
                      .after("A,")
                      .after("B,")
                      .after("SynthesizedObject")
                      .$(),
                  UtUtils.nonEmptyString())
              .$());
      AssertionInCatchClauseFinished.assertionInCatchClauseFinished();
    }
  }

  interface TestFunction {
    String apply(String value);
  }
/*
  @Test(expected = AssertionInCatchClauseFinished.class)
  public void givenErrorThrowingFallbackHandlerIsGiven$whenInvokeMethod$thenIntendedErrorThrown() {
    TestFunction x = new ObjectSynthesizer()
        .addInterface(TestFunction.class)
        .fallbackHandlerFactory(proxyDescriptor -> {
          throw new AssertionInCatchClauseFinished();
        })
        .synthesize();
    x.apply("hello");
  }
*/
 /*
  @Test(expected = AssertionInCatchClauseFinished.class)
  public void givenErrorValueReturningFallbackHandlerIsGiven$whenInvokeMethod$thenIntendedValueReturned() {
    TestFunction x = new ObjectSynthesizer()
        .addInterface(TestFunction.class)
        .fallbackHandlerFactory(proxyDescriptor -> {
          throw new AssertionInCatchClauseFinished();
        })
        .synthesize();
    assertThat(x.apply("hello"), asString().equalTo("hello").$());
  }
*/

  /**
   * Passes on JDK8
   */
  @Test
  public void givenCoreInterface$whenSynthesized$thenPass() {
    Object out = new LegacyObjectSynthesizer().addInterface(Serializable.class).fallbackTo(new Object()).synthesize();
    System.out.println(out);
    assertNotNull(out);
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void whenNonInterfaceClassPassed$thenExceptionThrown() {
    try {
      new LegacyObjectSynthesizer().addInterface(String.class).synthesize();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), asString().containsString(String.class.getCanonicalName()).$());
      AssertionInCatchClauseFinished.assertionInCatchClauseFinished();
    }
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingDefaultMethodExecuted$thenErrorThrown() {
    System.out.println(new LegacyObjectSynthesizer().addInterface(E.class).synthesize().castTo(E.class).eMethod());
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingOverridingMethodExecuted$thenErrorThrown() {
    new LegacyObjectSynthesizer()
        .addInterface(E.class)
        .fallbackTo(new E() {
          @Override
          public String eMethod() {
            throw new EException();
          }
        })
        .synthesize()
        .castTo(E.class)
        .eMethod();
  }

  /**
   * Now, only one fallback is allowed.
   */
  @ReleaseNote
  @Ignore
  @Test
  public void givenMultipleHandlerObjects$whenMethodsRun$thenMethodsOnHandlerObjectsCalled() {
    Object x = new LegacyObjectSynthesizer().addInterface(A.class)
        .addInterface(B.class)
        .fallbackTo(new A() {
          @Override
          public String aMethod() {
            return "Overridden A";
          }
        })
        .fallbackTo((B) () -> "Overridden B")
        .synthesize();
    A a = (A) x;
    B b = (B) x;
    System.out.println(a.aMethod());
    System.out.println(b.bMethod());
    assertThat(
        x,
        allOf(
            asString("aMethod").eq("Overridden A").$(),
            asString("bMethod").equalTo("Overridden B").$()
        )
    );
  }

  @SuppressWarnings("EqualsWithItself")
  @Test
  public void whenEqualsItself$thenTrue() {
    Object x = new LegacyObjectSynthesizer().addInterface(A.class).synthesize();
    assertThat(
        x.equals(x),
        asBoolean().isTrue().$()
    );
  }

  /**
   * See also: givenSynthesizedObjectsFromTheSameDefinitions$whenEquals$thenTrue
   */
  @Ignore
  @ReleaseNote
  @Test
  public void givenEmptySynthesizedObjects$whenEquals$thenTrue() {
    Object x1 = new LegacyObjectSynthesizer().addInterface(A.class).synthesize();
    Object x2 = new LegacyObjectSynthesizer().addInterface(A.class).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isTrue().$()
    );
  }

  /**
   * Perhaps, I should fix this test.
   * We would be able to implement equals/hash based on the equal-ness of descriptors.
   */
  @ReleaseNote
  @Ignore
  @Test
  public void givenSynthesizedObjectsFromTheSameDefinitions$whenEquals$thenTrue() {
    Object o = new Object();
    Object x1 = new LegacyObjectSynthesizer().addInterface(A.class).fallbackTo(o).synthesize();
    Object x2 = new LegacyObjectSynthesizer().addInterface(A.class).fallbackTo(o).synthesize();
    System.out.println(Objects.equals(x1, x2));
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isTrue().$()
    );
  }

  @Test
  public void givenSynthesizedObjectsFromDifferentDefinitions$whenEquals$thenFalse() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object x1 = new LegacyObjectSynthesizer().addInterface(A.class).fallbackTo(o1).synthesize();
    Object x2 = new LegacyObjectSynthesizer().addInterface(A.class).fallbackTo(o2).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isFalse().$()
    );
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingMethodIsInvoked$thenExceptionThrown() {
    E e = new LegacyObjectSynthesizer().addInterface(E.class).synthesize().castTo(E.class);
    System.out.println(e.eMethod());
  }


  @Test
  public void example() {
    Arrays.stream(LinkedList.class.getInterfaces()).forEach(System.out::println);
    System.out.println();
    Arrays.stream(List.class.getInterfaces()).forEach(System.out::println);

  }

  @Test(expected = ClassCastException.class)
  public void givenNoExplicitInterface$whenSynthesizedWithoutInclusionFromFallbackInNonAutoMode$thenClassCastException() {
    B b = new LegacyObjectSynthesizer()
        .fallbackTo((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize()
        .castTo(B.class);
    System.out.println(b.bMethod());
  }

  @Test
  public void givenSynthesizedObject$whenResynthesize$overridingHandlerIsInvoked() {
    A a = LegacyObjectSynthesizer.create(false)
        .addInterface(A.class)
        .handle(methodCall("aMethod").with((self, args) -> "OverridingA was called"))
        .synthesize()
        .castTo(A.class);
    A aa = LegacyObjectSynthesizer.create(false)
        .handle(methodCall("aMethod").with((self, args) -> "Re-OverridingA was called"))
        .includeInterfacesFromFallbackObject()
        .fallbackTo(a)
        .synthesize()
        .castTo(A.class);
    System.out.println(aa.aMethod());
  }

  @Test(expected = IllegalArgumentException.class)
  public void givenSynthesizerWithInterfaceBTwice$whenSynthesize$thenThrowsException() {
    try {
      B b = LegacyObjectSynthesizer.synthesizer()
          .addInterface(B.class)
          .addInterface(B.class)
          .fallbackTo((B) () -> "bMethod in lambda (test10) was called.")
          .synthesize()
          .castTo(B.class);
      System.out.println(b.bMethod());
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      assertThat(
          e.getMessage(),
          asString()
              .containsString("repeated interface: ")
              .containsString(B.class.getName()).$());
      throw e;
    }
  }

  @Test
  public void givenSynthesizerWithInterfaceB$whenSynthesizeWithNonB$thenReturnValueFromNonB() {
    B b = LegacyObjectSynthesizer.synthesizer()
        .addInterface(B.class)
        .fallbackTo(new Object() {
          /**
           * This method is called through the synthesizer
           */
          @SuppressWarnings("unused")
          public String bMethod() {
            return "bMethod in handlerObject";
          }
        })
        .synthesize()
        .castTo(B.class);
    System.out.println(b.bMethod());
    assertThat(b.bMethod(), asString().equalTo("bMethod in handlerObject").$());
  }

  @Test
  public void givenTweakerWithInterfaceB$whenSynthesizeWithB$thenReturnValueFromB() {
    B b = new LegacyObjectSynthesizer()
        .addInterface(B.class)
        .fallbackTo((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize()
        .castTo(B.class);
    System.out.println(b.bMethod());
    assertThat(b.bMethod(), asString().equalTo("bMethod in lambda (test10) was called.").$());
  }

  @Test
  public void givenDescriptorIsNotFinalized$whenQueried$thenFalse() {
    TestAssertions.assertThat(
        new ObjectSynthesizer(),
        when().asObject().castTo((ObjectSynthesizer) value())
            .then()
            .testPredicate(objectSynthesizerIsDescriptorFinalized().negate())
            .verify());
  }

  @Test
  public void givenDescriptorIsFinalized$whenQueried$thenTrue() {
    ObjectSynthesizer osynth = new ObjectSynthesizer().fallbackTo(new Object());
    osynth.synthesize();
    TestAssertions.assertThat(
        osynth,
        when().asObject().castTo((ObjectSynthesizer) value())
            .then()
            .testPredicate(objectSynthesizerIsDescriptorFinalized())
            .verify());
  }

  interface TestInterface {
    String helloMethod();
  }

  @Test
  public void test() {
    SynthesizedObject osynth = new ObjectSynthesizer()
        .handle(methodCall(nameMatchingRegex("helloM.*")).with((v, args) -> "helloWorldMethod"))
        .addInterface(TestInterface.class)
        .fallbackTo("HELLO_WORLD")
        .synthesize();

    TestAssertions.assertThat(
        osynth,
        when().asValueOfClass(SynthesizedObject.class)
            .exercise((SynthesizedObject s) -> s.castTo(TestInterface.class))
            .exercise(function("testInterfaceHelloMethod", TestInterface::helloMethod))
            .then()
            .isEqualTo("helloWorldMethod"));
  }

  interface TestInterface2 {

    String helloMethod0();

    String helloMethod1(Object var1);

    String helloMethod2(Object var1, Number number);

    String helloMethod4(String var1);

  }

  @Test
  public void givenSynthesizedObjectWithLenientMatchers$whenMethodWithMatchingHandlerInvoked$thenProperHandlerExecuted() {
    SynthesizedObject givenObject = synthesizeObject(
        "HELLO_WORLD",
        TestInterface2.class,
        builderForLenientHandlerEntry("helloMethod0").apply((v1, args) -> "helloWorldMethod0"),
        builderForLenientHandlerEntry("helloMethod1", String.class).apply((v1, args) -> "helloWorldMethod1:" + args[0]),
        builderForLenientHandlerEntry("helloMethod2", String.class, Integer.class).apply((v1, args) -> "helloWorldMethod2:" + args[0] + ":" + args[1]));

    TestAssertions.assertThat(
        givenObject,
        when().asObject()
            .allOf(
                $().as((TestInterface2) value())
                    .exercise(TestInterface2::helloMethod0)
                    .then()
                    .isEqualTo("helloWorldMethod0"),
                $().as((TestInterface2) value())
                    .exercise(v -> v.helloMethod1("HELLO"))
                    .then()
                    .isEqualTo("helloWorldMethod1:HELLO"),
                $().as((TestInterface2) value())
                    .exercise(v -> v.helloMethod2("HELLO", 1))
                    .then()
                    .isEqualTo("helloWorldMethod2:HELLO:1")
            ));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void givenSynthesizedObjectWithLenientMatchers$whenMethodWithNoMatchingHandlerInvoked$thenUnsupportedException() {
    SynthesizedObject givenObject = synthesizeObject(
        "HELLO_WORLD",
        TestInterface2.class,
        builderForLenientHandlerEntry("helloMethod4").apply((v, args) -> {
          throw new AssertionError("SHOULD NOT MATCH BECAUSE OF PARAMETER NUMBER:helloWorldMethod4()");
        }),
        builderForLenientHandlerEntry("helloMethod4", Integer.class).apply((v, args) -> {
          throw new AssertionError("SHOULD NOT MATCH BECAUSE OF PARAMETER TYPE:helloWorldMethod4(Integer)");
        }));

    try {
      String v = givenObject.castTo(TestInterface2.class).helloMethod4("hello!");
      System.out.println(v);
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      throw e;
    }
  }

  static Function<MethodHandler, MethodHandlerEntry> builderForLenientHandlerEntry(String methodName, Class<?>... parameterTypes) {
    return methodHandler -> MethodHandlerEntry.create(matchingLeniently(MethodSignature.create(methodName, parameterTypes)), methodHandler);
  }

  private <T> SynthesizedObject synthesizeObject(Object fallbackObject, Class<T> interfaceClass, MethodHandlerEntry... handlerEntries) {

    return new ObjectSynthesizer() {{
      for (MethodHandlerEntry eachEntry : handlerEntries)
        this.handle(eachEntry);
    }}
        .addInterface(interfaceClass)
        .fallbackTo(fallbackObject)
        .synthesize();
  }


  @Retention(RUNTIME)
  @interface TestAnnotation {
    String value() default "world";
  }

  interface TestInterface3 {
    @TestAnnotation
    String method1(String var);
  }

  @Test
  public void testAnnotatedMethod() {
    MethodHandler methodHandlingFunction = (o, args) -> "annotatedWith:" + TestAnnotation.class.getSimpleName() + ":" + Arrays.toString(args);
    SynthesizedObject so = new ObjectSynthesizer()
        .addInterface(TestInterface3.class)
        .handle(methodCall(annotatedWith(TestAnnotation.class)).with(methodHandlingFunction))
        .synthesize();
    TestAssertions.assertThat(so,
        when().asValueOfClass(SynthesizedObject.class)
            .exercise(synthesizedObjectCastTo(TestInterface3.class))
            .exercise(v -> v.method1("Hello"))
            .then().asString()
            .isEqualTo("annotatedWith:TestAnnotation:[Hello]"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testAnnotatedMethod2() {
    SynthesizedObject so = synthesizeObjectForInterface(TestInterface3.class);
    try {
      String message = so.castTo(TestInterface3.class).method1("Hello");
      System.out.println(message);
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      TestAssertions.assertThat(e, when().as((UnsupportedOperationException) value())
          .exercise(throwableGetMessage())
          .then().asString()
          .findSubstrings(
              "An appropriate method handler/implementation for 'method1(String)' was not found",
              "osynth",
              "TestInterface3",
              "SynthesizedObject"));
      throw e;
    }
  }

  interface TestInterface4 {
    @TestAnnotation(value = "WORLD")
    String method1(String var);
  }

  @Test
  public void testAnnotatedMethod3() {
    SynthesizedObject givenObject = synthesizeObjectForInterface(TestInterface4.class);

    TestAssertions.assertThat(
        givenObject,
        when().asValueOfClass(SynthesizedObject.class)
            .exercise(synthesizedObjectCastTo(TestInterface4.class))
            .exercise(v -> v.method1("Hello"))
            .then().asString()
            .isEqualTo("annotatedWith:TestAnnotation:[Hello]"));
  }

  private static <T> SynthesizedObject synthesizeObjectForInterface(Class<T> interfaceClass) {
    MethodHandler methodHandlingFunction = (o, args) -> "annotatedWith:" + TestAnnotation.class.getSimpleName() + ":" + Arrays.toString(args);
    return new ObjectSynthesizer()
        .addInterface(interfaceClass)
        .handle(methodCall(annotatedWith(TestAnnotation.class, annotation -> Objects.equals(annotation.value(), "WORLD"))).with(methodHandlingFunction))
        .synthesize();
  }
}
