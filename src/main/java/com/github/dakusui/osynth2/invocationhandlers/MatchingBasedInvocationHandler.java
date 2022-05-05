package com.github.dakusui.osynth2.invocationhandlers;

import com.github.dakusui.osynth2.core.*;
import com.github.dakusui.osynth2.core.utils.MethodUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;

public class MatchingBasedInvocationHandler extends OsynthInvocationHandler.Base implements OsynthInvocationHandler.WithCache {
  private final Map<Method, MethodHandler> cache = new ConcurrentHashMap<>();

  protected MatchingBasedInvocationHandler(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
  }

  @Override
  public MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(method);
    return this.descriptor().methodHandlerEntries().stream()
        .filter(me -> me.matcher().matches(methodSignature))
        .map(MethodHandlerEntry::handler)
        .findFirst()
        .orElseGet(() -> descriptor().interfaces().stream()
            .map((Class<?> eachInterfaceClass) -> MethodUtils.createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor().fallbackObject(), methodSignature))
        );
  }

  @Override
  public Map<Method, MethodHandler> cache() {
    return this.cache;
  }
}
