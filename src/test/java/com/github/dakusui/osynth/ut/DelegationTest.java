package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Function;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class DelegationTest {
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
    TestInterface object = new ObjectSynthesizer()
        .addInterface(TestInterface.class)
        .handle(methodCall("testMethod0").delegatingTo(new TestInterface.Impl()))
        .handle(methodCall("testMethod1",String.class).with((sobj, args) -> "methodHandler:testMethod1:" + Arrays.toString(args)))
        .synthesize()
        .castTo(TestInterface.class);

    String out = object.testMethod0();

    System.out.println(out);

    assertThat(
        object,
        allOf(
            isNotNull(),
            transform(TestInterface::testMethod0).check(
                allOf(
                    isNotNull(),
                    equalTo("implementationObject:testMethod0:<>"))),
            transform((Function<TestInterface, String>) testInterface -> testInterface.testMethod1("string1")).check(
                allOf(
                    isNotNull(),
                    equalTo("methodHandler:testMethod1:<>")))
        ));
  }
}
