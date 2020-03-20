package com.github.dakusui.osynth.utils;

import com.github.dakusui.pcond.functions.PrintableFunction;
import com.github.dakusui.pcond.functions.Printables;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.dakusui.pcond.functions.Printables.function;
import static com.github.dakusui.pcond.internals.InternalUtils.formatObject;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

public enum InternalFunctions {
  ;
  static final Function<Class<?>, List<Method>> METHODS = methodsFunction();

  private static Function<Class<?>, List<Method>> methodsFunction() {
    return function("methods", o -> asList(o.getMethods()));
  }

  public static Function<Collection<?>, List<?>> cartesianWith(Collection<?>... inners) {
    return Def.FACTORY_CARTESIAN_WITH.create(asList(inners));
  }

  enum Def {
    ;

    public static final PrintableFunction.Factory<Collection<?>, List<?>, List<Collection<?>>> FACTORY_CARTESIAN_WITH = factoryCartesianWith();

    private static PrintableFunction.Factory<Collection<?>, List<?>, List<Collection<?>>> factoryCartesianWith() {
      return Printables.functionFactory(
          collections -> "cartesianWith" + formatObject(collections),
          inners -> outer -> (List<?>) cartesian((Stream<?>) outer, inners.stream().map(Collection::stream).collect(toList())));
    }

    private static Stream<List<?>> cartesian(Stream<?> outer, List<Stream<?>> inners) {
      Stream<List<?>> ret = wrapWithList(outer);
      for (Stream<?> i : inners)
        ret = cartesianPrivate(ret, i);
      return ret;
    }

    private static Stream<List<?>> cartesianPrivate(Stream<List<?>> outer, Stream<?> inner) {
      return outer.flatMap(i -> inner.map(j -> new ArrayList<Object>(i) {{
        add(j);
      }}));
    }

    private static Stream<List<?>> wrapWithList(Stream<?> stream) {
      return stream.map(Collections::singletonList);
    }

    private static Stream<List<Object>> cartesian(List<Object> a, List<String> b) {
      return a.stream().flatMap(o -> b.stream().map(p -> asList(p, b)));
    }
  }
}
