package com.github.dakusui.osynth.compat.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.MethodSignature;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import static com.github.dakusui.crest.Crest.function;
import static com.github.dakusui.osynth.ObjectSynthesizer.*;
import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.fluent.Fluents.objectValue;
import static com.github.dakusui.pcond.fluent.Fluents.value;
import static com.github.dakusui.pcond.forms.Predicates.isEqualTo;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;
import static com.github.dakusui.thincrest.TestFluents.assertStatement;
import static com.github.dakusui.thincrest_pcond.functions.Functions.cast;
import static com.github.dakusui.thincrest_pcond.functions.Predicates.startsWith;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class NewObjectSynthesizerTest extends UtBase {
  @Test
  public void givenDescriptorIsNotFinalized$whenQueried$thenFalse() {
    assertStatement(
        objectValue(new ObjectSynthesizer())
            .toObject(v -> (ObjectSynthesizer) v)
            .then()
            .checkWithPredicate(objectSynthesizerIsDescriptorFinalized().negate()));
  }
  
  @Test
  public void givenDescriptorIsFinalized$whenQueried$thenTrue() {
    ObjectSynthesizer osynth = new ObjectSynthesizer().fallbackTo(new Object());
    osynth.synthesize();
    assertStatement(
        objectValue(osynth).toObject(v -> (ObjectSynthesizer) v)
            .then()
            .checkWithPredicate(objectSynthesizerIsDescriptorFinalized()));
  }
  
  interface TestInterface {
    String helloMethod();
  }
  
  @Test
  public void test() {
    SynthesizedObject osynth = new ObjectSynthesizer()
        .handle(methodCall(nameMatchingRegex("helloM.*")).with((v, args) -> "helloWorldMethod"))
        .addInterface(TestInterface.class)
        .fallbackTo("HELLO_WORLD")
        .synthesize();
    
    assertStatement(
        objectValue(osynth)
            .toObject(o -> o)
            .toObject((SynthesizedObject s) -> s.castTo(TestInterface.class))
            .toObject(function("testInterfaceHelloMethod", TestInterface::helloMethod))
            .then()
            .isEqualTo("helloWorldMethod"));
  }
  
  interface TestInterface2 {
    
    String helloMethod0();
    
    String helloMethod1(Object var1);
    
    String helloMethod2(Object var1, Number number);
    
    String helloMethod4(String var1);
    
  }
  
  @Test
  public void givenSynthesizedObjectWithLenientMatchers$whenMethodWithMatchingHandlerInvoked$thenProperHandlerExecuted() {
    SynthesizedObject givenObject = synthesizeObject(
        "HELLO_WORLD",
        TestInterface2.class,
        builderForLenientHandlerEntry("helloMethod0").apply((v1, args) -> "helloWorldMethod0"),
        builderForLenientHandlerEntry("helloMethod1", String.class).apply((v1, args) -> "helloWorldMethod1:" + args[0]),
        builderForLenientHandlerEntry("helloMethod2", String.class, Integer.class).apply((v1, args) -> "helloWorldMethod2:" + args[0] + ":" + args[1]));
    
    assertStatement(
        objectValue(givenObject)
            .transform(tx -> tx.asObject().toObject(v -> (TestInterface2)v)
                .toObject(TestInterface2::helloMethod0)
                .then()
                .isEqualTo("helloWorldMethod0").done())
            .transform(tx -> tx.asObject().toObject(v -> (TestInterface2)v)
                .toObject(v -> v.helloMethod1("HELLO"))
                .then()
                .isEqualTo("helloWorldMethod1:HELLO").done())
            .transform(tx -> tx.asObject().toObject(v -> (TestInterface2)v)
                .toObject(v -> v.helloMethod2("HELLO", 1))
                .then()
                .isEqualTo("helloWorldMethod2:HELLO:1").done()));
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void givenSynthesizedObjectWithLenientMatchers$whenMethodWithNoMatchingHandlerInvoked$thenUnsupportedException() {
    SynthesizedObject givenObject = synthesizeObject(
        "HELLO_WORLD",
        TestInterface2.class,
        builderForLenientHandlerEntry("helloMethod4").apply((v, args) -> {
          throw new AssertionError("SHOULD NOT MATCH BECAUSE OF PARAMETER NUMBER:helloWorldMethod4()");
        }),
        builderForLenientHandlerEntry("helloMethod4", Integer.class).apply((v, args) -> {
          throw new AssertionError("SHOULD NOT MATCH BECAUSE OF PARAMETER TYPE:helloWorldMethod4(Integer)");
        }));
    
    try {
      String v = givenObject.castTo(TestInterface2.class).helloMethod4("hello!");
      System.out.println(v);
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      throw e;
    }
  }
  
  static Function<MethodHandler, MethodHandlerEntry> builderForLenientHandlerEntry(String methodName, Class<?>... parameterTypes) {
    return methodHandler -> MethodHandlerEntry.create(matchingLeniently(MethodSignature.create(methodName, parameterTypes)), methodHandler, false);
  }
  
  private <T> SynthesizedObject synthesizeObject(Object fallbackObject, Class<T> interfaceClass, MethodHandlerEntry... handlerEntries) {
    return new ObjectSynthesizer() {{
      for (MethodHandlerEntry eachEntry : handlerEntries)
        this.handle(eachEntry);
    }}
        .addInterface(interfaceClass)
        .fallbackTo(fallbackObject)
        .synthesize();
  }
  
  
  @Retention(RUNTIME)
  @interface TestAnnotation {
    String value() default "world";
  }
  
  interface TestInterface3 {
    @TestAnnotation
    String method1(String var);
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testAnnotatedMethod() {
    MethodHandler methodHandlingFunction = (o, args) -> "annotatedWith:" + TestAnnotation.class.getSimpleName() + ":" + Arrays.toString(args);
    SynthesizedObject so = new ObjectSynthesizer()
        .addInterface(TestInterface3.class)
        .handle(methodCall(annotatedWith(TestAnnotation.class)).with(methodHandlingFunction))
        .synthesize();
    assertStatement(objectValue(so)
        .asObject()
        .toObject((Function<Object, SynthesizedObject>) cast(SynthesizedObject.class))
        .toObject(synthesizedObjectCastTo(TestInterface3.class))
        .toObject(v -> v.method1("Hello"))
        .then()
        .isEqualTo("annotatedWith:TestAnnotation:[Hello]"));
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void testAnnotatedMethod2() {
    SynthesizedObject so = synthesizeObjectForInterface(TestInterface3.class);
    try {
      String message = so.castTo(TestInterface3.class).method1("Hello");
      System.out.println(message);
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      assertStatement(objectValue(e)
          .toString(throwableGetMessage())
          .then()
          .findSubstrings(
              "An appropriate method handler/implementation for 'method1(String)' was not found",
              "osynth",
              "TestInterface3",
              "SynthesizedObject"));
      throw e;
    }
  }
  
  interface TestInterface4 {
    @TestAnnotation(value = "WORLD")
    String method1(String var);
  }
  
  @Test
  public void testAnnotatedMethod3() {
    SynthesizedObject givenObject = synthesizeObjectForInterface(TestInterface4.class);
    
    assertStatement(
        objectValue(givenObject)
            .toObject(v -> (SynthesizedObject) v)
            .toObject(synthesizedObjectCastTo(TestInterface4.class))
            .toObject(v -> v.method1("Hello"))
            .then()
            .isEqualTo("annotatedWith:TestAnnotation:[Hello]"));
  }
  
  private static <T> SynthesizedObject synthesizeObjectForInterface(Class<T> interfaceClass) {
    MethodHandler methodHandlingFunction = (o, args) -> "annotatedWith:" + TestAnnotation.class.getSimpleName() + ":" + Arrays.toString(args);
    return new ObjectSynthesizer()
        .addInterface(interfaceClass)
        .handle(methodCall(annotatedWith(TestAnnotation.class, annotation -> Objects.equals(annotation.value(), "WORLD"))).with(methodHandlingFunction))
        .synthesize();
  }
  
  @Test
  public void notGivenAnyObjectForFallback$whenSynthesizeAndCallMethod$thenDoesntBreak() {
    SynthesizedObject so = new ObjectSynthesizer().synthesize();
    System.out.println(so.toString());
    assertThat(so.toString(), startsWith("osynth(SynthesizedObject"));
  }
  
  
  @Test
  public void givenSynthesizedFromAnotherSynthesized$whenCallMethod$thenLetsSee() {
    SynthesizedObject so = ObjectSynthesizer.from(
        new ObjectSynthesizer()
            .addInterface(TestInterface4.class)
            .fallbackTo((TestInterface4) var -> "overriddenMethod1(String=" + var + ")")
            .synthesize()
            .descriptor()).synthesize();
    
    assertThat(
        so.castTo(TestInterface4.class).method1("hello"),
        isEqualTo("overriddenMethod1(String=hello)"));
  }
  
  @Test
  public void equalness$differentInterfaces() {
    SynthesizedObject so1 = new ObjectSynthesizer().addInterface(TestInterface4.class).synthesize();
    SynthesizedObject so2 = new ObjectSynthesizer().synthesize();
    
    assertThat(so1.descriptor(), isEqualTo(so2.descriptor()).negate());
  }
  
  @Test
  public void equalness$differentMethodHandlers() {
    SynthesizedObject so1 = new ObjectSynthesizer().handle(methodCall("m").with((obj, args) -> "world")).synthesize();
    SynthesizedObject so2 = new ObjectSynthesizer().handle(methodCall("n").with((obj, args) -> "hello")).synthesize();
    
    assertThat(so1.descriptor(), isEqualTo(so2.descriptor()).negate());
  }
  
  @Test
  public void equalness$differentSameMethodHandlers() {
    MethodHandlerEntry m = methodCall("m").with((obj, args) -> "world");
    SynthesizedObject so1 = new ObjectSynthesizer().handle(m).synthesize();
    SynthesizedObject so2 = new ObjectSynthesizer().handle(m).synthesize();
    
    assertThat(so1.descriptor(), isEqualTo(so2.descriptor()));
  }
}
