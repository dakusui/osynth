package com.github.dakusui.osynth2.core.utils;

import com.github.dakusui.osynth2.core.*;
import com.github.dakusui.osynth2.exceptions.OsynthException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.github.dakusui.osynth2.core.utils.MessageUtils.messageForMissingMethodHandler;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;
import static com.github.dakusui.pcond.forms.Predicates.transform;

public enum MethodUtils {
  ;

  public static MethodHandler createMethodHandlerFromFallbackObject(final Object fallbackObject, MethodSignature methodSignature) {
    Objects.requireNonNull(fallbackObject);
    return (synthesizedObject, args) -> execute(() -> {
      try {
        Method method = fallbackObject.getClass().getMethod(methodSignature.name(), methodSignature.parameterTypes());
        method.setAccessible(true);
        return method.invoke(fallbackObject, args);
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException(messageForMissingMethodHandler(methodSignature, synthesizedObject, e), e);
      }
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
    //require(fromClass, allOf(isNotNull(), classIsInterface()));
    return findMethodMatchingWith(methodSignature, fromClass).filter(Method::isDefault).map(m -> methodHandleFor(m, fromClass));
  }

  private static MethodHandle methodHandleFor(Method m, Class<?> fromClass) {
    assert that(fromClass, transform(AssertionUtils.classGetMethod(m.getName(), m.getParameterTypes())).check(isNotNull()));
    return execute(() -> createMethodHandlesLookupFor(fromClass).in(fromClass).unreflectSpecial(m, fromClass));
  }

  private static synchronized MethodHandles.Lookup createMethodHandlesLookupFor(Class<?> anInterfaceClass) {
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
