package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

@FunctionalInterface
public interface FallbackMethodHandlerFactory extends Function<ProxyDescriptor, Function<Method, Optional<BiFunction<Object, Object[], Object>>>> {
  default FallbackMethodHandlerFactory compose(FallbackMethodHandlerFactory before) {
    return proxyDescriptor -> method -> {
      Optional<BiFunction<Object, Object[], Object>> ret = before.apply(proxyDescriptor).apply(method);
      if (ret.isPresent())
        return ret;
      return FallbackMethodHandlerFactory.this.apply(proxyDescriptor).apply(method);
    };
  }
}
