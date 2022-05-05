package com.github.dakusui.osynth2.core;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.Preconditions.requireState;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;

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
    };
  }

  class Builder {
    MethodMatcher matcher;
    MethodHandler handler;

    public Builder() {
    }

    public Builder matcher(MethodMatcher signature) {
      this.matcher = requireNonNull(signature);
      return this;
    }

    public Builder handler(MethodHandler handler) {
      this.handler = requireNonNull(handler);
      return this;
    }

    public MethodHandlerEntry with(MethodHandler handler) {
      return this.handler(handler).build();
    }

    public MethodHandlerEntry build() {
      return create(matcher, handler);
    }
  }
}
