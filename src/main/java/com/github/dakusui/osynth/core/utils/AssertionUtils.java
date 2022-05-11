package com.github.dakusui.osynth.core.utils;

import com.github.dakusui.osynth.core.*;
import com.github.dakusui.pcond.internals.InternalUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.dakusui.pcond.core.refl.MethodQuery.instanceMethod;
import static com.github.dakusui.pcond.forms.Functions.call;
import static com.github.dakusui.pcond.forms.Functions.parameter;
import static com.github.dakusui.pcond.forms.Matchers.matcherForString;
import static com.github.dakusui.pcond.forms.Predicates.allOf;
import static com.github.dakusui.pcond.forms.Predicates.callp;
import static com.github.dakusui.pcond.forms.Printables.function;
import static com.github.dakusui.pcond.forms.Printables.predicate;
import static java.util.stream.Collectors.toList;

public enum AssertionUtils {
  ;

  public static Function<Class<?>, Method> classGetMethod(String name, Class<?>[] parameterTypes) {
    return call(instanceMethod(parameter(), "getMethod", name, parameterTypes));
  }

  public static Function<SynthesizedObject.Descriptor, Object> descriptorFallbackObject() {
    return call(instanceMethod(parameter(), "fallbackObject"));
  }

  public static Function<SynthesizedObject.Descriptor, List<MethodHandlerEntry>> descriptorMethodHandlerEntries() {
    return call(instanceMethod(parameter(), "methodHandlerEntries"));
  }

  public static Function<SynthesizedObject.Descriptor, List<Class<?>>> descriptorInterfaces() {
    return call(instanceMethod(parameter(), "interfaces"));
  }

  public static Predicate<Object> collectionContainsValue(Collection<?> targetSet, Object value) {
    return callp(instanceMethod(targetSet, "contains", value));
  }

  public static Predicate<Method> methodIsAnnotationPresent(Class<? extends Annotation> annotation) {
    return callp(instanceMethod(parameter(), "isAnnotationPresent", annotation));
  }

  public static <E> Function<Collection<E>, List<E>> collectionDuplicatedElements() {
    return function("duplicatedElements", AssertionUtils::duplicatedElementsIn);
  }

  public static <E> List<E> duplicatedElementsIn(Collection<E> collection) {
    LinkedHashSet<E> duplication = new LinkedHashSet<>();
    collection.stream()
        .filter(each -> !duplication.contains(each))
        .forEach(duplication::add);
    return new ArrayList<>(duplication);
  }

  public static <T> Predicate<Class<T>> classIsInterface() {
    return predicate("isInterface", Class::isInterface);
  }
}
