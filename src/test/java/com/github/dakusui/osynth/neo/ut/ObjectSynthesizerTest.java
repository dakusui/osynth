package com.github.dakusui.osynth.neo.ut;

import com.github.dakusui.osynth.neo.ObjectSynthesizer;
import com.github.dakusui.osynth.neo.SynthesizedObject;
import org.junit.Test;

import static com.github.dakusui.osynth.neo.ObjectSynthesizer.method;

public class ObjectSynthesizerTest {

  @Test
  public void testThis() {
    SynthesizedObject object =
        new ObjectSynthesizer()
            .addInterface(A.class)
            .handle(method("aMethod", String.class).with((synthesizedObject, args) -> args[0] + ", world!"))
            .fallbackObject(new Object())
            .synthesize();
    System.out.println(object.castTo(A.class).aMethod("Hello!"));
  }
}
