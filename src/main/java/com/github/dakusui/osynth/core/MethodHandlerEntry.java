package com.github.dakusui.osynth.core;

import static com.github.dakusui.osynth.core.utils.MethodUtils.createMethodHandlerDelegatingToObject;
import static com.github.dakusui.valid8j.Requires.requireNonNull;

public interface MethodHandlerEntry {
  MethodMatcher matcher();

  MethodHandler handler();

  boolean isBuiltIn();

  static MethodHandlerEntry create(MethodMatcher matcher, MethodHandler handler, boolean isBuiltIn) {
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
      public boolean isBuiltIn() {
        return isBuiltIn;
      }

      @Override
      public String toString() {
        return String.format("(%s,%s)", matcher, handler);
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
      return create(this.matcher, handler, false);
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
