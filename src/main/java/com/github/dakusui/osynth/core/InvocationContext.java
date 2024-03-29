package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;

import static java.util.Objects.requireNonNull;

public interface InvocationContext {
  ThreadLocal<InvocationContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

  Method invokedMethod();

  static InvocationContext forCurrentThread() {
    return requireNonNull(CONTEXT_THREAD_LOCAL.get());
  }

  class Impl implements InvocationContext {
    private static void forCurrentThreadOrNewOneWith(Method invokedMethod) {
      createContextIfNotYet().invokedMethod(invokedMethod);
    }

    static void contextWith(Method invokedMethod) {
      Impl.forCurrentThreadOrNewOneWith(invokedMethod);
    }

    private static Impl createContextIfNotYet() {
      Impl ret = (Impl) CONTEXT_THREAD_LOCAL.get();
      if (ret == null) {
        ret = new Impl();
        CONTEXT_THREAD_LOCAL.set(ret);
      }
      return ret;
    }

    private Method invokedMethod;

    void invokedMethod(Method invokedMethod) {
      this.invokedMethod = invokedMethod;
    }

    @Override
    public Method invokedMethod() {
      return this.invokedMethod;
    }
  }
}
