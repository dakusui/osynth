package com.github.dakusui.osynth.utils;

import com.github.dakusui.pcond.core.currying.CurriedFunction;
import com.github.dakusui.pcond.core.printable.ParameterizedPredicateFactory;
import com.github.dakusui.pcond.functions.Experimentals;
import com.github.dakusui.pcond.functions.Printables;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

import static com.github.dakusui.pcond.functions.Functions.curry;
import static com.github.dakusui.pcond.functions.Printables.predicate;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public enum InternalPredicates {
  ;

  public static Predicate<Class<?>> isInterfaceClass() {
    return Def.IS_INTERFACE_CLASS;
  }

  public static Predicate<Method> isDefaultMethod() {
    return Def.IS_DEFAULT_METHOD;
  }

  public static Predicate<Class<?>> isAssignableFrom(Class<?> aClass) {
    return Def.FACTORY_IS_ASSIGNABLE_FROM.create(aClass);
  }

  @SuppressWarnings("unchecked")
  public static <T> Predicate<T> matchesAnyOf(List<?> values, Predicate<T> cond) {
    return (Predicate<T>) Def.MATCHES_ANY_OF.create(values, cond);
  }

  public static void main(String... args) {
    System.out.println(matchesAnyOf(asList("hello", "world"), Predicate.isEqual("WORLD")).test("X"));
  }

  public static CurriedFunction<Object, Object> isAssignableFrom() {
    return curry(InternalPredicates.class, "isAssignableFrom", Class.class, Class.class);
  }

  @SuppressWarnings("unused")
  public static boolean isAssignableFrom(Class<?> a, Class<?> b) {
    return a.isAssignableFrom(b);
  }

  enum Def {
    ;

    private static final Predicate<Method>                       IS_DEFAULT_METHOD          = isDefaultMethodPredicate();
    private static final Predicate<Class<?>>                     IS_INTERFACE_CLASS         = isInterfaceClassPredicate();
    private static final ParameterizedPredicateFactory<Class<?>> FACTORY_IS_ASSIGNABLE_FROM = isAssignableFromPredicateFactory();
    private static final ParameterizedPredicateFactory<List<?>>  MATCHES_ANY_OF             = matchesAnyOfPredicateFactory();

    private static Predicate<Method> isDefaultMethodPredicate() {
      return predicate("isDefaultMethod", Method::isDefault);
    }

    private static Predicate<Class<?>> isInterfaceClassPredicate() {
      return Printables.predicate("isInterface", Class::isInterface);
    }

    public static ParameterizedPredicateFactory<Class<?>> isAssignableFromPredicateFactory() {
      return (ParameterizedPredicateFactory<Class<?>>) Experimentals.<Class<?>>parameterizedPredicate("(dummy)")
          .formatterFactory(args -> () -> "isAssignableFrom[" + ((Class<?>) args.get(0)).getName() + "]")
          .factory(args -> testedClass -> testedClass.isAssignableFrom((Class<?>) args.get(0)));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ParameterizedPredicateFactory<List<?>> matchesAnyOfPredicateFactory() {
      return (ParameterizedPredicateFactory<List<?>>) Experimentals.<List<?>>parameterizedPredicate("(dummy)")
          .formatterFactory(args -> () -> format("matchesAnyOf[%s,%s]", args.get(0), args.get(1)))
          .factory(args -> value -> ((List<?>) args.get(0)).stream().anyMatch(v -> false));
    }
  }
}
