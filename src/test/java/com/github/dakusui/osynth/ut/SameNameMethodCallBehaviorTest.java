package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;

public class SameNameMethodCallBehaviorTest {
  interface A {
    default void aMethod() {
      System.out.println("A#aMethod");
    }

    void aMethod2();
  }


  @Test
  public void test1() {
    A x = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod2").with((self, args) -> {
          ((A)self).aMethod();
          return null;
        }))
        .synthesize();
    x.aMethod();
  }

  @Test
  public void test2() {
    A x = new ObjectSynthesizer()
        .addInterface(A.class)
        .handle(methodCall("aMethod2").with((self, args) -> {
          System.out.println("2");
          ((A)self).aMethod();
          return null;
        }))
        .synthesize();
    x.aMethod2();
  }
}
