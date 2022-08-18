package com.github.dakusui.osynth.ut;


import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.osynth.ObjectSynthesizer;
import org.junit.Test;

import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.pcond.forms.Predicates.containsString;

public class InterfaceInclusionTest extends UtBase {
  interface F {
    String fMethod(String value);
  }

  @Test(expected = ClassCastException.class)
  public void givenInterfaceInclusionIsNotRequested$whenObjectIsCastToTestIF$thenExceptionThrown() {
    try {
      String out = new ObjectSynthesizer()
          .synthesize((F) value -> "anon:F:<" + value + ">")
          .castTo(F.class)
          .fMethod("Hello");
      System.out.println(out);
    } catch (Exception e) {
      // F is neither added to the synthesizer nor inclusion isn't specified.
      // Thus, the osynth considers, the cast isn't allowed.
      e.printStackTrace();
      assertThat(e.getMessage(), allOf(
          containsString("Tried to cast"),
          containsString(F.class.getName()),
          containsString("SynthesizedObject")
      ));
      throw e;
    }
  }

  @Test
  public void givenInterfaceInclusionIsRequested$whenObjectIsCastToTestIF$thenSuccessfullyCast() {
    String out = new ObjectSynthesizer()
        .includeInterfacesFromFallbackObject()
        .synthesize((F) method -> "anon:F:<" + method + ">")
        .castTo(F.class)
        .fMethod("Hello");
    System.out.println(out);
    assertThat(out, allOf(
        containsString("anon:F:<"),
        containsString("Hello"),
        containsString(">")));
  }
}
