package com.github.dakusui.osynth.invocationcontrollers;

import com.github.dakusui.osynth.core.*;
import com.github.dakusui.osynth.core.utils.AssertionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerFromInterfaces;
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
    // Put the entries in the reverse order in order to more prioritize an entry
    // comes earlier than the one comes later.
    MethodHandlerEntry[] methodHandlerEntries = this.descriptor().methodHandlerEntries().toArray(new MethodHandlerEntry[0]);
    IntStream.range(0, methodHandlerEntries.length)
        .map(i -> methodHandlerEntries.length - (i + 1))
        .mapToObj(i -> methodHandlerEntries[i])
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
