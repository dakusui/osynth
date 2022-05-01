package com.github.dakusui.osynth.neo;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;

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
}
