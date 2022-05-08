package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.AbstractObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.MethodMatcher;
import com.github.dakusui.osynth.core.MethodSignature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.dakusui.osynth.core.utils.MethodUtils.*;

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
    return new MethodHandlerEntry.Builder().matcher(callStrict(methodSignature));
  }

  public static MethodHandlerEntry.Builder method(MethodMatcher matcher) {
    return new MethodHandlerEntry.Builder().matcher(matcher);
  }

  public static <A extends Annotation> MethodMatcher callStrict(MethodSignature signature) {
    return MethodMatcher.createStrict(signature);
  }

  public static <A extends Annotation> MethodMatcher callLenient(MethodSignature signature) {
    return MethodMatcher.createLenient(signature);
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass) {
    return annotatedWith(annotationClass, predicateOverrideToString(p -> "true", v -> true));
  }

  public static <A extends Annotation> MethodMatcher annotatedWith(Class<A> annotationClass, Predicate<A> annotation) {
    return matching(
        () -> String.format("and(has annotation %s, %s)",
            simpleClassNameOf(annotationClass),
            toStringOrCompose(annotation)),
        ((Predicate<Method>) method -> method.isAnnotationPresent(annotationClass)).and(
            method -> annotation.test(method.getAnnotation(annotationClass)))
    );
  }

  public static MethodMatcher matching(Supplier<String> nameComposer, Predicate<Method> p) {
    return MethodMatcher.create(nameComposer, p);
  }
}
