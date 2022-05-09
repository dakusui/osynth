package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodMatcher;
import com.github.dakusui.osynth.core.MethodSignature;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Functions.findString;
import static com.github.dakusui.pcond.forms.Predicates.*;

@RunWith(Enclosed.class)
public class MethodMatcherTest {
  public static class ToStringTest {
    @Test
    public void givenNameMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.nameMatchingExactly("helloMethod");

      System.out.println(mm);

      assertThat(mm.toString(), allOf(
          transform(
              findString("nameMatchingExactly").andThen(
                  findString("helloMethod"))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.matchingExactly(MethodSignature.create("helloMethod", int.class));

      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("matchingExactly").andThen(
                  findString("nameMatchingExactly").andThen(
                      findString("helloMethod")))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenNameMatchingLeniently() {
      MethodMatcher mm = ObjectSynthesizer.matchingLeniently(MethodSignature.create("helloMethod", int.class));

      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("matchingLeniently").andThen(
                  findString("nameMatchingRegex").andThen(
                      findString("helloMethod")))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenAnnotatedWith1() {
      MethodMatcher mm = ObjectSynthesizer.annotatedWith(Test.class);
      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("annotatedWith").andThen(
                  findString("Test"))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenAnnotatedWith2() {
      MethodMatcher mm = ObjectSynthesizer.annotatedWith(Test.class, ann -> ann.expected().equals(Throwable.class));
      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("annotatedWith").andThen(
                  findString("Test").andThen(
                      findString("satisfying:").andThen(
                          findString("lambda:").andThen(
                              findString("declared in").andThen(
                                  findString(MethodMatcher.class.getCanonicalName())
                              ))))))
              .check(isNotNull())
      ));
    }
  }
}
