package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.Describable;
import com.github.dakusui.osynth.core.ProxyDescriptor;
import com.github.dakusui.osynth.utils.AssertionInCatchClauseFinished;
import com.github.dakusui.osynth.utils.UtBase;
import com.github.dakusui.osynth.utils.UtUtils;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.osynth.utils.AssertionInCatchClauseFinished.assertionInCatchClauseFinished;
import static com.github.dakusui.osynth.utils.UtUtils.nonEmptyString;
import static java.util.Collections.emptyList;

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
        .addHandlerObject(fallbackObject)
        .synthesize();
    assertThat(x,
        allOf(
            asString(call("aMethod").$()).equalTo("aMethod").$(),
            asString(call("bMethod").$()).equalTo("b is called").$()
        ));
  }

  @Test(expected = IllegalArgumentException.class)
  public void givenNoHandlerForMethodInB$whenMethodInBCalled$thenIllegalArgumentExceptionThrown() {
    B x = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .addHandlerObject(fallbackObject)
        .synthesize(B.class);
    try {
      System.out.println(x.bMethod());
    } catch (IllegalArgumentException e) {
      // No appropriate handler for the requested method:'public abstract java.lang.String com.github.dakusui.osynth.ut.ObjectSynthesizerTest$B.bMethod()' was found in fallback object:'[java.lang.Object@39ba5a14]'
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("No appropriate handler")
                      .after("ObjectSynthesizerTest\\$B")
                      .after("bMethod")
                      .after("was found in fallback object")
                      .after("java.lang.Object")
                      .$(),
                  nonEmptyString())
              .containsString("bMethod").$());
      throw e;
    }
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void whenSynthesizedWithUnregisteredInterface$thenIllegalArgumentExceptionThrown() {
    try {
      Serializable x = new ObjectSynthesizer()
          .addInterface(A.class)
          .addInterface(B.class)
          .addHandlerObject(fallbackObject)
          .synthesize(Serializable.class);
      System.out.println(x);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("value:")
                      .after(Serializable.class.getName())
                      .after("violated")
                      .after("isEqualTo\\[class java.lang.Object\\]")
                      .after("isInterface")
                      .after(A.class.getSimpleName())
                      .after(B.class.getSimpleName())
                      .after("isAssignableFrom")
                      .$(),
                  nonEmptyString())
              .$());
      assertionInCatchClauseFinished();
    }
  }

  interface TestFunction {
    String apply(String value);
  }

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

  /**
   * Passes on JDK8
   */
  @Test(expected = AssertionInCatchClauseFinished.class)
  public void givenCoreInterface$whenSynthesized$thenFail() {
    try {
      System.out.println(new ObjectSynthesizer().addInterface(Serializable.class).addHandlerObject(new Object()).<Object>synthesize());
    } catch (RuntimeException e) {
      assertThat(
          e,
          allOf(
              asString(call("getMessage").$())
                  .startsWith("Failed to create a method handles lookup")
                  .containsString(Serializable.class.getCanonicalName())
                  .containsString("prohibited")
                  .$(),
              asObject(call(UtUtils.class, "rootCause", e).$())
                  .isInstanceOf(IllegalArgumentException.class)
                  .$()
          )
      );
      assertionInCatchClauseFinished();
    }
  }

  @Test(expected = AssertionInCatchClauseFinished.class)
  public void whenNonInterfaceClassPassed$thenExceptionThrown() {
    try {
      new ObjectSynthesizer().addInterface(String.class).synthesize();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), asString().containsString(String.class.getCanonicalName()).$());
      assertionInCatchClauseFinished();
    }
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingDefaultMethodExecuted$thenErrorThrown() {
    System.out.println(new ObjectSynthesizer().addInterface(E.class).synthesize(E.class).eMethod());
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingOverridingMethodExecuted$thenErrorThrown() {
    new ObjectSynthesizer()
        .addInterface(E.class)
        .addHandlerObject(new E() {
          @Override
          public String eMethod() {
            throw new EException();
          }
        })
        .synthesize(E.class)
        .eMethod();
  }

  @Test
  public void givenMultipleHandlerObjects$whenMethodsRun$thenMethodsOnHandlerObjectsCalled() {
    Object x = new ObjectSynthesizer().addInterface(A.class)
        .addInterface(B.class)
        .addHandlerObject(new A() {
          @Override
          public String aMethod() {
            return "Overridden A";
          }
        })
        .addHandlerObject((B) () -> "Overridden B")
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

  @Test
  public void givenEmptySynthesizedObjects$whenEquals$thenTrue() {
    Object x1 = new ObjectSynthesizer().addInterface(A.class).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isTrue().$()
    );
  }

  @Test
  public void givenSynthesizedObjectsFromTheSameDefinitions$whenEquals$thenTrue() {
    Object o = new Object();
    Object x1 = new ObjectSynthesizer().addInterface(A.class).addHandlerObject(o).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).addHandlerObject(o).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isTrue().$()
    );
  }

  @Test
  public void givenSynthesizedObjectsFromDifferentDefinitions$whenEquals$thenFalse() {
    Object o1 = new Object();
    Object o2 = new Object();
    Object x1 = new ObjectSynthesizer().addInterface(A.class).addHandlerObject(o1).synthesize();
    Object x2 = new ObjectSynthesizer().addInterface(A.class).addHandlerObject(o2).synthesize();
    assertThat(
        x1,
        asBoolean(call("equals", x2).$()).isFalse().$()
    );
  }

  @Test(expected = E.EException.class)
  public void whenErrorThrowingMethodIsInvoked$thenExceptionThrown() {
    E e = new ObjectSynthesizer().addInterface(E.class).synthesize();
    System.out.println(e.eMethod());
  }

  @Test
  public void givenEmptyProxyDescriptor$whenHashCode$thenEqualToHashCodeFromEmptyList() {
    ProxyDescriptor desc = createEmptyDesc();
    assertThat(desc.hashCode(), asInteger().equalTo(emptyList().hashCode()).$());
  }

  @Test
  public void example() {
    Arrays.stream(LinkedList.class.getInterfaces()).forEach(System.out::println);
    System.out.println();
    Arrays.stream(List.class.getInterfaces()).forEach(System.out::println);

  }

  @Test
  public void givenNoExplicitInterface$whenSynthesizeObjectInAutoMode$thenDescribable() {
    X x = ObjectSynthesizer.create(true)
        .addHandlerObject((X) () -> "bMethodX in lambda")
        .synthesize();
    System.out.println(x.bMethod());
    System.out.println(((Describable) x).describe());
  }

  @Test(expected = IllegalArgumentException.class)
  public void givenNoExplicitInterface$whenSynthesizeObjectInNonAutoMode$thenErrorIsThrown() {
    B b = ObjectSynthesizer.create(false)
        .addHandlerObject((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize(B.class);
    System.out.println(b.bMethod());
  }

  @Test
  public void givenSynthesizedObject$whenResynthesize$overridingHandlerIsInvoked() {
    A a = ObjectSynthesizer.create(false)
        .addInterface(A.class)
        .handle(methodCall("aMethod").with((self, args) -> "OverridingA was called"))
        .synthesize();
    A aa = ObjectSynthesizer.create(false)
        .handle(methodCall("aMethod").with((self, args) -> "Re-OverridingA was called"))
        .resynthesizeFrom(a);
    System.out.println(aa.aMethod());
  }

  @Test(expected = IllegalStateException.class)
  public void givenSynthesizerWithInterfaceB$whenSynthesizeWithB$thenThrowsException() {
    try {
      B b = ObjectSynthesizer.synthesizer()
          .addInterface(B.class)
          .addHandlerObject((B) () -> "bMethod in lambda (test10) was called.")
          .synthesize();
      System.out.println(b.bMethod());
    } catch (IllegalStateException e) {
      e.printStackTrace();
      assertThat(
          e.getMessage(),
          asString()
              .containsString("violated precondition:value stream noneMatch[isInstanceOf[")
              .containsString("B]]").$());
      throw e;
    }
  }

  @Test
  public void givenSynthesizerWithInterfaceB$whenSynthesizeWithNonB$thenReturnValueFromNonB() {
    B b = ObjectSynthesizer.synthesizer()
        .addInterface(B.class)
        .addHandlerObject(new Object() {
          /**
           * This method is called through the synthesizer
           */
          @SuppressWarnings("unused")
          public String bMethod() {
            return "bMethod in handlerObject";
          }
        })
        .synthesize();
    System.out.println(b.bMethod());
    assertThat(b.bMethod(), asString().equalTo("bMethod in handlerObject").$());
  }

  @Test
  public void givenTweakerWithInterfaceB$whenSynthesizeWithB$thenReturnValueFromB() {
    B b = ObjectSynthesizer.tweaker()
        .addInterface(B.class)
        .addHandlerObject((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize();
    System.out.println(b.bMethod());
    assertThat(b.bMethod(), asString().equalTo("bMethod in lambda (test10) was called.").$());
  }

  @Test(expected = IllegalStateException.class)
  public void givenTweakerWithInterfaceA$whenSynthesizeWithA$thenThrowsException() {
    try {
      A a = ObjectSynthesizer.tweaker()
          .addInterface(A.class)
          .addHandlerObject((new A() {
            @Override
            public String aMethod() {
              throw new RuntimeException("aMethod in handlerObject: This method should not be called because A.class is registered and synthesizer is in 'tweak' mode.");
            }
          }))
          .synthesize();
      System.out.println(a.aMethod());
    } catch (IllegalStateException e) {
      e.printStackTrace();
      assertThat(
          e.getMessage(),
          asString().containsString("violated precondition:value methods->stream noneMatch[isDefaultMethod]").$());
      throw e;
    }
  }

  protected ProxyDescriptor createEmptyDesc() {
    return new ProxyDescriptor(
        new LinkedList<>(),
        new LinkedList<>(),
        new LinkedList<>(),
        ObjectSynthesizer.DEFAULT_FALLBACK_HANDLER_FACTORY);
  }
}
