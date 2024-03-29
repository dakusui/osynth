package com.github.dakusui.osynth.compat.sandbox;

import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodSignature;
import com.github.dakusui.osynth.core.utils.MethodUtils;
import com.github.dakusui.osynth.core.SynthesizedObject;
import org.junit.Test;

import java.util.Arrays;

import static com.github.dakusui.osynth.compat.sandbox.Sandbox.createProxy;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;

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
  public void tryFindMethodHandleFor() throws Throwable {
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

  private Object applyMethodHandler(SynthesizedObject synthesizedObject, MethodHandler methodHandler, Object... args) throws Throwable {
    return methodHandler.handle(synthesizedObject, args);
  }
}
