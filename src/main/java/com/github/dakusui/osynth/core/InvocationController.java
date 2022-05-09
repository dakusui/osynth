package com.github.dakusui.osynth.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dakusui.osynth.core.utils.MethodUtils.execute;
import static com.github.dakusui.osynth.core.utils.MethodUtils.toEmptyArrayIfNull;

public interface InvocationController extends InvocationHandler {
  Object[] EMPTY_ARGS = new Object[0];

  @Override
  default Object invoke(Object proxy, Method method, Object[] args) {
    assert proxy instanceof SynthesizedObject;
    InvocationContext.Impl.contextWith(method);
    return execute(() -> methodHandlerFor(method).handle((SynthesizedObject) proxy, toEmptyArrayIfNull(args)));
  }

  default MethodHandler methodHandlerFor(Method method) {
    return figuredOutMethodHandlerAndApplyDecorator(method);
  }

  default MethodHandler figuredOutMethodHandlerAndApplyDecorator(Method method) {
    return descriptor()
        .methodHandlerDecorator()
        .apply(method, figuredOutMethodHandlerFor(method));
  }

  MethodHandler figuredOutMethodHandlerFor(Method invokedMethod);

  SynthesizedObject.Descriptor descriptor();

  static InvocationContext invocationContext() {
    return InvocationContext.forCurrentThread();
  }

  interface WithCache extends InvocationController {
    /**
     * Returns a newly created map object used for method handler cache.
     *
     * Default implementation of this method returns a new {@link ConcurrentHashMap}
     * object.
     * Assign the returned map of this method to a field returned by
     * {@link this#cache()} method.
     *
     * @return A new map for method handler cache.
     */
    default Map<Method, MethodHandler> createCache() {
      return new ConcurrentHashMap<>();
    }

    /**
     * Returns an already created map object to be used for a method handler cache.
     *
     * @return A map for method handler cache.
     */
    Map<Method, MethodHandler> cache();

    default MethodHandler methodHandlerFor(Method method) {
      return cache().computeIfAbsent(method, this::figuredOutMethodHandlerAndApplyDecorator);
    }
  }

  abstract class Base implements InvocationController {
    private final SynthesizedObject.Descriptor descriptor;

    protected Base(SynthesizedObject.Descriptor descriptor) {
      this.descriptor = descriptor;
    }

    @Override
    public SynthesizedObject.Descriptor descriptor() {
      return this.descriptor;
    }
  }
}
