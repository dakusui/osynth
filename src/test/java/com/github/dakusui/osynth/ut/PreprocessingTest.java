package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.compat.utils.UtBase;
import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import org.junit.Test;

import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.thincrest_pcond.functions.Predicates.containsString;

public class PreprocessingTest extends UtBase {
  @Test(expected = ClassCastException.class)
  public void givenValidationLeftDefault$whenTwoReservedMethodsTriedOverridden$thenExceptionThrown() {
    try {
      Object synthesizedObject = new ObjectSynthesizer()
          .disablePreprocessing()
          .synthesize();
      System.out.println(synthesizedObject);
    } catch (RuntimeException e) {
      e.printStackTrace();
      assertThat(e.getMessage(), allOf(
          containsString("cannot be cast to"),
          containsString(SynthesizedObject.class.getName())
      ));
      throw e;
    }
  }
}
