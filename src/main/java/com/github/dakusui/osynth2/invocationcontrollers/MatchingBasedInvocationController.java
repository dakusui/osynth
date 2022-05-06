package com.github.dakusui.osynth2.invocationcontrollers;

import com.github.dakusui.osynth2.core.*;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromInterfaces;

public class MatchingBasedInvocationController extends OsynthInvocationHandler.Base implements OsynthInvocationHandler.WithCache {
  private final Map<Method, MethodHandler> cache = new ConcurrentHashMap<>();

  public MatchingBasedInvocationController(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
  }

  @Override
  public MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodSignature methodSignature = MethodSignature.create(method);
    return this.descriptor().methodHandlerEntries().stream()
        .filter(me -> me.matcher().matches(methodSignature))
        .map(MethodHandlerEntry::handler)
        .findFirst()
        .orElseGet(() -> createMethodHandlerFromInterfaces(descriptor().interfaces(), methodSignature)
            .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor().fallbackObject(), methodSignature)));
  }

  @Override
  public Map<Method, MethodHandler> cache() {
    return this.cache;
  }
}
