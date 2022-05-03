package com.github.dakusui.osynth.utils;

import com.github.dakusui.osynth.neo.MethodHandler;
import com.github.dakusui.osynth.neo.MethodSignature;
import com.github.dakusui.osynth.neo.SynthesizedObject;
import com.github.dakusui.osynth.neo.annotations.BuiltInHandlerFactory;
import com.github.dakusui.pcond.core.refl.MethodQuery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.dakusui.pcond.core.refl.MethodQuery.instanceMethod;
import static com.github.dakusui.pcond.functions.Functions.*;
import static com.github.dakusui.pcond.functions.Functions.call;
import static com.github.dakusui.pcond.functions.Predicates.callp;

public enum AssertionUtils {
  ;

  public static Function<Class<?>, Method> classGetMethod(String name, Class<?>[] parameterTypes) {
    return call(instanceMethod(parameter(), "getMethod", name, parameterTypes));
  }

  public static Predicate<Class<?>> classIsInterface() {
    return callp(MethodQuery.instanceMethod(parameter(), "isInterface"));
  }

  public static Function<SynthesizedObject, Object> synthesizedObjectFallbackObject() {
    return call(MethodQuery.instanceMethod(parameter(), "fallbackObject"));
  }

  public static Function<SynthesizedObject.Descriptor, Object> descriptorFallbackObject() {
   return call(instanceMethod(parameter(), "fallbackObject"));
 }

  public static Function<SynthesizedObject.Descriptor, Map<MethodSignature, MethodHandler>> descriptorMethodHandlers() {
  return call(instanceMethod(parameter(), "methodHandlers"));
}

  public static Function<SynthesizedObject.Descriptor, ClassLoader> descriptorClassLoader() {
 return call(instanceMethod(parameter(), "classLoader"));
}

  public static Function<SynthesizedObject.Descriptor, List<Class<?>>> descriptorInterfaces() {
 return call(instanceMethod(parameter(), "interfaces"));
}

  public static Function<Map<?, ?>, Collection<?>> mapKeySet(Object value) {
    return call(instanceMethod(value, "keySet"));
  }

  public static Predicate<Object> collectionContainsValue(Collection<?> targetSet, Object value) {
    return callp(instanceMethod(targetSet, "contains", value));
  }

  public static Predicate<Method> methodIsAnnotationPresent(Class<? extends Annotation> annotation) {
    return callp(instanceMethod(parameter(), "isAnnotationPresent", annotation));
  }
}
