package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import org.junit.Test;

import java.util.Arrays;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.fluent.Fluents.objectValue;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;
import static com.github.dakusui.thincrest.TestFluents.assertStatement;
import static com.github.dakusui.thincrest_pcond.functions.Printable.function;

public class DelegationTest extends UtBase {
  interface TestInterface {
    String testMethod0();
    
    String testMethod1(String a);
    
    String testMethod2(String a, String b);
    
    class Impl implements TestInterface {
      @Override
      public String testMethod0() {
        return "implementationObject:testMethod0:<>";
      }
      
      @Override
      public String testMethod1(String a) {
        return "implementationObject:testMethod1:<" + a + ">";
      }
      
      @Override
      public String testMethod2(String a, String b) {
        return "implementationObject:testMethod2:<" + a + ":" + b + ">";
      }
    }
  }
  
  @Test
  public void whenDoDelegation$thenDelegatedObjectTakesControl() {
    TestInterface object = new ObjectSynthesizer()
        .addInterface(TestInterface.class)
        .handle(methodCall("testMethod0").delegatingTo(new TestInterface.Impl()))
        .synthesize()
        .castTo(TestInterface.class);
    
    String out = object.testMethod0();
    
    System.out.println(out);
    
    assertThat(
        out,
        allOf(
            isNotNull(),
            equalTo("implementationObject:testMethod0:<>")));
  }
  
  @Test
  public void given$whenDoDelegation$thenDelegatedObjectTakesControl() {
    TestInterface givenObject = new ObjectSynthesizer()
        .addInterface(TestInterface.class)
        .handle(methodCall("testMethod0").delegatingTo(new TestInterface.Impl()))
        .handle(methodCall("testMethod1", String.class).with((sobj, args) -> "methodHandler:testMethod1:" + Arrays.toString(args)))
        .synthesize()
        .castTo(TestInterface.class);
    
    System.out.println(givenObject);
    
    assertStatement(
        objectValue(givenObject)
            .transform(tx -> tx.asObject().then().isNotNull().done())
            .transform(tx -> tx.toObject(v -> v)
                .toObject(function("invoke[testMethod0]", TestInterface::testMethod0))
                .then()
                .isNotNull()
                .isEqualTo("implementationObject:testMethod0:<>")
                .done())
            .transform(tx -> tx.asObject().toObject(v -> (TestInterface) v)
                .toObject(function("invoke[testMethod1](arg1)", v -> v.testMethod1("testMethod1:arg1")))
                .then()
                .isNotNull()
                .isEqualTo("methodHandler:testMethod1:[testMethod1:arg1]")
                .done()));
  }
  
  @Test(expected = UnsupportedOperationException.class)
  public void given$whenDoDelegation$thenDelegatedObjectTakesControl2() {
    TestInterface object = new ObjectSynthesizer()
        .addInterface(TestInterface.class)
        .handle(methodCall("testMethod0").delegatingTo(new TestInterface.Impl()))
        .handle(methodCall("testMethod1", String.class).with((sobj, args) -> "methodHandler:testMethod1:" + Arrays.toString(args)))
        .synthesize()
        .castTo(TestInterface.class);
    
    try {
      System.out.println(object.testMethod2("testMethod2:arg1", "testMethod2:arg2"));
    } catch (UnsupportedOperationException e) {
      e.printStackTrace();
      throw e;
    }
  }
}
