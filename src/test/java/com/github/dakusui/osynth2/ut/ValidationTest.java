package com.github.dakusui.osynth2.ut;

import com.github.dakusui.osynth.utils.UtBase;
import com.github.dakusui.osynth2.ObjectSynthesizer;
import com.github.dakusui.osynth2.core.MethodHandler;
import com.github.dakusui.osynth2.core.SynthesizedObject;
import com.github.dakusui.osynth2.exceptions.ValidationException;
import org.junit.Test;

import static com.github.dakusui.osynth2.ObjectSynthesizer.method;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.functions.Predicates.allOf;
import static com.github.dakusui.pcond.functions.Predicates.containsString;

public class ValidationTest extends UtBase {
  @Test
  public void givenValidationDisabled$whenReservedMethodTriedOverridden$thenNoExceptionThrown() {
    SynthesizedObject synthesizedObject = new ObjectSynthesizer()
        .disableValidation()
        .fallbackObject(new Object())
        .handle(method("descriptor")
            .with(createNewDescriptorReturningHandler())).synthesize();
    System.out.println(synthesizedObject.descriptor());
  }

  @Test(expected = ValidationException.class)
  public void givenValidationLeftDefault$whenOneReservedMethodTriedOverridden$thenExceptionThrown() {
    try {
      SynthesizedObject synthesizedObject = new ObjectSynthesizer()
          .fallbackObject(new Object())
          .handle(method("descriptor").with(createNewDescriptorReturningHandler()))
          .synthesize();
      System.out.println(synthesizedObject);
    } catch (RuntimeException e) {
      assertThat(e.getMessage(), allOf(
          containsString("Reserved methods cannot be overridden"),
          containsString("descriptor()")
      ));
      throw e;
    }
  }

  @Test(expected = ValidationException.class)
  public void givenValidationLeftDefault$whenTwoReservedMethodsTriedOverridden$thenExceptionThrown() {
    try {
      SynthesizedObject synthesizedObject = new ObjectSynthesizer()
          .fallbackObject(new Object())
          .handle(method("descriptor").with(createNewDescriptorReturningHandler()))
          .handle(method("castTo", Class.class).with(createNewDescriptorReturningHandler()))
          .synthesize();
      System.out.println(synthesizedObject);
    } catch (RuntimeException e) {
      e.printStackTrace();
      assertThat(e.getMessage(), allOf(
          containsString("Reserved methods cannot be overridden"),
          containsString("descriptor()")
      ));
      throw e;
    }
  }

  private static MethodHandler createNewDescriptorReturningHandler() {
    return (sobj, objects) -> new SynthesizedObject.Descriptor.Builder()
        .fallbackObject(new Object())
        .build();
  }

  private static MethodHandler createCastingWithoutCheckHandler() {
    return (sobj, objects) -> ((Class<?>)objects[0]).cast(sobj);
  }
}
