package com.github.dakusui.osynth.utils;

import com.github.dakusui.pcond.core.printable.PrintableFunctionFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.github.dakusui.pcond.forms.Printables.function;
import static java.util.Arrays.asList;

public enum InternalFunctions {
  ;

  @SuppressWarnings("unchecked")
  public static <T> Function<T, List<Method>> methods() {
    return (Function<T, List<Method>>) Def.METHODS;
  }

  public static <T> Function<T, List<T>> listOf() {
    return PrintableFunctionFactory.function("listOf", Collections::singletonList);
  }

  enum Def {
    ;
    static final Function<Class<?>, List<Method>> METHODS = methodsFunction();

    private static Function<Class<?>, List<Method>> methodsFunction() {
      return function("methods", o -> asList(o.getMethods()));
    }
  }
}
