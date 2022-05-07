package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * This interface is applied to a method handler right after it is figured out
 * by the `osynth`'s method handler looking up mechanism is finished.
 *
 * The returned method handler is used by the rest of the library thereafter.
 *
 * This mechanism is useful to construct an "AOP"-like feature such as auto-logging, etc.
 */
public interface MethodHandlerDecorator extends BiFunction<Method, MethodHandler, MethodHandler> {
  MethodHandlerDecorator IDENTITY                       = (method, methodHandler) -> methodHandler;
  Set<MethodSignature>   PASS_THROUGH_METHOD_SIGNATURES = new HashSet<MethodSignature>() {
    {
      this.addAll(SynthesizedObject.RESERVED_METHOD_SIGNATURES);
      this.addAll(SynthesizedObject.BUILT_IN_METHOD_SIGNATURES);
    }
  };

  default MethodHandlerDecorator compose(MethodHandlerDecorator before) {
    return (method, methodHandler) -> MethodHandlerDecorator.this.apply(method, before.apply(method, methodHandler));
  }

  default MethodHandlerDecorator andThen(MethodHandlerDecorator after) {
    return (method, methodHandler) -> after.apply(method, MethodHandlerDecorator.this.apply(method, methodHandler));
  }

  static MethodHandlerDecorator filterPredefinedMethods(MethodHandlerDecorator decorator) {
    return (method, methodHandler) -> {
      if (isPassThroughMethod(method))
        return methodHandler;
      return decorator.apply(method, methodHandler);
    };
  }

  static boolean isPassThroughMethod(Method method) {
    return PASS_THROUGH_METHOD_SIGNATURES.contains(MethodSignature.create(method));
  }
}
