package com.github.dakusui.osynth2.core;

/**
 * An interface that represents how a method request will be handled.
 *
 * The `osynth` library's framework calls the {@link MethodHandler#handle(SynthesizedObject, Object[])}
 * method from its own implementation of {@link java.lang.reflect.InvocationHandler}
 * interface.
 */
@FunctionalInterface
public interface MethodHandler {

  /**
   * A method to handle a method invocation request.
   *
   * Note that `osynth` guarantees the `args[]` is an empty array, even if the method doesn't
   * have any parameters, unlike the {@link java.lang.reflect.InvocationHandler}'s
   * {@code invoke} method.
   *
   * @param synthesizedObject A synthesized object on which the method was invoked.
   * @param args              Arguments passed to the target method.
   * @return The result for the handled method.
   * @throws Throwable An exception thrown during the handling.
   */
  Object handle(SynthesizedObject synthesizedObject, Object[] args) throws Throwable;
}
