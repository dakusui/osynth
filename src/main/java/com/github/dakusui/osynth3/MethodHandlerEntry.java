package com.github.dakusui.osynth3;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.Preconditions.requireState;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;

public interface MethodHandlerEntry {
  MethodSignature signature();

  MethodHandler handler();

  static MethodHandlerEntry create(MethodSignature signature, MethodHandler handler) {
    requireNonNull(signature);
    requireNonNull(handler);
    return new MethodHandlerEntry() {
      @Override
      public MethodSignature signature() {
        return signature;
      }

      @Override
      public MethodHandler handler() {
        return handler;
      }
    };
  }

  class Builder {
    MethodSignature signature;
    MethodHandler   handler;

    public Builder() {
    }

    public Builder signature(MethodSignature signature) {
      this.signature = requireNonNull(signature);
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

      return new MethodHandlerEntry() {
        final MethodSignature signature = requireState(Builder.this.signature, isNotNull());
        final MethodHandler handler = requireState(Builder.this.handler, isNotNull());

        @Override
        public MethodSignature signature() {
          return this.signature;
        }

        @Override
        public MethodHandler handler() {
          return this.handler;
        }
      };
    }
  }
}
