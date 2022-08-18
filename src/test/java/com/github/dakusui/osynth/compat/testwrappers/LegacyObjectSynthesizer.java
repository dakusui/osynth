package com.github.dakusui.osynth.compat.testwrappers;

public class LegacyObjectSynthesizer extends com.github.dakusui.osynth.ObjectSynthesizer {

  public static LegacyObjectSynthesizer synthesizer() {
    return new LegacyObjectSynthesizer();
  }

  public static LegacyObjectSynthesizer create(boolean b) {
    if (b)
      return (LegacyObjectSynthesizer) new LegacyObjectSynthesizer().includeInterfacesFromFallbackObject();
    else
      return new LegacyObjectSynthesizer();
  }
}
