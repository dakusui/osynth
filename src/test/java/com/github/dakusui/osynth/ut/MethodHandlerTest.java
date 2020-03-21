package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.core.MethodHandler;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import static com.github.dakusui.crest.Crest.*;

public class MethodHandlerTest {
  @Test
  public void testMethodHandler() {
    Object targetObject = new Object();
    MethodHandler mh1 = MethodHandler.toStringHandler(targetObject, s -> "helloWorld");
    MethodHandler mh2 = MethodHandler.toStringHandler(targetObject, s -> "Hi");
    Object obj = new Object();
    assertThat(
        mh1,
        allOf(
            asObject().equalTo(mh1).$(),
            asBoolean("equals", mh1).isTrue().$(),
            asInteger("hashCode").equalTo(mh1.hashCode()).$(),
            not(asInteger("hashCode").equalTo(mh2.hashCode()).$()),
            not(asObject().equalTo(mh2).$()),
            not(asObject().equalTo(obj).$())));

  }

  @Test
  public void testMatcher() {
    Object targetObject = new Object();
    MethodHandler mh1 = MethodHandler.toStringHandler(targetObject, s -> "helloWorld");
    MethodHandler mh1b = MethodHandler.toStringHandler(targetObject, s -> "helloWorld");
    MethodHandler mh2 = MethodHandler.hashCodeHandler(targetObject, Object::hashCode);
    Object obj = new Object();
    Predicate<Method> matcher = mh1.matcher();
    Predicate<Method> matcherB = mh1b.matcher();
    Predicate<Method> anotherMatcher = mh2.matcher();
    assertThat(
        matcher,
        allOf(
            allOf(
                asObject().equalTo(matcher).$(),
                asBoolean("equals", matcher).isTrue().$(),
                asInteger("hashCode").equalTo(matcher.hashCode()).$()),
            allOf(
                asObject().equalTo(matcherB).$(),
                asBoolean("equals", matcherB).isTrue().$(),
                asInteger("hashCode").equalTo(matcherB.hashCode()).$()),
            allOf(
                asBoolean("equals", anotherMatcher).isFalse().$(),
                not(asInteger("hashCode").equalTo(anotherMatcher.hashCode()).$()),
                not(asObject().equalTo(anotherMatcher).$())),
            not(asObject().equalTo(obj).$())));

  }

  @Test
  public void testMatcherParameter() {
    Predicate<Method> mh1 = MethodHandler.builderByNameAndParameterTypes("helloMethod", String.class).with((self, args) -> "hi").matcher();
    Predicate<Method> mh2 = MethodHandler.builderByNameAndParameterTypes("helloMethod", Integer.class).with((self, args) -> "hi").matcher();

    assertThat(
        mh1,
        allOf(
            asObject().equalTo(mh1).$(),
            not(asObject().equalTo(mh2).$()))
    );
  }
}
