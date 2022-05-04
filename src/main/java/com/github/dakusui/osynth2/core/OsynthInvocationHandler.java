package com.github.dakusui.osynth2.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class OsynthInvocationHandler implements InvocationHandler {
  final         Map<MethodSignature, MethodHandler> methodHandlers;
  final         List<Class<?>>                      interfaceClasses;
  private final Object                              fallbackObject;

  public OsynthInvocationHandler(Map<MethodSignature, MethodHandler> methodHandlers, List<Class<?>> interfaceClasses, Object fallbackObject) {
    this.methodHandlers = requireNonNull(methodHandlers);
    this.interfaceClasses = requireNonNull(interfaceClasses);
    this.fallbackObject = requireNonNull(fallbackObject);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    //assert that(proxy, and(isNotNull(), isInstanceOf(SynthesizedObject.class)));
    MethodHandler methodHandler = figureOutMethodHandlerFor(method);
    return methodHandler.apply((SynthesizedObject) proxy, args);
  }

  protected MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(method);
    if (methodHandlers.containsKey(methodSignature))
      methodHandler = methodHandlers.get(methodSignature);
    else
      methodHandler = interfaceClasses.stream()
          .map((Class<?> eachInterfaceClass) -> MethodUtils.createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .orElseGet(() -> MethodUtils.createMethodHandlerFromFallbackObject(fallbackObject, methodSignature));
    return methodHandler;
  }
}
