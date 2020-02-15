package com.github.dakusui.objsynth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public interface MethodHandler extends BiFunction<Object, Object[], Object>, Predicate<Method> {
  static Builder builder(Predicate<Method> predicate) {
    return new Builder(predicate);
  }

  static Builder builderByNameAndParameterTypes(String methodName, Class<?>[] parameterTypes) {
    return builder(method -> {
      AtomicInteger i = new AtomicInteger(-1);
      return Objects.equals(
          methodName, method.getName()) &&
          parameterTypes.length == method.getParameterCount() &&
          Arrays.stream(
              parameterTypes
          ).peek(
              type -> i.getAndIncrement()
          ).allMatch(
              type -> type.isAssignableFrom(method.getParameterTypes()[i.get()])
          );
    });
  }

  class Builder {
    private final Predicate<Method>                    matcher;
    private       BiFunction<Object, Object[], Object> function;

    public Builder(Predicate<Method> matcher) {
      this.matcher = Objects.requireNonNull(matcher);
    }

    public MethodHandler with(BiFunction<Object, Object[], Object> function) {
      this.function = Objects.requireNonNull(function);
      return this.build();
    }

    public MethodHandler build() {
      return new MethodHandler() {
        @Override
        public Object apply(Object self, Object[] objects) {
          return function.apply(self, objects);
        }

        @Override
        public boolean test(Method method) {
          return matcher.test(method);
        }
      };
    }
  }
}
