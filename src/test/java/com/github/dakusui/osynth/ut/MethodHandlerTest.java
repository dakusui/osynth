package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.MethodHandler;
import org.junit.Test;

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
            not(asObject().equalTo(mh2).$()),
            not(asObject().equalTo(obj).$())));

  }

  @Test
  public void testMatcher() {
    Object targetObject = new Object();
    MethodHandler mh1 = MethodHandler.toStringHandler(targetObject, s -> "helloWorld");
    MethodHandler mh2 = MethodHandler.hashCodeHandler(targetObject, Object::hashCode);
    Object obj = new Object();
    assertThat(
        mh1.matcher(),
        allOf(
            asObject().equalTo(mh1.matcher()).$(),
            not(asObject().equalTo(mh2.matcher()).$()),
            not(asObject().equalTo(obj).$())));

  }
}
