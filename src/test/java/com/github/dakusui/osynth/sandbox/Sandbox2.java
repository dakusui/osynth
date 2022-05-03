package com.github.dakusui.osynth.sandbox;

import com.github.dakusui.osynth.neo.MethodHandler;
import com.github.dakusui.osynth.neo.MethodSignature;
import com.github.dakusui.osynth.neo.SynthesizedObject;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.dakusui.osynth.sandbox.Sandbox.createProxy;
import static com.github.dakusui.osynth.utils.AssertionUtils.*;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.functions.Predicates.*;
import static java.util.Arrays.asList;

@SuppressWarnings("NewClassNamingConvention")
public class Sandbox2 {
  interface InterfaceWithDefaultMethod {
    @SuppressWarnings("unused")
    default String defaultMethod(String yourName) {
      return "Hello, " + yourName;
    }
  }

  interface InterfaceWithDefaultMethodMadeAbstractAgain extends InterfaceWithDefaultMethod {
    @Override
    String defaultMethod(String yourName);
  }

  @Test
  public void tryFindMethodHandleFor() {
    Class<?> targetInterfaceClass = InterfaceWithDefaultMethod.class;
    String targetMethodName = "defaultMethod";
    MethodHandler methodHandler = MethodUtils.createMethodHandlerFromInterfaceClass(
            targetInterfaceClass, MethodSignature.create(targetMethodName, String.class)
        )
        .orElseThrow(RuntimeException::new);
    assertThat(
        applyMethodHandler(
            createDummyProxy(SynthesizedObject.class, targetInterfaceClass),
            methodHandler,
            "Scott Tiger"),

        allOf(
            isEqualTo("Hello, Scott Tiger")));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void givenDefaultMethodMadeAbstractAgainInSubInterface() {
    Class<?> targetInterfaceClass = InterfaceWithDefaultMethodMadeAbstractAgain.class;
    String targetMethodName = "defaultMethod";
    MethodUtils.createMethodHandlerFromInterfaceClass(
            targetInterfaceClass, MethodSignature.create(targetMethodName, String.class)
        )
        .orElseThrow(UnsupportedOperationException::new);
  }

  private SynthesizedObject createDummyProxy(Class<?>... classes) {
    return (SynthesizedObject) createProxy(
        (proxy, method, args) -> {
          throw new RuntimeException(String.format("A dummy invocation handler was invoked with proxy: %s method: %s, args: %s", proxy, method, Arrays.toString(args)));
        },
        classes);
  }

  private Object applyMethodHandler(SynthesizedObject synthesizedObject, MethodHandler methodHandler, Object... args) {
    return methodHandler.apply(synthesizedObject, args);
  }

  enum MethodUtils {
    ;

    static class OsynthInvocationHandler implements InvocationHandler {
      final         Map<MethodSignature, MethodHandler> methodHandlers;
      final         List<Class<?>>                      interfaceClasses;
      private final Object                              fallbackObject;

      OsynthInvocationHandler(Map<MethodSignature, MethodHandler> methodHandlers, List<Class<?>> interfaceClasses, Object fallbackObject) {
        this.methodHandlers = methodHandlers;
        this.interfaceClasses = interfaceClasses;
        this.fallbackObject = fallbackObject;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) {
        assert that(proxy, and(isNotNull(), isInstanceOf(SynthesizedObject.class)));
        MethodHandler methodHandler;
        MethodSignature methodSignature = MethodSignature.create(method);
        if (methodHandlers.containsKey(methodSignature))
          methodHandler = methodHandlers.get(methodSignature);
        else
          methodHandler = interfaceClasses.stream()
              .map((Class<?> eachInterfaceClass) -> createMethodHandlerFromInterfaceClass(eachInterfaceClass, methodSignature))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .findFirst()
              .orElseGet(() -> createMethodHandlerFromFallbackObject(fallbackObject, methodSignature));
        return methodHandler.apply((SynthesizedObject) proxy, args);
      }
    }

    static MethodHandler createMethodHandlerFromFallbackObject(final Object fallbackObject, MethodSignature methodSignature) {
      return (synthesizedObject, args) -> {
        try {
          assert that(synthesizedObject, and(
              isNotNull(),
              transform(synthesizedObjectFallbackObject()).check(isSameReferenceAs(fallbackObject))));
          return fallbackObject.getClass().getMethod(methodSignature.name(), methodSignature.parameterClasses()).invoke(fallbackObject, args);
        } catch (NoSuchMethodException e) {
          throw new UnsupportedOperationException(e);
        } catch (InvocationTargetException |
                 IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      };
    }

    static Optional<MethodHandler> createMethodHandlerFromInterfaceClass(Class<?> fromClass, MethodSignature methodSignature) {
      return findMethodHandleFor(methodSignature, fromClass).map(MethodUtils::toMethodHandler);
    }

    static MethodHandler toMethodHandler(MethodHandle methodHandle) {
      return (SynthesizedObject synthesizedObject, Object[] arguments) -> {
        try {
          return methodHandle.bindTo(synthesizedObject).invokeWithArguments(asList(arguments));
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      };
    }

    static Optional<MethodHandle> findMethodHandleFor(MethodSignature methodSignature, Class<?> fromClass) {
      require(fromClass, allOf(isNotNull(), classIsInterface()));
      return findMethodMatchingWith(methodSignature, fromClass)
          .filter(Method::isDefault)
          .map(m -> methodHandleFor(m, fromClass));
    }

    private static MethodHandle methodHandleFor(Method m, Class<?> fromClass) {
      assert that(fromClass, transform(classGetMethod(m.getName(), m.getParameterTypes())).check(isNotNull()));
      try {
        return MethodHandles.lookup().in(fromClass).unreflectSpecial(m, fromClass);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
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

}
