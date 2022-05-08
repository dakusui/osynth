package com.github.dakusui.osynth.core;

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
  }
}
