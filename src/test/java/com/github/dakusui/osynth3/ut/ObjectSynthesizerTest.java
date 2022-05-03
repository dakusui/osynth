package com.github.dakusui.osynth3.ut;

import com.github.dakusui.osynth3.ObjectSynthesizer;
import com.github.dakusui.osynth3.core.SynthesizedObject;
import com.github.dakusui.osynth.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.osynth3.ObjectSynthesizer.method;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.functions.Predicates.allOf;
import static com.github.dakusui.pcond.functions.Predicates.containsString;

public class ObjectSynthesizerTest extends UtBase {
  public interface A {
    String aMethod(String message);
  }

  @Test
  public void testIfCustomMethodHandlerOverridesAbstractMethod() {
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(A.class)
            .handle(method("aMethod", String.class).with((synthesizedObject, args) -> "customMethodHandler:<" + args[0] + ">"))
            .fallbackObject(new Object())
            .synthesize();
    String output = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output);
    assertThat(output, allOf(
        containsString("customMethodHandler"),
        containsString("Hello!")
    ));
  }

  interface B {
    default String bMethod(String arg) {
      return "defaultImplementation:bMethod:arg:<" + arg + ">";
    }
  }

  @Test
  public void testDefaultMethodIsOverridden() {
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(B.class)
            .handle(method("bMethod", String.class).with((synthesizedObject, args) -> "overridden by methodHandler for bMethod:<" + args[0] + ">"))
            .fallbackObject(new Object())
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(B.class)
            .handle(method("aMethod", String.class).with((synthesizedObject, args) -> "overridden by methodHandler for aMethod:<" + args[0] + ">"))
            .fallbackObject(new Object())
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(A.class)
            .addInterface(B.class)
            .fallbackObject(new Object())
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(A.class)
            .addInterface(B.class)
            .fallbackObject(new C())
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(A.class)
            .addInterface(B.class)
            .fallbackObject(new B() {
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(BB.class)
            .fallbackObject(new B() {
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
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(BB.class)
            .fallbackObject(new Object())
            .synthesize();
    try {
      object.castTo(B.class).bMethod("Hello!");
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      String output = e.getMessage();
      assertThat(output, allOf(
          containsString("bMethod(String)"),
          containsString("was not found in"),
          containsString("'osynth:"),
          containsString("methodHandlers={}"),
          containsString(BB.class.getName())
      ));
      throw e;
    }
  }
}
