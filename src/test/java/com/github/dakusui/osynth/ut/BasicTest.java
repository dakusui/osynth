package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.exceptions.OsynthException;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;

public class BasicTest extends UtBase {
  public interface A {
    String aMethod(String message);
  }

  @Test
  public void testIfCustomMethodHandlerOverridesAbstractMethod() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> "customMethodHandler:<" + args[0] + ">"))
        .fallbackTo(new Object())
        .synthesize();
    String output = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("customMethodHandler"),
        containsString("Hello!")
    ));
  }

  @Test
  public void testIfCustomMethodHandlerOverridesAbstractMethodTwice() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> "customMethodHandler:<" + args[0] + ">"))
        .fallbackTo(new Object())
        .synthesize();
    String output1 = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output1);
    String output2 = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output2);
    assertThat(output1, allOf(
        containsString("customMethodHandler"),
        containsString("Hello!")
    ));
    assertThat(output2, allOf(
        containsString("customMethodHandler"),
        containsString("Hello!")
    ));
    assertThat(output1, isEqualTo(output2));
  }


  static class TestRuntimeException extends RuntimeException {
    TestRuntimeException(String message) {
      super(message);
    }
  }

  @Test(expected = TestRuntimeException.class)
  public void whenCustomMethodHandlerOverridesAbstractMethodThrowsException$theExceptionThrown() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> {
          throw new TestRuntimeException(
              "customMethodHandler:<" + args[0] + ">");
        }))
        .fallbackTo(new Object())
        .synthesize();
    try {
      String output = object.castTo(A.class).aMethod("Hello!");
      System.out.println(output);
    } catch (TestRuntimeException e) {
      assertThat(e.getMessage(), allOf(
          containsString("customMethodHandler"),
          containsString("Hello!")
      ));
      throw e;
    }
  }

  static class TestCheckedException extends Exception {
    TestCheckedException(String message) {
      super(message);
    }
  }

  @Test(expected = TestCheckedException.class)
  public void whenCustomMethodHandlerOverridesAbstractMethodThrowsCheckedException$theExceptionThrownAsOsynthInvocationTargetException() throws Throwable {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> {
          throw new TestCheckedException(
              "customMethodHandler:<" + args[0] + ">");
        }))
        .fallbackTo(new Object())
        .synthesize();
    try {
      String output = object.castTo(A.class).aMethod("Hello!");
      System.out.println(output);
    } catch (OsynthException e) {
      assertThat(e.getMessage(), allOf(
          containsString("customMethodHandler"),
          containsString("Hello!")
      ));
      throw e.getCause();
    }
  }

  @Test
  public void whenCallToString$descriptorToStringIsCalled() {
    Object fallbackObject = new Object();
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> "customMethodHandler:<" + args[0] + ">"))
        .fallbackTo(fallbackObject)
        .synthesize();
    String output = object.castTo(A.class).toString();
    System.out.println(output);
    assertThat(output, allOf(
        startsWith("osynth(A,"),
        containsString("fallback:"),
        containsString(fallbackObject.toString())
    ));
  }

  interface B {
    default String bMethod(String arg) {
      return "defaultImplementation:bMethod:arg:<" + arg + ">";
    }
  }

  @Test
  public void testDefaultMethodIsOverridden() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(B.class)
        .handle(methodCall("bMethod", String.class).with((synthesizedObject, args) -> "overridden by methodHandler for bMethod:<" + args[0] + ">"))
        .fallbackTo(new Object())
        .synthesize();
    String output = object.castTo(B.class).bMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("overridden by methodHandler"),
        containsString("for bMethod"),
        containsString("Hello!")
    ));
  }

  @Test
  public void testDefaultMethodIsUsed() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(B.class)
        .handle(methodCall("aMethod", String.class).with((synthesizedObject, args) -> "overridden by methodHandler for aMethod:<" + args[0] + ">"))
        .fallbackTo(new Object())
        .synthesize();
    String output = object.castTo(B.class).bMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("defaultImplementation:"),
        containsString("Hello!")));
  }

  public static class C implements A, B {

    @Override
    public String aMethod(String message) {
      return "implementation in fallback object for aMethod: <" + message + ">";
    }

    @Override
    public String bMethod(String message) {
      return "implementation in fallback object for bMethod: <" + message + ">";
    }
  }

  @Test
  public void testFallbackObject$defaultMethodIsPickedUp() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .fallbackTo(new Object())
        .synthesize();
    String output = object.castTo(B.class).bMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("defaultImplementation"),
        containsString("bMethod"),
        containsString("Hello!")
    ));
  }

  @Test
  public void testFallbackObject$methodInFallbackIsPickedUp() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .fallbackTo(new C())
        .synthesize();
    String output = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output);

    assertThat(output, allOf(
        containsString("implementation in fallback object"),
        containsString("aMethod"),
        containsString("Hello!")
    ));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testFallbackObject$whenImplementationIsMissing$methodInFallbackIsPickedUpAndUnsupportedOperationThrown() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .addInterface(B.class)
        .fallbackTo(new B() {
        })
        .synthesize();
    String output = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("implementation in fallback object"),
        containsString("aMethod"),
        containsString("Hello!")
    ));
  }

  interface BB extends B {
    @Override
    String bMethod(String message);
  }

  @Test
  public void testFallbackObject$whenMethodWithDefaultImplementationIsOverriddenWithoutDefaultImplementation$methodInFallbackIsPickedUp() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(BB.class)
        .fallbackTo(new B() {
          @Override
          public String bMethod(String message) {
            return "fallback bMethod: <" + message + ">";
          }
        })
        .synthesize();
    String output = object.castTo(B.class).bMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("fallback"),
        containsString("bMethod"),
        containsString("<Hello!>")
    ));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testFallbackObject$whenImplementationIsOverriddenWithoutDefaultImplementation$methodInFallbackIsPickedUpAndUnsupportedOperationThrown() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(BB.class)
        .fallbackTo(new Object())
        .synthesize();
    try {
      object.castTo(B.class).bMethod("Hello!");
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      String output = e.getMessage();
      assertThat(output, allOf(
          containsString("bMethod(String)"),
          containsString("was not found in"),
          containsString("'osynth(BB,"),
          containsString("methodHandlers=["),
          containsString(BB.class.getName())
      ));
      throw e;
    }
  }
}
