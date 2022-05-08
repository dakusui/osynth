package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer extends AbstractObjectSynthesizer<ObjectSynthesizer> {

  public ObjectSynthesizer() {
  }

  public static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return methodCall(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder methodCall(MethodSignature methodRequest) {
    return new MethodHandlerEntry.Builder().handle(methodRequest);
  }

  public static MethodHandlerEntry.Builder methodSatisfies(Predicate<? super Method> p) {
    return methodSatisfies(WipUtils.composeSimpleClassName(p.getClass()), p);
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

  public static enum WipUtils {
    ;

    public static boolean isToStringOverridden(Class<?> aClass) {
      try {
        return !aClass.getMethod("toString").getDeclaringClass().equals(Object.class);
      } catch (NoSuchMethodException e) {
        throw new AssertionError(e);
      }
    }

    public static String composeSimpleClassName(Class<?> aClass) {
      if (aClass.getSimpleName().length() > 0 && !aClass.isSynthetic())
        return aClass.getSimpleName();
      final String label;
      final Optional<String> m;
      if (aClass.isSynthetic()) {
        label = "lambda";
        m = Optional.of(enclosingClassNameOfLambda(aClass.getCanonicalName()));
      } else {
        label = "anonymous";
        m = Optional.empty();
      }
      return streamSupertypes(aClass)
          .filter(each -> !Objects.equals(Object.class, each))
          .map(WipUtils::composeSimpleClassName)
          .collect(joining(",", label + ":(", ")")) +
          m.map(v -> ":declared in " + v).orElse("");
    }

    private static String enclosingClassNameOfLambda(String canonicalNameOfLambda) {
      String ret = canonicalNameOfLambda.substring(0, canonicalNameOfLambda.lastIndexOf("$$"));
      int b = ret.lastIndexOf("$");
      if (b < 0)
        return ret;
      return ret.substring(b + "$".length());
    }

    private static Stream<Class<?>> streamSupertypes(Class<?> klass) {
      return Stream.concat(
              Stream.of(klass.getSuperclass()),
              Arrays.stream(klass.getInterfaces()))
          .filter(Objects::nonNull);
    }
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
        System.out.println("custom:lambda:   " + WipUtils.composeSimpleClassName(p.getClass()));
        System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
        System.out.println("= anonymous class");
        System.out.println("custom:anonymous:" + WipUtils.composeSimpleClassName(q.getClass()));
        System.out.println("normal:anonymous:" + q.getClass().getSimpleName());
        System.out.println("= normal class");
        System.out.println("custom:static:   " + WipUtils.composeSimpleClassName(ComposeSimpleClassNameTest.class));
        System.out.println("normal:static:   " + ComposeSimpleClassNameTest.class.getSimpleName());
        System.out.println("= lambda defined inside anonymous");
        System.out.println("custom:lambda:   " + WipUtils.composeSimpleClassName(p.getClass()));
        System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
        System.out.println("canonical:lambda:" + p.getClass().getCanonicalName());
      }
    }
  }
}
