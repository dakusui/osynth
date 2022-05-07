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
    MethodSignature       methodRequest;
    MethodHandler         handler;
    MethodMatcher.Factory matcherFactory;

    public Builder() {
      this.strictly();
    }

    public Builder handle(MethodSignature methodRequest) {
      this.methodRequest = requireNonNull(methodRequest);
      return this;
    }

    public Builder strictly() {
      return this.createMatcherWith(MethodMatcher.Factory.STRICT);
    }

    public Builder leniently() {
      return this.createMatcherWith(MethodMatcher.Factory.LENIENT);
    }

    public MethodHandlerEntry with(MethodHandler handler) {
      return this.handler(handler).build();
    }

    public Builder handler(MethodHandler handler) {
      this.handler = requireNonNull(handler);
      return this;
    }

    public Builder createMatcherWith(MethodMatcher.Factory matcherFactory) {
      this.matcherFactory = requireNonNull(matcherFactory);
      return this;
    }

    public MethodHandlerEntry build() {
      return create(this.matcherFactory.create(this.methodRequest), handler);
    }
  }
}
