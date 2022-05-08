package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodMatcher;
import com.github.dakusui.osynth.core.MethodSignature;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class MethodMatcherTest {
  public static class ToStringTest {
    @Test
    public void givenNameMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.nameMatchingExactly("helloMethod");

      System.out.println(mm);
    }

    @Test
    public void givenMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.matchingExactly(MethodSignature.create("helloMethod", int.class));

      System.out.println(mm);
    }
  }
}
