package com.github.dakusui.osynth2.compat;

public class ObjectSynthesizer extends com.github.dakusui.osynth2.ObjectSynthesizer {

  public static ObjectSynthesizer synthesizer() {
    return new ObjectSynthesizer();
  }

  public static ObjectSynthesizer create(boolean b) {
    return new ObjectSynthesizer();
  }
}
