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

  @FunctionalInterface
  interface ByMethodSignature extends MethodMatcher {
    enum PrivateUtils {
      ;

      private static ByMethodSignature overrideToString(Function<ByMethodSignature, String> toString, ByMethodSignature byMethodSignature) {
        return new ByMethodSignature() {
          @Override
          public boolean test(Method m) {
            return byMethodSignature.test(m);
          }

          @Override
          public String toString() {
            return toString.apply(this);
          }
        };
      }
    }
  }

  static ByMethodSignature createStrict(MethodSignature targetMethodSignature) {
    return ByMethodSignature.PrivateUtils.overrideToString(
        v -> "matcher:" + targetMethodSignature,
        (Method candidate) -> Objects.equals(targetMethodSignature.name(), candidate.getName())
            && Arrays.equals(targetMethodSignature.parameterTypes(), candidate.getParameterTypes()));
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
    return ByMethodSignature.PrivateUtils.overrideToString(
        v -> "matcher:" + targetMethodSignature,
        (Method candidate) -> {
          AtomicInteger i = new AtomicInteger(0);
          Class<?>[] parameterTypes = candidate.getParameterTypes();
          return Objects.equals(targetMethodSignature.name(), candidate.getName()) &&
              targetMethodSignature.parameterTypes().length == parameterTypes.length &&
              Arrays.stream(targetMethodSignature.parameterTypes())
                  .allMatch(type -> type.isAssignableFrom(parameterTypes[i.getAndIncrement()]));
        });
  }
}
