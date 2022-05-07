package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.requireNonNull;

public interface MethodMatcher {
  default boolean matches(Method m) {
    return matches(MethodSignature.create(m));
  }

  boolean matches(MethodSignature s);

  enum Factory {
    /**
     * It does not make sense to use a matcher created by this factory with
     * {@link com.github.dakusui.osynth.invocationcontrollers.StandardInvocationController}.
     * Because the controller searches for a custom method handler for an invoked
     * method by "exact match", always.
     */
    LENIENT {
      @Override
      MethodSignatureMatcher create(MethodSignature handlableMethod) {
        return new MethodSignatureMatcher.Base(handlableMethod) {
          @Override
          public boolean matches(MethodSignature candidate) {
            AtomicInteger i = new AtomicInteger(0);
            return Objects.equals(handlableMethod().name(), candidate.name()) &&
                handlableMethod().parameterTypes().length == candidate.parameterTypes().length &&
                Arrays.stream(handlableMethod().parameterTypes())
                    .allMatch(type -> type.isAssignableFrom(candidate.parameterTypes()[i.getAndIncrement()]));
          }
        };
      }
    },
    STRICT {
      @Override
      MethodSignatureMatcher create(MethodSignature handlableMethod) {
        return new MethodSignatureMatcher.Base(handlableMethod) {
          @Override
          public boolean matches(MethodSignature candidate) {
            return Objects.equals(this.handlableMethod().name(), candidate.name())
                && Arrays.equals(this.handlableMethod().parameterTypes(), candidate.parameterTypes());
          }
        };
      }
    };

    abstract MethodSignatureMatcher create(MethodSignature request);
  }

  interface MethodSignatureMatcher extends MethodMatcher {
    MethodSignature handlableMethod();

    abstract class Base implements MethodSignatureMatcher {
      private final MethodSignature handlableMethod;

      protected Base(MethodSignature handlableMethod) {
        this.handlableMethod = requireNonNull(handlableMethod);
      }

      @Override
      public MethodSignature handlableMethod() {
        return this.handlableMethod;
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(this.handlableMethod.hashCode());
      }

      @SuppressWarnings("EqualsWhichDoesntCheckParameterClass" /* It is actually checking using .getClass() method */)
      @Override
      public boolean equals(Object anotherObject) {
        if (this == anotherObject)
          return true;
        if (anotherObject == null)
          return false;
        Base another = (Base) anotherObject;
        return Objects.equals(this.handlableMethod(), another.handlableMethod()) &&
            Objects.equals(this.getClass(), another.getClass());
      }

      @Override
      public String toString() {
        return "matcher:" + this.handlableMethod();
      }
    }

    static MethodSignatureMatcher create(MethodSignature methodSignature, MethodMatcher.Factory factory) {
      return factory.create(methodSignature);
    }
  }
}
