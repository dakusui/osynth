package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.exceptions.ValidationException;
import org.junit.Test;

import java.io.Serializable;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.Fluents.value;
import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.pcond.forms.Predicates.containsString;
import static java.lang.String.format;

public class ValidationTest extends UtBase {
  @Test
  public void givenValidationDisabled$whenReservedMethodTriedOverridden$thenNoExceptionThrown() {
    SynthesizedObject synthesizedObject = new ObjectSynthesizer()
        .disableValidation()
        .fallbackTo(new Object())
        .handle(methodCall("descriptor")
            .with(createNewDescriptorReturningHandler())).synthesize();
    System.out.println(synthesizedObject.descriptor());
  }

  @Test(expected = ValidationException.class)
  public void givenValidationLeftDefault$whenOneReservedMethodTriedOverridden$thenExceptionThrown() {
    try {
      SynthesizedObject synthesizedObject = new ObjectSynthesizer()
          .fallbackTo(new Object())
          .handle(methodCall("descriptor").with(createNewDescriptorReturningHandler()))
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

  @Test(expected = ValidationException.class)
  public void givenValidationLeftDefault$whenTwoReservedMethodsTriedOverridden$thenExceptionThrown() {
    try {
      SynthesizedObject synthesizedObject = new ObjectSynthesizer()
          .fallbackTo(new Object())
          .handle(methodCall("descriptor").with(createNewDescriptorReturningHandler()))
          .handle(methodCall("castTo", Class.class).with(createNewDescriptorReturningHandler()))
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

  @Test(expected = ValidationException.class)
  public void givenDuplicationCheckEnabled$whenSameIFRegisteredTwice$thenExceptionThrown() {
    try {
      SynthesizedObject synthesizedObject = new ObjectSynthesizer()
          .fallbackTo(new Object())
          .enableDuplicatedInterfaceCheck()
          .addInterface(Serializable.class)
          .addInterface(Serializable.class)
          .synthesize();
      System.out.println(synthesizedObject);
    } catch (ValidationException e) {
      e.printStackTrace();
      assertThat(e.getMessage(), allOf(
          containsString("violated"),
          containsString("duplicatedElements isEmpty"),
          containsString(format("[%s,%s]", Serializable.class, Serializable.class)),
          containsString(format("[%s]", Serializable.class))
      ));
      throw e;
    }
  }

  public interface TestInterface {
    default String testMethod(String message) {
      return "testMethod[" + message + "]";
    }
  }

  @Test
  public void givenDuplicationCheckEnabled$whenNoSameIFRegisteredTwice$thenPass() {
    SynthesizedObject synthesizedObject = new ObjectSynthesizer()
        .fallbackTo(new Object())
        .enableDuplicatedInterfaceCheck()
        .addInterface(TestInterface.class)
        .synthesize();
    assertThat(synthesizedObject.castTo(TestInterface.class),
        when((TestInterface) value()).asObject()
            .exercise(v -> v.testMethod("Hello!"))
            .then()
            .isEqualTo("testMethod[Hello!]")
            .verify());
  }

  private static MethodHandler createNewDescriptorReturningHandler() {
    return (sobj, objects) -> new SynthesizedObject.Descriptor.Builder()
        .fallbackObject(new Object())
        .methodHandlerDecorator(MethodHandlerDecorator.IDENTITY)
        .build();
  }

  private static MethodHandler createCastingWithoutCheckHandler() {
    return (sobj, objects) -> ((Class<?>) objects[0]).cast(sobj);
  }
}
