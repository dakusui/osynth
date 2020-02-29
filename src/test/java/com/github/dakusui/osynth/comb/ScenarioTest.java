package com.github.dakusui.osynth.comb;

import com.github.dakusui.crest.core.ExecutionFailure;
import com.github.dakusui.jcunit8.factorspace.Parameter;
import com.github.dakusui.jcunit8.runners.junit4.JCUnit8;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Condition;
import com.github.dakusui.jcunit8.runners.junit4.annotations.From;
import com.github.dakusui.jcunit8.runners.junit4.annotations.Given;
import com.github.dakusui.jcunit8.runners.junit4.annotations.ParameterSource;
import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.comb.model.ExceptionType;
import com.github.dakusui.osynth.comb.model.MethodType;
import com.github.dakusui.osynth.comb.model.ObjectSynthesizerWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.github.dakusui.crest.Crest.*;
import static java.util.Arrays.asList;

/**
 * I1,
 * I2,
 * H1,
 * H2,
 * F1
 * m(p1,p2)
 * <p>
 * I: 0, 1, 2    ... registered interfaces
 * H: 0, 1, 2    ... registered method handlers
 * F: 0, 1       ... registered fallback
 * Ma: 0, 1, 2   ... num args of the reacting method
 * Me: 0, 1      ... thrown exception or not
 */
@RunWith(JCUnit8.class)
public class ScenarioTest {

  @ParameterSource
  public Parameter.Factory<Boolean> auto() {
    return Parameter.Simple.Factory.of(asList(false, true));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numMethodHandlers() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numInterfaces() {
    return Parameter.Simple.Factory.of(asList(1, 2));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numHandlerObjects() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<Boolean> customFallback() {
    return Parameter.Simple.Factory.of(asList(true, false));
  }

  @ParameterSource
  public Parameter.Factory<Integer> numArgs() {
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
  }

  @ParameterSource
  public Parameter.Factory<MethodType> methodType() {
    return Parameter.Simple.Factory.of(asList(MethodType.NORMAL, MethodType.EXCEPTION));
  }

  @ParameterSource
  public Parameter.Factory<ExceptionType> exceptionType() {
    return Parameter.Simple.Factory.of(asList(ExceptionType.NONE, ExceptionType.CHECKED_EXCEPTION, ExceptionType.RUNTIME_EXCEPTION, ExceptionType.ERROR));
  }

  @Condition(constraint = true)
  public boolean ifMethodTypeIsNormalExceptionTypeMustBeNull(@From("methodType") MethodType methodType, @From("exceptionType") ExceptionType exceptionType) {
    if (methodType == MethodType.NORMAL)
      return exceptionType == ExceptionType.NONE;
    return true;
  }

  @Condition(constraint = true)
  public boolean atLeastOneHandlerPresent(
      @From("numMethodHandlers") Integer numMethodHandlers,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback
  ) {
    return (numMethodHandlers != null && numMethodHandlers > 0) || numHandlerObjects > 0 || customFallback;
  }

  @Condition
  public boolean normalReturningMethod(@From("methodType") MethodType methodType) {
    return methodType == MethodType.NORMAL;
  }

  @Condition
  public boolean runtimeExceptionThrowingMethod(@From("exceptionType") ExceptionType exceptionType) {
    return exceptionType == ExceptionType.RUNTIME_EXCEPTION;
  }

  @Condition
  public boolean errorThrowingMethod(@From("exceptionType") ExceptionType exceptionType) {
    return exceptionType == ExceptionType.ERROR;
  }

  @Condition
  public boolean checkedExceptionThrowingMethod(@From("exceptionType") ExceptionType exceptionType) {
    return exceptionType == ExceptionType.CHECKED_EXCEPTION;
  }

  @Test
  public void print(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    System.out.printf("auto:%s, numMethodHandlers:%s, numInterfaces=%s, numHandlerObjects=%s, customFallback=%s, methodType=%s, numArgs=%s, exceptionType=%s%n",
        auto,
        numMethodHandlers,
        numInterfaces,
        numHandlerObjects,
        customFallback,
        methodType,
        numArgs,
        exceptionType
    );
  }

  @Given("normalReturningMethod")
  @Test
  public void whenSynthesized$thenTargetMethodIsRun(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    assertThat(
        obj,
        asString(targetMethodDef.methodName(), targetMethodDef.args())
            .containsString(targetMethodDef.methodName())
            .$()
    );
  }

  @Test
  public void testEquals(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj1 = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    Object obj2 = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    assertThat(
        obj1,
        allOf(
            not(asObject().equalTo(obj2).$()),
            asObject().equalTo(obj1).$()));
  }

  @Given("runtimeExceptionThrowingMethod")
  @Test

  public void whenSynthesized$thenTargetMethodThrowsRuntimeException(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalRuntimeException.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (ExecutionFailure e) {
            throw getRootCause(e);
          }
        }
    );
  }

  @Given("errorThrowingMethod")
  @Test
  public void whenSynthesized$thenTargetMethodThrowsError(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalError.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (ExecutionFailure e) {
            throw getRootCause(e);
          }
        }
    );
  }

  @Given("checkedExceptionThrowingMethod")
  @Test
  public void whenSynthesized$thenTargetMethodThrowsCheckedException(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("customFallback") boolean customFallback,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, customFallback, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalCheckedException.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (ExecutionFailure e) {
            throw getRootCause(e);
          }
        }
    );
  }

  public static Throwable getRootCause(Throwable e) {
    Throwable cause = e.getCause();
    if (cause == null)
      return e;
    return getRootCause(cause);
  }

  public Object synthesizeObject(@From("auto") boolean auto, @From("numInterfaces") int numMethodHandlers, @From("numInterfaces") int numInterfaces, @From("numHandlerObjects") int numHandlerObjects, @From("customFallback") boolean customFallback, TargetMethodDef targetMethodDef) {
    ObjectSynthesizer objectSynthesizer = ObjectSynthesizer.create(auto);
    return new ObjectSynthesizerWrapper(objectSynthesizer)
        .addMethodHandlers(targetMethodDef, numMethodHandlers)
        .addHandlerObjects(targetMethodDef, numHandlerObjects)
        .addInterfaces(targetMethodDef, numInterfaces)
        .setFallbackHandlerFactory(targetMethodDef, customFallback)
        .synthesize();
  }
}
