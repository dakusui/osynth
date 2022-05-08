package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.ObjectSynthesizer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface MethodMatcher extends Predicate<Method> {
  @Override
  boolean test(Method m);

  static MethodMatcher create(Supplier<String> nameComposer, Predicate<? super Method> p) {
    final Supplier<String> nc;
    if (ObjectSynthesizer.WipUtils.isToStringOverridden(p.getClass()))
      nc = () -> nameComposer.get() + ":" + p;
    else
      nc = () -> nameComposer.get() + ":" + ObjectSynthesizer.WipUtils.composeSimpleClassName(p.getClass());

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

  enum Factory {
    LENIENT {
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
      @Override
      ByMethodSignature create(MethodSignature targetMethodSignature) {
        return new ByMethodSignature.Base(targetMethodSignature) {
          @Override
          public boolean matches(MethodSignature candidate) {
            AtomicInteger i = new AtomicInteger(0);
            return Objects.equals(targetMethodSignature().name(), candidate.name()) &&
                targetMethodSignature().parameterTypes().length == candidate.parameterTypes().length &&
                Arrays.stream(targetMethodSignature().parameterTypes())
                    .allMatch(type -> type.isAssignableFrom(candidate.parameterTypes()[i.getAndIncrement()]));
          }
        };
      }
    },
    STRICT {
      @Override
      ByMethodSignature create(MethodSignature handlableMethod) {
        return new ByMethodSignature.Base(handlableMethod) {
          @Override
          public boolean matches(MethodSignature candidate) {
            return Objects.equals(this.targetMethodSignature().name(), candidate.name())
                && Arrays.equals(this.targetMethodSignature().parameterTypes(), candidate.parameterTypes());
          }
        };
      }
    };

    abstract ByMethodSignature create(MethodSignature request);
  }

  interface ByMethodSignature extends MethodMatcher {
    default boolean test(Method method) {
      return matches(MethodSignature.create(method));
    }

    boolean matches(MethodSignature s);

    abstract class Base implements ByMethodSignature {
      private final MethodSignature targetMethodSignature;

      protected Base(MethodSignature targetMethodSignature) {
        this.targetMethodSignature = requireNonNull(targetMethodSignature);
      }

      MethodSignature targetMethodSignature() {
        return this.targetMethodSignature;
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(this.targetMethodSignature.hashCode());
      }

      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass" /* It is actually checking using .getClass() method */)
      @Override
      public boolean equals(Object anotherObject) {
        if (this == anotherObject)
          return true;
        if (anotherObject == null)
          return false;
        Base another = (Base) anotherObject;
        return Objects.equals(this.targetMethodSignature(), another.targetMethodSignature()) &&
            Objects.equals(this.getClass(), another.getClass());
      }

      @Override
      public String toString() {
        return "matcher:" + this.targetMethodSignature();
      }
    }

    static ByMethodSignature create(MethodSignature methodSignature, MethodMatcher.Factory factory) {
      return factory.create(methodSignature);
    }
  }
}
