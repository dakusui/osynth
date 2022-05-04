package com.github.dakusui.osynth2.core;

import com.github.dakusui.osynth2.core.utils.MethodUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static java.util.Objects.requireNonNull;

public class OsynthInvocationHandler implements InvocationHandler {
  final SynthesizedObject.Descriptor descriptor;

  public OsynthInvocationHandler(SynthesizedObject.Descriptor descriptor) {
    this.descriptor = descriptor;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    //assert that(proxy, and(isNotNull(), isInstanceOf(SynthesizedObject.class)));
    assert proxy instanceof SynthesizedObject;
    MethodHandler methodHandler = figureOutMethodHandlerFor(method);
    return methodHandler.apply((SynthesizedObject) proxy, args);
  }

  protected MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(method);
    if (descriptor.methodHandlers().containsKey(methodSignature))
      methodHandler = descriptor.methodHandlers().get(methodSignature);
    else
      methodHandler = descriptor.interfaces().stream()
          .map((Class<?> eachInterfaceClass) -> MethodUtils.createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor.fallbackObject(), methodSignature));
    return methodHandler;
  }
}
