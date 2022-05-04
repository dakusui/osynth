package com.github.dakusui.osynth2.ut;


import com.github.dakusui.osynth.utils.UtBase;
import com.github.dakusui.osynth2.ObjectSynthesizer;
import org.junit.Test;

import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.pcond.forms.Predicates.containsString;

public class InterfaceInclusionTest extends UtBase {
  interface F {
    String fMethod(String method);
  }

  @Test(expected = ClassCastException.class)
  public void givenInterfaceInclusionIsNotRequested$whenObjectIsCastToTestIF$thenExceptionThrown() {
    try {
      String out = new ObjectSynthesizer()
          .synthesize((F) method -> "anon:F:<" + ">")
          .castTo(F.class)
          .fMethod("Hello");
      System.out.println(out);
    } catch (Exception e) {
      assertThat(e.getMessage(), allOf(
          containsString("Tried to cast"),
          containsString(F.class.getName()),
          containsString("SynthesizedObject")
      ));
      throw e;
    }
  }

  @Test
  public void givenInterfaceInclusionIsRequested$whenObjectIsCastToTesetIF$thenSuccessfullyCast() {
    String out = new ObjectSynthesizer()
        .includeInterfacesFrom()
        .synthesize((F) method -> "anon:F:<" + ">")
        .castTo(F.class)
        .fMethod("Hello");
    System.out.println(out);
    assertThat(out, allOf(containsString("anon:F:"), containsString(F.class.getName())));
  }
}
