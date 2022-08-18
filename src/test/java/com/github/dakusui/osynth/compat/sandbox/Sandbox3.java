package com.github.dakusui.osynth.compat.sandbox;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static com.github.dakusui.osynth.compat.sandbox.Sandbox.createProxy;

public class Sandbox3 {
  interface A {
  }

  interface B {
  }

  @Test
  public void playWithProxySuperInterface() {
    Proxy proxy = createProxy(
        (proxy1, method, args) -> null,
        A.class, B.class
    );
    System.out.println(proxy.getClass().getSuperclass());
    Arrays.stream(proxy.getClass().getInterfaces())
        .forEach(System.out::println);
  }
}
