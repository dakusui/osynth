package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.github.dakusui.osynth.core.utils.MethodUtils.*;
import static java.lang.String.format;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer extends AbstractObjectSynthesizer<ObjectSynthesizer> {

  public ObjectSynthesizer() {
    super(new SynthesizedObject.Descriptor.Builder()
        .fallbackObject(DEFAULT_FALLBACK_OBJECT));
  }

  public ObjectSynthesizer(SynthesizedObject.Descriptor descriptor) {
    super(new SynthesizedObject.Descriptor.Builder(descriptor));
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
    return MethodMatcher.overrideToString(mm -> ("matchingExactly:" + mm.toString()), nameMatchingExactly(signature.name()).and(parameterTypesMatchingExactly(signature.parameterTypes())));
  }

  /**
   * Returns a "lenient" method matcher by signature.
   * The returned matcher checks if
   * <p>
   * 1. The name of a method to be tested if it is matching the name of the `targetMethodSignature` as a regular expression.
   * 2. Every parameter types of the method to be tested is equal to or more special than the corresponding parameter type in the `signature`.
   * <p>
   * If the signature doesn't have any parameter types, it matches a method without
   * any parameters.
   * In case you want to create a matcher that matches a method with a specific name but
   * doesn't care any parameter types, use {@link ObjectSynthesizer#nameMatchingRegex(String)}
   * or {@link ObjectSynthesizer#nameMatchingExactly(String)}.
   *
   * @param signature The method signature that matches a returned matcher.
   * @return A method matcher by signature.
   */
  public static MethodMatcher matchingLeniently(MethodSignature signature) {
    return MethodMatcher.overrideToString(mm -> ("matchingLeniently:" + mm.toString()), nameMatchingRegex(signature.name()).and(parameterTypesMatchingLeniently(signature.parameterTypes())));
  }

  public static MethodMatcher nameMatchingExactly(String methodName) {
    return MethodMatcher.create(
        (mm) -> format("nameMatchingExactly[%s]", methodName),
        method -> Objects.equals(methodName, method.getName()));
  }

  public static MethodMatcher nameMatchingRegex(String regexForMethodName) {
    Pattern regex = Pattern.compile(regexForMethodName);
    return MethodMatcher.create(
        (mm) -> format("nameMatchingRegex[%s]", regexForMethodName),
        method -> regex.matcher(method.getName()).matches());
  }

  public static MethodMatcher parameterTypesMatchingExactly(Class<?>[] parameterTypes) {
    return MethodMatcher.create(
        (mm) -> format("parameterTypesMatchingExactly%s", Arrays.toString(parameterTypes)),
        method -> Arrays.equals(parameterTypes, method.getParameterTypes())
    );
  }

  public static MethodMatcher parameterTypesMatchingLeniently(Class<?>[] parameterTypes) {
    return MethodMatcher.create(
        (mm) -> format("parameterTypesMatchingLeniently%s", Arrays.toString(parameterTypes)),
        method -> {
          AtomicInteger i = new AtomicInteger(0);
          return parameterTypes.length == method.getParameterTypes().length &&
              Arrays.stream(parameterTypes)
                  .allMatch(type -> type.isAssignableFrom(parameterTypes[i.getAndIncrement()]));
        }
    );
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass) {
    return matching(
        m -> "annotatedWith(@" + simpleClassNameOf(annotationClass) + ")",
        method -> method.isAnnotationPresent(annotationClass));
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass, Predicate<A> annotationPredicate) {
    return annotatedWith(annotationClass).and(
        matching(
            m -> "satisfying:" + toSlightlyPrettierStringUnlessToStringOverridden(m),
            m -> annotationPredicate.test(m.getAnnotation(annotationClass))));
  }

  public static MethodMatcher matching(Function<MethodMatcher, String> nameComposer, Predicate<Method> p) {
    return MethodMatcher.create(nameComposer, p);
  }
}
