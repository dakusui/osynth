package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
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

  private Object fallbackObject = new Object();

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
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("Fallback object:")
                      .after("is not assignable to")
                      .after(", which declares requested method:")
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
      assertThat(
          e,
          asString(call("getMessage").$())
              .check(
                  substringAfterRegex("No matching interface was found for")
                      .after("Serializable")
                      .after("A")
                      .after("B")
                      .$(),
                  nonEmptyString())
              .$());
      assertionInCatchClauseFinished();
    }
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
    ObjectSynthesizer.ProxyDescriptor desc = createEmptyDesc();
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
    System.out.println(((ObjectSynthesizer.Describable) x).describe());
  }

  @Test(expected = IllegalArgumentException.class)
  public void givenNoExplicitInterface$whenSynthesizeObjectInNonAutoMode$thenErrorIsThrown() {
    B b = ObjectSynthesizer.create(false)
        .addHandlerObject((B) () -> "bMethod in lambda (test10) was called.")
        .synthesize();
    System.out.println(b.bMethod());
  }

  protected ObjectSynthesizer.ProxyDescriptor createEmptyDesc() {
    return new ObjectSynthesizer.ProxyDescriptor(
        new LinkedList<>(),
        new LinkedList<>(),
        new LinkedList<>());
  }
}
