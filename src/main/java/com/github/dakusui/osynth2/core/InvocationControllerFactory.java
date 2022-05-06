package com.github.dakusui.osynth2.core;

import com.github.dakusui.osynth2.ObjectSynthesizer;

import java.util.function.Function;

public interface InvocationControllerFactory extends Function<ObjectSynthesizer, InvocationController> {
}
