package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.ObjectSynthesizer;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public interface MethodHandler extends BiFunction<Object, Object[], Object>, Predicate<Method> {
  Predicate<Method> matcher();

  BiFunction<Object, Object[], Object> function();

  static Builder builder(Predicate<Method> predicate) {
    return new Builder(predicate);
  }

  static Builder builderByNameAndParameterTypes(String methodName, Class<?>... parameterTypes) {
    return builder(MethodMatcher.createMethodMatcher(methodName, parameterTypes));
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
    return hashCodeHandler(o, Object::hashCode);
  }

  static MethodHandler hashCodeHandler(Object o, Function<Object, Integer> hashCode) {
    return ObjectSynthesizer.methodCall("hashCode").with((self, args) -> hashCode.apply(o));
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
        public Predicate<Method> matcher() {
          return matcher;
        }

        @Override
        public BiFunction<Object, Object[], Object> function() {
          return function;
        }

        @Override
        public Object apply(Object self, Object[] objects) {
          return function.apply(self, objects);
        }

        @Override
        public boolean test(Method method) {
          return matcher.test(method);
        }

        @Override
        public int hashCode() {
          return function.hashCode();
        }

        @Override
        public boolean equals(Object anotherObject) {
          if (this == anotherObject)
            return true;
          if (anotherObject instanceof MethodHandler) {
            MethodHandler another = (MethodHandler) anotherObject;
            return this.matcher().equals(another.matcher()) && this.function().equals(another.function());
          }
          return false;
        }
      };
    }
  }
}
