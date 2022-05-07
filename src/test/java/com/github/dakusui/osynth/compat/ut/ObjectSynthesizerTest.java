package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.compat.ObjectSynthesizer;
import com.github.dakusui.osynth.compat.utils.AssertionInCatchClauseFinished;
import com.github.dakusui.osynth.compat.utils.UtBase;
import com.github.dakusui.osynth.compat.utils.UtUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.compat.ObjectSynthesizer.methodCall;
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
    Object x = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackObject(fallbackObject)
        .synthesize();
    assertThat(x,
        allOf(
            asString(call("aMethod").$()).equalTo("aMethod").$(),
            asString(call("bMethod").$()).equalTo("b is called").$()
        ));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void givenNoHandlerForMethodInB$whenMethodInBCalled$thenUnsupportedOperationException() {
    B x = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .fallbackObject(fallbackObject)
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
      Serializable x = new ObjectSynthesizer()
          .addInterface(A.class)
          .addInterface(B.class)
          .fallbackObject(fallbackObject)
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
    Object out = new ObjectSynthesizer().addInterface(Serializable.class).fallbackObject(new Object()).synthesize();
    System.out.println(out);
    assertNotNull(out);
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void whenNonInterfaceClassPassed$thenExceptionThrown() {
    try {
      new ObjectSynthesizer().addInterface(String.class).synthesize();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), asString().containsString(String.class.getCanonicalName()).$());
      AssertionInCatchClauseFinished.assertionInCatchClauseFinished();
    }
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingDefaultMethodExecuted$thenErrorThrown() {
    System.out.println(new ObjectSynthesizer().addInterface(E.class).synthesize().castTo(E.class).eMethod());
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingOverridingMethodExecuted$thenErrorThrown() {
    new ObjectSynthesizer()
        .addInterface(E.class)
        .fallbackObject(new E() {
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
    Object x = new ObjectSynthesizer().addInterface(A.class)
        .addInterface(B.class)
        .fallbackObject(new A() {
          @Override
          public String aMethod() {
            return "Overridden A";
          }
        })
        .fallbackObject((B) () -> "Overridden B")
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
    Object x = new ObjectSynthesizer().addInterface(A.class).synthesize();
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
    Object x1 = new ObjectSynthesizer().addInterface(A.class).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).synthesize();
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
    Object x1 = new ObjectSynthesizer().addInterface(A.class).fallbackObject(o).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).fallbackObject(o).synthesize();
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
    Object x1 = new ObjectSynthesizer().addInterface(A.class).fallbackObject(o1).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).fallbackObject(o2).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isFalse().$()
    );
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingMethodIsInvoked$thenExceptionThrown() {
    E e = new ObjectSynthesizer().addInterface(E.class).synthesize().castTo(E.class);
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
    B b = new ObjectSynthesizer()
        .fallbackObject((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize()
        .castTo(B.class);
    System.out.println(b.bMethod());
  }

  @Test
  public void givenSynthesizedObject$whenResynthesize$overridingHandlerIsInvoked() {
    A a = ObjectSynthesizer.create(false)
        .addInterface(A.class)
        .handle(methodCall("aMethod").with((self, args) -> "OverridingA was called"))
        .synthesize()
        .castTo(A.class);
    A aa = ObjectSynthesizer.create(false)
        .handle(methodCall("aMethod").with((self, args) -> "Re-OverridingA was called"))
        .includeInterfacesFrom()
        .fallbackObject(a)
        .synthesize()
        .castTo(A.class);
    System.out.println(aa.aMethod());
  }

  @Test(expected = IllegalArgumentException.class)
  public void givenSynthesizerWithInterfaceBTwice$whenSynthesize$thenThrowsException() {
    try {
      B b = ObjectSynthesizer.synthesizer()
          .addInterface(B.class)
          .addInterface(B.class)
          .fallbackObject((B) () -> "bMethod in lambda (test10) was called.")
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
    B b = ObjectSynthesizer.synthesizer()
        .addInterface(B.class)
        .fallbackObject(new Object() {
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
    B b = new ObjectSynthesizer()
        .addInterface(B.class)
        .fallbackObject((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize()
        .castTo(B.class);
    System.out.println(b.bMethod());
    assertThat(b.bMethod(), asString().equalTo("bMethod in lambda (test10) was called.").$());
  }
}
