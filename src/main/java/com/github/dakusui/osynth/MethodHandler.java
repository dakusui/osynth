package com.github.dakusui.osynth;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface MethodHandler extends BiFunction<Object, Object[], Object>, Predicate<Method> {
  static Builder builder(Predicate<Method> predicate) {
    return new Builder(predicate);
  }

  static Builder builderByNameAndParameterTypes(String methodName, Class<?>... parameterTypes) {
    return builder(method -> {
      AtomicInteger i = new AtomicInteger(0);
      return Objects.equals(
          methodName,
          method.getName()) &&
          parameterTypes.length == method.getParameterCount() &&
          Arrays.stream(parameterTypes)
              .allMatch(type -> type.isAssignableFrom(method.getParameterTypes()[i.getAndIncrement()]));
    });
  }

  /**
   * @param o         An object on which {@code equals(Object)} method is called.
   * @param converter A function that converts an argument given to {@code equals(Object)} method to a variable that can be compared to {@code o}.
   * @return A method handler object.
   */
  static MethodHandler equalsHandler(Object o, Function<Object, Object> converter) {
    return ObjectSynthesizer.methodCall("equals", Object.class)
        .with((self, args) -> o == args[0] || o.equals(converter.apply(args[0])));
  }

  static MethodHandler hashCodeHandler(Object o) {
    return ObjectSynthesizer.methodCall("hashCode").with((self, args) -> o.hashCode());
  }

  static MethodHandler toStringHandler(Object o, Function<Object, String> formatter) {
    return ObjectSynthesizer.methodCall("toString").with((self, args) -> formatter.apply(o));
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
