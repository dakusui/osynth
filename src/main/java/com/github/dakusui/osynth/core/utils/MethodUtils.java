package com.github.dakusui.osynth.core.utils;

import com.github.dakusui.osynth.core.InvocationController;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodSignature;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.exceptions.OsynthException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.core.utils.MessageUtils.messageForMissingMethodHandler;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;
import static com.github.dakusui.pcond.forms.Predicates.transform;
import static com.github.dakusui.valid8j.Assertions.that;
import static java.util.stream.Collectors.joining;

public enum MethodUtils {
  ;

  public static MethodHandler createMethodHandlerFromFallbackObject(final Object fallbackObject, MethodSignature methodSignature) {
    assert that(fallbackObject, isNotNull());
    return createMethodHandlerDelegatingToObject(fallbackObject, methodSignature);
  }

  public static MethodHandler createMethodHandlerDelegatingToObject(Object object, MethodSignature methodSignature) {
    assert object != null;
    return (synthesizedObject, args) -> execute(() -> {
      Method method = getMethodFromClass(synthesizedObject, object.getClass(), methodSignature.name(), methodSignature.parameterTypes());
      method.setAccessible(true);
      return method.invoke(object, args);
    });
  }

  public static Optional<MethodHandler> createMethodHandlerFromInterfaceClass(Class<?> fromClass, MethodSignature methodSignature) {
    return findMethodHandleFor(methodSignature, fromClass).map(MethodUtils::toMethodHandler);
  }

  static MethodHandler toMethodHandler(MethodHandle methodHandle) {
    return (SynthesizedObject synthesizedObject, Object[] arguments) -> execute(
        () -> methodHandle.bindTo(synthesizedObject).invokeWithArguments(arguments));
  }

  static Optional<MethodHandle> findMethodHandleFor(MethodSignature methodSignature, Class<?> fromClass) {
    return findMethodMatchingWith(methodSignature, fromClass).filter(Method::isDefault).map(m -> methodHandleFor(m, fromClass));
  }

  private static MethodHandle methodHandleFor(Method m, Class<?> fromClass) {
    assert that(fromClass, transform(AssertionUtils.classGetMethod(m.getName(), m.getParameterTypes())).check(isNotNull()));
    return execute(() -> createMethodHandlesLookupFor(fromClass).in(fromClass).unreflectSpecial(m, fromClass));
  }

  public static synchronized MethodHandles.Lookup createMethodHandlesLookupFor(Class<?> anInterfaceClass) {
    return execute(() -> {
      Constructor<MethodHandles.Lookup> constructor;
      constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
      constructor.setAccessible(true);
      return constructor.newInstance(anInterfaceClass);
    });
  }

  /**
   * A method to uniform a way to handle exceptions.
   *
   * @param block A block to execute hand to handle possible exceptions.
   * @param <T>   A type of the returned value.
   * @return A returned value from the block.
   */
  public static <T> T execute(FailableSupplier<T> block) {
    return execute(block, Throwable::getMessage);
  }

  public static <T> T execute(FailableSupplier<T> block, Function<Throwable, String> messageComposerOnFailure) {
    try {
      return block.get();
    } catch (Error e) {
      throw e;
    } catch (Throwable e) {
      throw OsynthException.from(messageComposerOnFailure.apply(e), e);
    }
  }

  public static Object[] toEmptyArrayIfNull(Object[] args) {
    if (args == null)
      return InvocationController.EMPTY_ARGS;
    return args;
  }

  public static Optional<MethodHandler> createMethodHandlerFromInterfaces(List<Class<?>> interfaces, MethodSignature methodSignature) {
    return interfaces.stream()
        .map((Class<?> eachInterfaceClass) -> createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  public static boolean isToStringOverridden(Class<?> aClass) {
    return !getMethodFromClass(aClass, "toString").getDeclaringClass().equals(Object.class);
  }

  public static Method getMethodFromClass(Class<?> aClass, String methodName, Class<?>... parameterTypes) {
    return getMethodFromClass(aClass, aClass, methodName, parameterTypes);
  }

  private static Method getMethodFromClass(Object objectForErrorMessageFormatting, Class<?> aClass, String methodName, Class<?>... parameterTypes) {
    try {
      return aClass.getMethod(methodName, parameterTypes);
    } catch (NoSuchMethodException e) {
      throw new UnsupportedOperationException(messageForMissingMethodHandler(methodName, parameterTypes, objectForErrorMessageFormatting, e), e);
    }
  }

  public static String simpleClassNameOf(Class<?> aClass) {
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
        .map(MethodUtils::simpleClassNameOf)
        .collect(joining(",", label + ":(", ")")) +
        m.map(v -> ":declared in " + v).orElse("");
  }

  public static String prettierToString(Object object) {
    if (object == null)
      return "null";
    Class<?> aClass = object.getClass();
    return isToStringOverridden(aClass) ?
        object.toString() :
        simpleClassNameOf(aClass) + "@" + System.identityHashCode(object);
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

  /**
   * An interface to define a block which possibly throws an exception.
   * Intended to be used with {@link MethodUtils#execute} method.
   *
   * @param <T> A type of the value retuned by the block.
   */
  public interface FailableSupplier<T> {
    T get() throws Throwable;
  }

  private static Optional<Method> findMethodMatchingWith(MethodSignature methodSignature, Class<?> fromClass) {
    try {
      return Optional.of(fromClass.getMethod(methodSignature.name(), methodSignature.parameterTypes()));
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }

  public static MethodHandler withName(String name, MethodHandler methodHandler) {
    return new MethodHandler() {
      @Override
      public Object handle(SynthesizedObject synthesizedObject, Object[] objects) throws Throwable {
        return methodHandler.handle(synthesizedObject, objects);
      }

      @Override
      public String toString() {
        return name;
      }
    };
  }
}
