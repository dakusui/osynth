package com.github.dakusui.osynth.compat.comb;

import com.github.dakusui.crest.core.ExecutionFailure;
import com.github.dakusui.jcunit.core.tuples.Tuple;
import com.github.dakusui.jcunit8.factorspace.Parameter;
import com.github.dakusui.jcunit8.pipeline.Requirement;
import com.github.dakusui.jcunit8.runners.junit4.JCUnit8;
import com.github.dakusui.jcunit8.runners.junit4.annotations.*;
import com.github.dakusui.osynth.compat.comb.def.I;
import com.github.dakusui.osynth.compat.comb.model.ExceptionType;
import com.github.dakusui.osynth.compat.comb.model.MethodType;
import com.github.dakusui.osynth.compat.comb.model.ObjectSynthesizerWrapper;
import com.github.dakusui.osynth.compat.comb.model.TargetMethodDef;
import com.github.dakusui.osynth.compat.testwrappers.LegacyObjectSynthesizer;
import com.github.dakusui.osynth.compat.testwrappers.SimpleObjectSynthesizer;
import com.github.dakusui.osynth.exceptions.OsynthException;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.osynth.ut.core.utils.UtUtils;
import org.junit.ComparisonFailure;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.List;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;
import static com.github.dakusui.valid8j.Assertions.precondition;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

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
@ConfigureWith(JCUnitBasedTest.ConfigFactory.class)
public class JCUnitBasedTest extends UtBase {
  public static class ConfigFactory extends com.github.dakusui.jcunit8.pipeline.stages.ConfigFactory.Base {
    @Override
    protected Requirement defineRequirement(Requirement.Builder defaultValues) {
      return defaultValues.addSeed(Tuple.builder()
          .put("auto", false)
          .put("numMethodHandlers", 1)
          .put("numInterfaces", 1)
          .put("numHandlerObjects", 1)
          .put("customFallback", false)
          .put("numArgs", 1)
          .put("methodType", MethodType.NORMAL)
          .put("exceptionType", ExceptionType.NONE)
          .build()).build();
    }
  }

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
    return Parameter.Simple.Factory.of(asList(1, 2, 0));
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
  public boolean whenMethodTypeIsNormalExceptionTypeIsAlsoNull(@From("methodType") MethodType methodType, @From("exceptionType") ExceptionType exceptionType) {
    if (methodType == MethodType.NORMAL)
      return exceptionType == ExceptionType.NONE;
    return exceptionType != ExceptionType.NONE;
  }

  @Condition(constraint = true)
  public boolean atLeastOneHandlerPresent(
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("numInterfaces") int numInterfaces,
      @From("customFallback") boolean customFallback) {
    return numMethodHandlers > 0 || numHandlerObjects > 0 || numInterfaces > 0 || customFallback;
  }

  @Condition
  public boolean secondIsProvided(
      @From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("numInterfaces") int numInterfaces) {
    return numInterfaces >= 2 || (auto && (numMethodHandlers >= 2 || numHandlerObjects >= 2));
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

  @Condition
  public boolean noInterface(@From("numInterfaces") int numInterfaces) {
    return numInterfaces == 0;
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

  @Given("normalReturningMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenTargetMethodIsRun(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThat(
        obj,
        asString(targetMethodDef.methodName(), targetMethodDef.args())
            .containsString(targetMethodDef.methodName()).$());
  }

  @Given("normalReturningMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenMethodWrittenBothIsRun(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects) throws Throwable {
    TargetMethodDef targetMethodDef = new TargetMethodDef(MethodType.NORMAL, 0, ExceptionType.NONE);
    I obj = (I) synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    System.out.println(obj.apply0_both());
    assertThat(
        obj,
        asString("apply0_both").containsString("apply0_both:I1:").$());
  }

  @Given("normalReturningMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenMethodWrittenInFirstIsRun(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(MethodType.NORMAL, 0, ExceptionType.NONE);
    I obj = (I) synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThat(
        obj,
        asString("apply0_1").containsString("apply0_1:I1:").$());
  }

  @Given("normalReturningMethod&&secondIsProvided")
  @Test
  public void whenSynthesized$thenMethodWrittenInSecondIsRun(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(MethodType.NORMAL, 0, ExceptionType.NONE);
    I obj = (I) synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThat(
        obj,
        asString("apply0_2").containsString("apply0_2:I2:").$());
  }

  @Given("normalReturningMethod&&!atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenNoHandlerReported(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThrows(IllegalArgumentException.class, () -> {
      try {
        System.out.println(((I) obj).apply0());
      } catch (IllegalArgumentException e) {
        assertThat(e.getMessage(), asString().containsString("No appropriate handler for the requested method").$());
        throw e;
      }
    });
  }

//  @Test
  @Ignore // A test methodology issue. What does method handlers (entries) are equal mean needs to be clarified first.
  public void testEquals(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType,
      @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    System.out.println("auto:             " + auto);
    System.out.println("numMethodHandlers:" + numMethodHandlers);
    System.out.println("numInterfaces:    " + numInterfaces);
    System.out.println("numHandlerObjects:" + numHandlerObjects);
    System.out.println("methodType:       " + methodType);
    System.out.println("numArgs:          " + numArgs);
    System.out.println("exceptionType:    " + exceptionType);
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj1 = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    Object obj2 = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    Object objX = new Object();
    System.out.println("RESULT:           " + obj1.equals(obj2));
    assertThat(
        obj1,
        allOf(
            allOf(
                asObject().equalTo(obj2).$(),
                asBoolean("equals", obj2).isTrue().$(),
                asInteger("hashCode").equalTo(obj2.hashCode()).$()),
            allOf(
                asObject().equalTo(obj1).$(),
                asBoolean("equals", obj1).isTrue().$(),
                asInteger("hashCode").equalTo(obj1.hashCode()).$()),
            not(asObject().equalTo(objX).$())));
  }

  @Given("runtimeExceptionThrowingMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenTargetMethodThrowsRuntimeException(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalRuntimeException.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (ExecutionFailure e) {
            throw UtUtils.rootCause(e);
          }
        }
    );
  }

  @Given("errorThrowingMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenTargetMethodThrowsError(@From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalError.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (ExecutionFailure e) {
            throw UtUtils.rootCause(e);
          }
        }
    );
  }

  @Given("checkedExceptionThrowingMethod&&atLeastOneHandlerPresent")
  @Test
  public void whenSynthesized$thenTargetMethodThrowsCheckedException(
      @From("auto") boolean auto,
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numInterfaces") int numInterfaces,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Object obj = synthesizeObject(auto, numMethodHandlers, numInterfaces, numHandlerObjects, targetMethodDef);
    assertThrows(
        ExceptionType.IntentionalCheckedException.class,
        () -> {
          try {
            assertThat(obj, asString(targetMethodDef.methodName(), targetMethodDef.args()).$());
          } catch (OsynthException e) {
            throw e;
          } catch (ExecutionFailure e) {
            e.printStackTrace();
            throw UtUtils.rootCause(e);
          }
        }
    );
  }

  @Given("normalReturningMethod&&atLeastOneHandlerPresent&&noInterface")
  @Test
  public void whenSynthesizedWithSimpleObjectSynthesizer$thenTargetMethodIsRun(
      @From("numMethodHandlers") int numMethodHandlers,
      @From("numHandlerObjects") int numHandlerObjects,
      @From("methodType") MethodType methodType, @From("numArgs") int numArgs,
      @From("exceptionType") ExceptionType exceptionType) {
    assert precondition(methodType, isNotNull());
    TargetMethodDef targetMethodDef = new TargetMethodDef(methodType, numArgs, exceptionType);
    Class<?>[] interfaces = targetMethodDef.getMethodType().interfaces(targetMethodDef.getExceptionType());
    Object obj = new ObjectSynthesizerWrapper(requireNonNull(SimpleObjectSynthesizer.create(interfaces[0])))
        .addMethodHandlers(targetMethodDef, numMethodHandlers)
        .addHandlerObjects(targetMethodDef, numHandlerObjects)
        .synthesize(Object.class);
    assertThat(
        obj,
        asString(targetMethodDef.methodName(), targetMethodDef.args())
            .containsString(targetMethodDef.methodName())
            .$()
    );
  }

  public static Object synthesizeObject(
      boolean auto,
      int numMethodHandlers,
      int numInterfaces,
      int numHandlerObjects,
      TargetMethodDef targetMethodDef) {
    LegacyObjectSynthesizer objectSynthesizer = LegacyObjectSynthesizer.create(auto);
    objectSynthesizer.addInterface(I.class);
    return new ObjectSynthesizerWrapper(objectSynthesizer)
        .addMethodHandlers(targetMethodDef, numMethodHandlers)
        .addHandlerObjects(targetMethodDef, numHandlerObjects)
        .addInterfaces(targetMethodDef, numInterfaces)
        .synthesize(Object.class);
  }

  private static void assertThrows(Class<? extends Throwable> exceptionClass, Executable executable) {
    try {
      executable.execute();
    } catch (Throwable t) {
      if (!exceptionClass.isInstance(t))
        throw new ComparisonFailure("Not an expected exceptioin type", exceptionClass.getName(), t.getClass().getName());
    }
  }

  /**
   * A method to workaround an incompatibility introduced in JUnit 4.13.1, where a test class run by a runner extending
   * {@link Parameterized} must have a static method annotated with {@link Parameterized.Parameters} and returning a list
   * even if the extending runner is responsible for providing the parameter automatically.
   *
   * @return An empty list.
   */
  @Parameterized.Parameters
  public static List<Object[]> aDummyParameterMethod() {
    return Collections.emptyList();
  }

  interface Executable {
    void execute() throws Throwable;
  }
}
