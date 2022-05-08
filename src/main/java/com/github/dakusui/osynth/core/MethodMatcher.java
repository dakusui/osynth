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
    return (m -> this.test(m) && other.test(m));
  }

  default MethodMatcher or(MethodMatcher other) {
    throw new UnsupportedOperationException();
  }

  default MethodMatcher negate() {
    throw new UnsupportedOperationException();
  }

  static MethodMatcher create(Supplier<String> nameComposer, Predicate<? super Method> p) {
    final Supplier<String> nc;
    if (MethodUtils.isToStringOverridden(p.getClass()))
      nc = () -> nameComposer.get() + ":" + p;
    else
      nc = () -> nameComposer.get() + ":" + MethodUtils.simpleClassNameOf(p.getClass());

    return new MethodMatcher() {
      @Override
      public boolean test(Method m) {
        return p.test(m);
      }

      @Override
      public String toString() {
        return nc.get();
      }
    };
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
