package com.github.dakusui.osynth2.core;

import com.github.dakusui.osynth2.core.utils.MethodUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;

public class StandardInvocationHandler extends OsynthInvocationHandler.Base implements OsynthInvocationHandler.WithCache {
  private final Map<MethodMatcher, MethodHandler>       methodHandlerMap;
  private final ThreadLocal<Map<Method, MethodHandler>> methodHandlerCache = new ThreadLocal<>();

  public StandardInvocationHandler(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
    this.methodHandlerMap = new HashMap<>();
    this.descriptor().methodHandlers()
        .forEach(eachEntry -> methodHandlerMap.put(
            eachEntry.matcher(),
            eachEntry.handler()));
    this.methodHandlerCache.set(new HashMap<>());
  }

  public MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(method);
    if (this.methodHandlerMap.containsKey(methodSignature))
      methodHandler = this.methodHandlerMap.get(methodSignature);
    else
      methodHandler = descriptor().interfaces().stream()
          .map((Class<?> eachInterfaceClass) -> MethodUtils.createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .findFirst()
          .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor().fallbackObject(), methodSignature));
    return methodHandler;
  }

  @Override
  public Map<Method, MethodHandler> cache() {
    return this.methodHandlerCache.get();
  }
}
