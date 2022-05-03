package com.github.dakusui.osynth3;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import static com.github.dakusui.osynth.utils.AssertionUtils.*;
import static com.github.dakusui.osynth.utils.Messages.failedToInstantiate;
import static com.github.dakusui.osynth.utils.Messages.formatMessageForMissingMethodHandler;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.functions.Predicates.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public enum MethodUtils {
  ;

  static MethodHandler createMethodHandlerFromFallbackObject(final Object fallbackObject, MethodSignature methodSignature) {
    Objects.requireNonNull(fallbackObject);
    return (synthesizedObject, args) -> {
      try {
        assert synthesizedObject != null &&
            synthesizedObject.fallbackObject() == fallbackObject;
        Method method = fallbackObject.getClass().getMethod(
            methodSignature.name(),
            methodSignature.parameterClasses());
        method.setAccessible(true);
        return method
            .invoke(fallbackObject, args);
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException(formatMessageForMissingMethodHandler(methodSignature, synthesizedObject, e), e);
      } catch (InvocationTargetException |
               IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static Optional<MethodHandler> createMethodHandlerFromInterfaceClass(Class<?> fromClass, MethodSignature methodSignature) {
    return findMethodHandleFor(methodSignature, fromClass).map(MethodUtils::toMethodHandler);
  }

  static MethodHandler toMethodHandler(MethodHandle methodHandle) {
    return (SynthesizedObject synthesizedObject, Object[] arguments) -> {
      try {
        return methodHandle.bindTo(synthesizedObject).invokeWithArguments(
            arguments == null ?
                emptyList() :
                asList(arguments));
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }

  static Optional<MethodHandle> findMethodHandleFor(MethodSignature methodSignature, Class<?> fromClass) {
    //require(fromClass, allOf(isNotNull(), classIsInterface()));
    return findMethodMatchingWith(methodSignature, fromClass)
        .filter(Method::isDefault)
        .map(m -> methodHandleFor(m, fromClass));
  }

  private static MethodHandle methodHandleFor(Method m, Class<?> fromClass) {
    assert that(fromClass, transform(classGetMethod(m.getName(), m.getParameterTypes())).check(isNotNull()));
    try {
      return createMethodHandlesLookupFor(fromClass)
          .in(fromClass)
          .unreflectSpecial(m, fromClass);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static MethodHandles.Lookup createLookup() {
    return MethodHandles.lookup();
  }

  private static synchronized MethodHandles.Lookup createMethodHandlesLookupFor(Class<?> anInterfaceClass) {
    Constructor<MethodHandles.Lookup> constructor;
    try {
      constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
      constructor.setAccessible(true);
      try {
        return constructor.newInstance(anInterfaceClass);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    } catch (Throwable e) {
      throw new RuntimeException(failedToInstantiate(anInterfaceClass), e);
    }
  }

  private static Optional<Method> findMethodMatchingWith(MethodSignature methodSignature, Class<?> fromClass) {
    try {
      return Optional.of(fromClass.getMethod(methodSignature.name(), methodSignature.parameterClasses()));
    } catch (NoSuchMethodException e) {
      return Optional.empty();
    }
  }
}
