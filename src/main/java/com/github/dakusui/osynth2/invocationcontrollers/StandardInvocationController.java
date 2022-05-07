package com.github.dakusui.osynth2.invocationcontrollers;

import com.github.dakusui.osynth2.core.*;
import com.github.dakusui.osynth2.core.utils.AssertionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromInterfaces;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.forms.Functions.stream;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class StandardInvocationController extends InvocationController.Base implements InvocationController.WithCache {
  private final Map<MethodSignature, MethodHandler> methodHandlerMap;
  private final Map<Method, MethodHandler>          methodHandlerCache = createCache();

  public StandardInvocationController(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
    require(descriptor.methodHandlerEntries(),
        transform(stream().andThen(AssertionUtils.streamToMethodHandlerEntryStream())
            .andThen(AssertionUtils.methodHandlerEntryStreamToMethodMatcherStream()))
            .check(allMatch(isInstanceOf(MethodMatcher.MethodSignatureMatcher.class))));
    this.methodHandlerMap = new HashMap<>();
    this.descriptor().methodHandlerEntries()
        .forEach(eachEntry -> methodHandlerMap.put(
            ((MethodMatcher.MethodSignatureMatcher) eachEntry.matcher()).handlableMethod(),
            eachEntry.handler()));
  }

  public MethodHandler figuredOutMethodHandlerFor(Method invokedMethod) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(invokedMethod);
    if (this.methodHandlerMap.containsKey(methodSignature))
      methodHandler = this.methodHandlerMap.get(methodSignature);
    else
      methodHandler = createMethodHandlerFromInterfaces(descriptor().interfaces(), methodSignature)
          .orElseGet(() -> createMethodHandlerFromFallbackObject(descriptor().fallbackObject(), methodSignature));
    return methodHandler;
  }

  @Override
  public Map<Method, MethodHandler> cache() {
    return this.methodHandlerCache;
  }
}
