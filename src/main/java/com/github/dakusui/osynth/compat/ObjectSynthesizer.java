package com.github.dakusui.osynth.compat;

public class ObjectSynthesizer extends com.github.dakusui.osynth.ObjectSynthesizer {

  public static ObjectSynthesizer synthesizer() {
    return new ObjectSynthesizer();
  }

  public static ObjectSynthesizer create(boolean b) {
    return new ObjectSynthesizer();
  }
}
