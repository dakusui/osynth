package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import static com.github.dakusui.osynth.core.SynthesizedObject.BUILT_IN_METHODS;
import static com.github.dakusui.osynth.core.SynthesizedObject.RESERVED_METHODS;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

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
  Set<MethodSignature>   PASS_THROUGH_METHOD_SIGNATURES = unmodifiableSet(new HashSet<MethodSignature>() {
    {
      this.addAll(RESERVED_METHODS.stream().map(MethodSignature::create).collect(toSet()));
      this.addAll(BUILT_IN_METHODS.stream().map(MethodSignature::create).collect(toSet()));
    }
  });

  static MethodHandlerDecorator chainMethodHandlerDecorators(MethodHandlerDecorator methodHandlerDecorator, MethodHandlerDecorator methodHandlerDecorator1) {
    return methodHandlerDecorator == null ?
        methodHandlerDecorator1 :
        methodHandlerDecorator.andThen(methodHandlerDecorator1);
  }

  default MethodHandlerDecorator compose(MethodHandlerDecorator before) {
    return (method, methodHandler) -> MethodHandlerDecorator.this.apply(method, before.apply(method, methodHandler));
  }

  default MethodHandlerDecorator andThen(MethodHandlerDecorator after) {
    return (method, methodHandler) -> after.apply(method, MethodHandlerDecorator.this.apply(method, methodHandler));
  }

  static MethodHandlerDecorator filterOutPredefinedMethods(MethodHandlerDecorator decorator) {
    class PredefinedMethodFilteringOutMethodHandlerDecorator implements MethodHandlerDecorator {
      final MethodHandlerDecorator childDecorator = decorator;

      @Override
      public MethodHandler apply(Method method, MethodHandler methodHandler) {
        if (isPassThroughMethod(method))
          return methodHandler;
        return decorator.apply(method, methodHandler);
      }

      public int hashCode() {
        return this.childDecorator.hashCode();
      }

      public boolean equals(Object anotherObject) {
        if (this == anotherObject)
          return true;
        if (!(anotherObject instanceof PredefinedMethodFilteringOutMethodHandlerDecorator))
          return false;
        PredefinedMethodFilteringOutMethodHandlerDecorator another = (PredefinedMethodFilteringOutMethodHandlerDecorator) anotherObject;
        return Objects.equals(this.childDecorator, another.childDecorator);
      }
    }
    return new PredefinedMethodFilteringOutMethodHandlerDecorator();
  }

  static boolean isPassThroughMethod(Method method) {
    return PASS_THROUGH_METHOD_SIGNATURES.contains(MethodSignature.create(method));
  }
}
