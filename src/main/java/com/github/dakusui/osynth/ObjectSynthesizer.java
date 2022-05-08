package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.*;
import com.github.dakusui.osynth.core.utils.MethodUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.dakusui.osynth.core.MethodMatcher.ByMethodSignature.createStrict;
import static com.github.dakusui.osynth.core.MethodMatcher.createLenient;
import static java.util.stream.Collectors.joining;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer extends AbstractObjectSynthesizer<ObjectSynthesizer> {

  public ObjectSynthesizer() {
  }

  public static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return methodCallStrict(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder methodCallStrict(MethodSignature methodSignature) {
    return new MethodHandlerEntry.Builder().matcher(createStrict(methodSignature));
  }

  public static MethodHandlerEntry.Builder lenientMethodCall(MethodSignature methodSignature) {
    return new MethodHandlerEntry.Builder().matcher(createLenient(methodSignature));
  }

  public static <A extends Annotation> MethodHandlerEntry.Builder methodAnnotatedWith(Class<A> annotationClass) {
    return methodAnnotatedWith(annotationClass, v -> true);
  }

  public static <A extends Annotation> MethodHandlerEntry.Builder methodAnnotatedWith(Class<A> annotationClass, Predicate<A> annotation) {
    return methodSatisfies(((Predicate<Method>) method -> method.isAnnotationPresent(annotationClass)).and(method -> annotation.test(method.getAnnotation(annotationClass))));
  }

  public static MethodHandlerEntry.Builder methodSatisfies(Predicate<? super Method> p) {
    return methodSatisfies(MethodUtils.composeSimpleClassName(p.getClass()), p);
  }

  public static MethodHandlerEntry.Builder methodSatisfies(String name, Predicate<? super Method> p) {
    return methodSatisfies(() -> name, p);
  }

  public static MethodHandlerEntry.Builder methodSatisfies(Supplier<String> nameComposer, Predicate<? super Method> p) {
    return methodMatching(MethodMatcher.create(nameComposer, p));
  }

  public static MethodHandlerEntry.Builder methodMatching(MethodMatcher matcher) {
    return new MethodHandlerEntry.Builder().matcher(matcher);
  }

  public static class ComposeSimpleClassNameTest {

    public static void main(String... args) {
      {
        Predicate<Object> p = v -> true;
        Predicate<Object> q = new Predicate<Object>() {
          @Override
          public boolean test(Object o) {
            return false;
          }
        };
        Predicate<Object> r = new Supplier<Predicate<Object>>() {

          @Override
          public Predicate<Object> get() {
            return v -> v == p;
          }
        }.get();
        System.out.println("= lamda");
        System.out.println("custom:lambda:   " + MethodUtils.composeSimpleClassName(p.getClass()));
        System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
        System.out.println("= anonymous class");
        System.out.println("custom:anonymous:" + MethodUtils.composeSimpleClassName(q.getClass()));
        System.out.println("normal:anonymous:" + q.getClass().getSimpleName());
        System.out.println("= normal class");
        System.out.println("custom:static:   " + MethodUtils.composeSimpleClassName(ComposeSimpleClassNameTest.class));
        System.out.println("normal:static:   " + ComposeSimpleClassNameTest.class.getSimpleName());
        System.out.println("= lambda defined inside anonymous");
        System.out.println("custom:lambda:   " + MethodUtils.composeSimpleClassName(p.getClass()));
        System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
        System.out.println("canonical:lambda:" + p.getClass().getCanonicalName());
      }
    }
  }
}
