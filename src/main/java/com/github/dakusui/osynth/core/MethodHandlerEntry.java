package com.github.dakusui.osynth.core;

import java.util.Objects;

import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerDelegatingToObject;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;

public interface MethodHandlerEntry {
  MethodMatcher matcher();

  MethodHandler handler();

  static MethodHandlerEntry create(MethodMatcher matcher, MethodHandler handler) {
    requireNonNull(matcher);
    requireNonNull(handler);
    return new MethodHandlerEntry() {
      @Override
      public MethodMatcher matcher() {
        return matcher;
      }

      @Override
      public MethodHandler handler() {
        return handler;
      }

      @Override
      public String toString() {
        return String.format("(%s,%s)", matcher, handler);
      }

      @Override
      public int hashCode() {
        return Objects.hashCode(this.matcher());
      }

      @Override
      public boolean equals(Object anotherObject) {
        if (anotherObject == this)
          return true;
        if (!(anotherObject instanceof MethodHandlerEntry))
          return false;
        MethodHandlerEntry another = (MethodHandlerEntry) anotherObject;
        return Objects.equals(this.matcher(), another.matcher())
            && Objects.equals(this.handler(), another.handler());
      }
    };
  }

  class Builder {
    MethodHandler handler;
    private MethodMatcher matcher;

    public Builder() {
    }

    public Builder matcher(MethodMatcher matcher) {
      this.matcher = requireNonNull(matcher);
      return this;
    }

    public Builder handler(MethodHandler handler) {
      this.handler = requireNonNull(handler);
      return this;
    }

    public MethodHandlerEntry build() {
      return create(this.matcher, handler);
    }

    public MethodHandlerEntry with(MethodHandler handler) {
      return this.handler(handler).build();
    }

    public MethodHandlerEntry delegatingTo(Object object) {
      requireNonNull(object);
      return this.handler((synthesizedObject, args) -> createMethodHandlerDelegatingToObject(
              object,
              MethodSignature.create(InvocationController.invocationContext().invokedMethod())).handle(synthesizedObject, args))
          .build();
    }
  }
}
