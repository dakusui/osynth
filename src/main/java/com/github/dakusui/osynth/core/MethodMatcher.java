package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.core.utils.MethodUtils;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface MethodMatcher {
  boolean test(Method m);

  default MethodMatcher and(MethodMatcher other) {
    return create(m -> "and(" + this + "," + other + ")", v -> this.test(v) && other.test(v));
  }

  default MethodMatcher or(MethodMatcher other) {
    return create(m -> "or(" + this + "," + other + ")", v -> this.test(v) || other.test(v));
  }

  default MethodMatcher negate() {
    return create(m -> "not(" + this + ")", v -> !test(v));
  }

  static MethodMatcher create(Function<MethodMatcher, String> toString, Predicate<Method> methodPredicate) {
    return PrivateUtils.methodMatcherOverrideToString(toString, create(methodPredicate));
  }

  static MethodMatcher create(Predicate<Method> methodPredicate) {
    return methodPredicate::test;
  }

  enum PrivateUtils {
    ;

    private static MethodMatcher methodMatcherOverrideToString(Function<MethodMatcher, String> toString, MethodMatcher matcher) {
      return new MethodMatcher() {
        @Override
        public boolean test(Method m) {
          return matcher.test(m);
        }

        @Override
        public String toString() {
          return toString.apply(matcher);
        }
      };
    }
  }
}
