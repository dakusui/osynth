package com.github.dakusui.osynth.compat.core;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface FallbackHandlerFactory extends Function<ProxyDescriptor, Function<Method, Optional<BiFunction<Object, Object[], Object>>>> {
  default FallbackHandlerFactory compose(FallbackHandlerFactory before) {
    return proxyDescriptor -> method -> {
      Optional<BiFunction<Object, Object[], Object>> ret = before.apply(proxyDescriptor).apply(method);
      if (ret.isPresent())
        return ret;
      return FallbackHandlerFactory.this.apply(proxyDescriptor).apply(method);
    };
  }
}
