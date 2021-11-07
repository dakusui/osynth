package com.github.dakusui.osynth.annotations;

import com.github.dakusui.osynth.ObjectSynthesizer;

@FunctionalInterface
public interface Configurator {
  ObjectSynthesizer configure(ObjectSynthesizer configure);
}
