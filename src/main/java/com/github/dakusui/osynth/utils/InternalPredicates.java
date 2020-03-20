package com.github.dakusui.osynth.utils;

import com.github.dakusui.pcond.functions.PrintablePredicate;
import com.github.dakusui.pcond.functions.Printables;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.dakusui.pcond.functions.Printables.predicate;

public enum InternalPredicates {
  ;

  @SuppressWarnings("unchecked")
  public static <T> Function<T, List<Method>> methods() {
    return (Function<T, List<Method>>) InternalFunctions.METHODS;
  }

  public static Predicate<Class<?>> isInterfaceClass() {
    return Def.IS_INTERFACE_CLASS;
  }

  public static Predicate<Method> isDefaultMethod() {
    return Def.IS_DEFAULT_METHOD;
  }

  public static Predicate<Class<?>> isAssignableFrom(Class<?> aClass) {
    return Def.FACTORY_IS_ASSIGNABLE_FROM.create(aClass);
  }

  enum Def {
    ;

    private static final Predicate<Method> IS_DEFAULT_METHOD = isDefaultMethodPredicate();
    private static final Predicate<Class<?>> IS_INTERFACE_CLASS = isInterfaceClassPredicate();
    private static final PrintablePredicate.Factory<Class<?>, Class<?>> FACTORY_IS_ASSIGNABLE_FROM = isAssignableFromPredicateFactory();

    private static Predicate<Method> isDefaultMethodPredicate() {
      return predicate("isDefaultMethod", Method::isDefault);
    }

    private static Predicate<Class<?>> isInterfaceClassPredicate() {
      return Printables.predicate("isInterface", Class::isInterface);
    }

    public static PrintablePredicate.Factory<Class<?>, Class<?>> isAssignableFromPredicateFactory() {
      return Printables.predicateFactory(
          v -> "isAssignableFrom[" + v.getName() + "]",
          aClass1 -> testedClass -> testedClass.isAssignableFrom(aClass1));
    }
  }
}
