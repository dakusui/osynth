package com.github.dakusui.osynth.compat.sandbox;

public class SandboxBuilder {
  static class BaseBuilder {
    @SuppressWarnings("unchecked")
    <B extends BaseBuilder> B method1() {
      return (B) this;
    }

    @SuppressWarnings("unchecked")
    <B extends BaseBuilder> B method2() {
      return (B) this;
    }

    String build() {
      return "hello";
    }
  }

  static class ABuilder extends BaseBuilder {
    ABuilder methodInSubclass() {
      return this;
    }
  }

  public void playWithExtendedBuilder() {
    ////
    // Doesn't compile
    //    System.out.println(new ABuilder().method1().methodInSubclass().build());
  }
}
