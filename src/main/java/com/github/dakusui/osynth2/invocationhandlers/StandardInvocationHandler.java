package com.github.dakusui.osynth2.invocationhandlers;

import com.github.dakusui.osynth2.core.*;
import com.github.dakusui.osynth2.core.utils.AssertionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromFallbackObject;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.createMethodHandlerFromInterfaces;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.forms.Functions.stream;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class StandardInvocationHandler extends OsynthInvocationHandler.Base implements OsynthInvocationHandler.WithCache {
  private final Map<MethodMatcher, MethodHandler> methodHandlerMap;
  private final Map<Method, MethodHandler>        methodHandlerCache = new ConcurrentHashMap<>();

  public StandardInvocationHandler(SynthesizedObject.Descriptor descriptor) {
    super(descriptor);
    require(descriptor.methodHandlerEntries(),
        transform(stream().andThen(AssertionUtils.streamToMethodHandlerEntryStream())
            .andThen(AssertionUtils.methodHandlerEntryStreamToMethodMatcherStream()))
            .check(allMatch(isInstanceOf(MethodSignature.class))));
    this.methodHandlerMap = new HashMap<>();
    this.descriptor().methodHandlerEntries()
        .forEach(eachEntry -> methodHandlerMap.put(
            eachEntry.matcher(),
            eachEntry.handler()));
  }

  public MethodHandler figureOutMethodHandlerFor(Method method) {
    MethodHandler methodHandler;
    MethodSignature methodSignature = MethodSignature.create(method);
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
