package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.AbstractObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.MethodMatcher;
import com.github.dakusui.osynth.core.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.github.dakusui.osynth.core.utils.MethodUtils.*;
import static java.lang.String.format;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer extends AbstractObjectSynthesizer<ObjectSynthesizer> {

  public ObjectSynthesizer() {
  }

  public static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return methodCall(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder methodCall(MethodSignature methodSignature) {
    return new MethodHandlerEntry.Builder().matcher(matchingExactly(methodSignature));
  }

  public static MethodHandlerEntry.Builder methodCall(MethodMatcher matcher) {
    return new MethodHandlerEntry.Builder().matcher(matcher);
  }

  public static MethodMatcher matchingExactly(MethodSignature signature) {
    return nameMatchingExactly(signature.name()).and(parameterTypesMatchingExactly(signature.parameterTypes()));
  }

  /**
   * Returns a "lenient" method matcher by signature.
   * The returned matcher checks if
   *
   * 1. The name of a method to be tested is equal to the `targetMethodSignature`.
   * 2. Every parameter types of the method to be tested is equal to or more special than the corresponding parameter type in the `signature`.
   *
   * @param signature The method signature that matches a returned matcher.
   * @return A method matcher by signature.
   */
  public static MethodMatcher matchingLeniently(MethodSignature signature) {
    return nameMatchingRegex(signature.name()).and(parameterTypesMatchingLeniently(signature.parameterTypes()));
  }

  public static MethodMatcher nameMatchingExactly(String methodName) {
    return MethodMatcher.create(
        () -> format("nameMatchingExactly[%s]", methodName),
        method -> Objects.equals(methodName, method.getName()));
  }

  public static MethodMatcher nameMatchingRegex(String regexForMethodName) {
    Pattern regex = Pattern.compile(regexForMethodName);
    return MethodMatcher.create(
        () -> format("nameMatchingRegex[%s]", regexForMethodName),
        method -> regex.matcher(method.getName()).matches());
  }

  public static MethodMatcher parameterTypesMatchingExactly(Class<?>[] parameterTypes) {
    return MethodMatcher.create(
        () -> format("parameterTypesMatchingExactly%s", Arrays.toString(parameterTypes)),
        method -> Arrays.equals(parameterTypes, method.getParameterTypes())
    );
  }

  public static MethodMatcher parameterTypesMatchingLeniently(Class<?>[] parameterTypes) {
    return MethodMatcher.create(
        () -> format("parameterTypesMatchingLeniently%s", Arrays.toString(parameterTypes)),
        method -> {
          AtomicInteger i = new AtomicInteger(0);
          return parameterTypes.length == method.getParameterTypes().length &&
              Arrays.stream(parameterTypes)
                  .allMatch(type -> type.isAssignableFrom(parameterTypes[i.getAndIncrement()]));
        }
    );
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass) {
    return annotatedWith(annotationClass, predicateOverrideToString(p -> "true", v -> true));
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass, Predicate<A> annotation) {
    return matching(
        () -> format("and(has annotation %s, %s)", simpleClassNameOf(annotationClass), toStringOrCompose(annotation)),
        ((Predicate<Method>) method -> method.isAnnotationPresent(annotationClass)).and(
            method -> annotation.test(method.getAnnotation(annotationClass)))
    );
  }

  public static MethodMatcher matching(Supplier<String> nameComposer, Predicate<Method> p) {
    return MethodMatcher.create(nameComposer, p);
  }
}
