package com.github.dakusui.osynth.compat;

public class SimpleObjectSynthesizer<T> extends ObjectSynthesizer {
  public SimpleObjectSynthesizer(Class<T> anInterface) {
    this.addInterface(anInterface);
  }

  public static <T> SimpleObjectSynthesizer<T> create(Class<T> anInterface) {
    return new SimpleObjectSynthesizer<>(anInterface);
  }
}
