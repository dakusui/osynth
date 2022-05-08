package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.core.utils.MethodUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@FunctionalInterface
public interface MethodMatcher extends Predicate<Method> {
  @Override
  boolean test(Method m);

  static MethodMatcher create(Supplier<String> nameComposer, Predicate<? super Method> p) {
    final Supplier<String> nc;
    if (MethodUtils.isToStringOverridden(p.getClass()))
      nc = () -> nameComposer.get() + ":" + p;
    else
      nc = () -> nameComposer.get() + ":" + MethodUtils.composeSimpleClassName(p.getClass());

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

  @FunctionalInterface
  interface ByMethodSignature extends MethodMatcher {
    default boolean test(Method method) {
      return matches(MethodSignature.create(method));
    }

    boolean matches(MethodSignature s);

    static ByMethodSignature createStrict(MethodSignature targetMethodSignature) {
      return overrideToString(
          v -> "matcher:" + targetMethodSignature,
          (MethodSignature candidate) -> Objects.equals(targetMethodSignature.name(), candidate.name())
              && Arrays.equals(targetMethodSignature.parameterTypes(), candidate.parameterTypes()));
    }
  }

  /**
   * Returns a "lenient" method matcher by signature.
   * The returned matcher checks if
   *
   * 1. The name of a method to be tested is equal to the `targetMethodSignature`.
   * 2. Every parameter types of the method to be tested is equal to or more special than the corresponding parameter type in the `targetMethodSignature`.
   *
   * @param targetMethodSignature The method signature that matches a returned matcher.
   * @return A method matcher by signature.
   */
  static ByMethodSignature createLenient(MethodSignature targetMethodSignature) {
    return overrideToString(
        v -> "matcher:" + targetMethodSignature,
        (MethodSignature candidate) -> {
          AtomicInteger i = new AtomicInteger(0);
          return Objects.equals(targetMethodSignature.name(), candidate.name()) &&
              targetMethodSignature.parameterTypes().length == candidate.parameterTypes().length &&
              Arrays.stream(targetMethodSignature.parameterTypes())
                  .allMatch(type -> type.isAssignableFrom(candidate.parameterTypes()[i.getAndIncrement()]));
        });
  }

  static ByMethodSignature overrideToString(Function<ByMethodSignature, String> toString, ByMethodSignature byMethodSignature) {
    return new ByMethodSignature() {
      @Override
      public boolean matches(MethodSignature s) {
        return byMethodSignature.matches(s);
      }

      @Override
      public String toString() {
        return toString.apply(this);
      }
    };
  }
}
