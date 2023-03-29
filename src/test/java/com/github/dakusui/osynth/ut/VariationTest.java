package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.forms.Predicates.findSubstrings;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;

public class VariationTest extends UtBase {
  public interface A {
    String aMethod(String message);
  }

  @Test
  public void whenCallBuiltInMethodFromInsideHandler$thenCalledSuccessfully() {
    SynthesizedObject object = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod", String.class)
            .with((synthesizedObject, args) ->
                args[0] + ":" + synthesizedObject.descriptor().toString()))
        .fallbackTo(new Object())
        .synthesize();
    String output = object.castTo(A.class).aMethod("Hello!");
    System.out.println(output);
    assertThat(output, findSubstrings(
        "Hello!",
        "methodHandlers=[",
        "nameMatchingExactly[aMethod],"));
  }
}
