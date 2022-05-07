package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.AbstractObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.MethodSignature;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer extends AbstractObjectSynthesizer<ObjectSynthesizer> {

  public ObjectSynthesizer() {
  }

  public static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return methodCall(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder methodCall(MethodSignature methodRequest) {
    return new MethodHandlerEntry.Builder().handle(methodRequest);
  }

}
