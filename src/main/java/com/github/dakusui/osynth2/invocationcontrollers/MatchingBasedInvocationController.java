package com.github.dakusui.osynth2.invocationcontrollers;

import com.github.dakusui.osynth2.core.*;

import java.lang.reflect.Method;
import java.util.Map;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromInterfaces;

public class MatchingBasedInvocationController extends InvocationController.Base implements InvocationController.WithCache {
  private final Map<Method, MethodHandler> cache = createCache();

  public MatchingBasedInvocationController(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
  }

  @Override
  public MethodHandler figureOutMethodHandlerFor(Method invokedMethod) {
    MethodSignature invokedMethodSignature = MethodSignature.create(invokedMethod);
    return this.descriptor().methodHandlerEntries().stream()
        .filter(me -> me.matcher().matches(invokedMethodSignature))
        .map(MethodHandlerEntry::handler)
        .findFirst()
        .orElseGet(() -> createMethodHandlerFromInterfaces(descriptor().interfaces(), invokedMethodSignature)
            .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor().fallbackObject(), invokedMethodSignature)));
  }

  @Override
  public Map<Method, MethodHandler> cache() {
    return this.cache;
  }
}
