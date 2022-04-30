package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public interface MethodMatcher extends Predicate<Method> {
  String methodName();

  Class<?>[] parameterTypes();

  @Override
  default boolean test(Method method) {
    AtomicInteger i = new AtomicInteger(0);
    return Objects.equals(
        methodName(),
        method.getName()) &&
        parameterTypes().length == method.getParameterCount() &&
        Arrays.stream(parameterTypes())
            .allMatch(type -> type.isAssignableFrom(method.getParameterTypes()[i.getAndIncrement()]));
  }

  static MethodMatcher createMethodMatcher(String methodName, Class<?>[] parameterTypes) {
    return new MethodMatcher() {
      @Override
      public String methodName() {
        return methodName;
      }

      @Override
      public Class<?>[] parameterTypes() {
        return parameterTypes;
      }

      @Override
      public int hashCode() {
        return methodName.hashCode();
      }

      @Override
      public boolean equals(Object anotherObject) {
        if (this == anotherObject)
          return true;
        if (anotherObject instanceof MethodMatcher) {
          MethodMatcher another = (MethodMatcher) anotherObject;
          return this.methodName().equals(another.methodName()) && Arrays.equals(this.parameterTypes(), another.parameterTypes());
        }
        return false;
      }

    };
  }
}
