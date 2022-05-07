package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.ObjectSynthesizer;

import java.util.function.Function;

public interface InvocationControllerFactory extends Function<ObjectSynthesizer, InvocationController> {
}
