package com.github.dakusui.osynth2.compat.sandbox;

public class SandboxBuilder {
  static class BaseBuilder {
    <B extends BaseBuilder> B method1() {
      return (B) this;
    }

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
