package com.github.dakusui.osynth2.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public interface OsynthInvocationHandler extends InvocationHandler {

  @Override
  default Object invoke(Object proxy, Method method, Object[] args) {
    //assert that(proxy, and(isNotNull(), isInstanceOf(SynthesizedObject.class)));
    assert proxy instanceof SynthesizedObject;
    return methodHandlerFor(method).apply((SynthesizedObject) proxy, args);
  }

  default MethodHandler methodHandlerFor(Method method) {
    return figureOutMethodHandlerFor(method);
  }

  MethodHandler figureOutMethodHandlerFor(Method method);

  SynthesizedObject.Descriptor descriptor();

  interface WithCache extends OsynthInvocationHandler {
    Map<Method, MethodHandler> cache();

    default MethodHandler methodHandlerFor(Method method) {
      return cache().computeIfAbsent(method, this::figureOutMethodHandlerFor);
    }
  }

  abstract class Base implements OsynthInvocationHandler {
    private final SynthesizedObject.Descriptor descriptor;

    protected Base(SynthesizedObject.Descriptor descriptor) {
      this.descriptor = descriptor;
    }

    @Override
    public SynthesizedObject.Descriptor descriptor() {
      return this.descriptor;
    }
  }
}
