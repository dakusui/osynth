package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.core.utils.MethodUtils;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class ComposeSimpleClassNameTest {

  public static void main(String... args) {
    {
      Predicate<Object> p = v -> true;
      Predicate<Object> q = new Predicate<Object>() {
        @Override
        public boolean test(Object o) {
          return false;
        }
      };
      Predicate<Object> r = new Supplier<Predicate<Object>>() {

        @Override
        public Predicate<Object> get() {
          return v -> v == p;
        }
      }.get();
      System.out.println("= lamda");
      System.out.println("custom:lambda:   " + MethodUtils.simpleClassNameOf(p.getClass()));
      System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
      System.out.println("= anonymous class");
      System.out.println("custom:anonymous:" + MethodUtils.simpleClassNameOf(q.getClass()));
      System.out.println("normal:anonymous:" + q.getClass().getSimpleName());
      System.out.println("= normal class");
      System.out.println("custom:static:   " + MethodUtils.simpleClassNameOf(ComposeSimpleClassNameTest.class));
      System.out.println("normal:static:   " + ComposeSimpleClassNameTest.class.getSimpleName());
      System.out.println("= lambda defined inside anonymous");
      System.out.println("custom:lambda:   " + MethodUtils.simpleClassNameOf(p.getClass()));
      System.out.println("normal:lambda:   " + p.getClass().getSimpleName());
      System.out.println("canonical:lambda:" + p.getClass().getCanonicalName());
    }
  }
}
