package com.github.dakusui.osynth.invocationcontrollers;

import com.github.dakusui.osynth.core.*;

import java.lang.reflect.Method;
import java.util.Map;

import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerFromInterfaces;

public class StandardInvocationController extends InvocationController.Base implements InvocationController.WithCache {
  private final Map<Method, MethodHandler> cache = createCache();

  public StandardInvocationController(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
  }

  @Override
  public MethodHandler figuredOutMethodHandlerFor(Method invokedMethod) {
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
