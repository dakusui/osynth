package com.github.dakusui.osynth.sandbox;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;

public class Sandbox {
  @Test
  public void test() {
    Proxy p1 = (Proxy) Proxy.newProxyInstance(
        Sandbox.class.getClassLoader(),
        new Class[] { I1.class },
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            //            if (method.getName().equals("toString"))
            //              return "proxy:" + this.toString();
            //            if (method.getName().equals("getClass"))
            //              return "getClass!";
            Class<?> klass = proxy.getClass();
            System.out.println("proxy.getClass():(" + klass + ")");
            //            System.out.println("proxy:(" + proxy.toString() + ")");
            System.out.println("proxy instanceof I1:" + (proxy instanceof I1));
            return null;
          }
        }
    );
    ((I1) p1).method1();
  }

  @Test
  public void test1() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .sorted(Comparator.comparing(Method::getName))
        .map(each -> each.getName() + ": " + each)
        .forEach(System.out::println);
  }

  @Test
  public void test2() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .filter(each -> !Modifier.isFinal(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test2b() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .filter(each -> !Modifier.isFinal(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test3() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .filter(each -> Modifier.isFinal(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test3a() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .filter(each -> Modifier.isFinal(each.getModifiers()))
        .filter(each -> !Modifier.isPrivate(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test4() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .filter(each -> Modifier.isNative(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test5() {
    Arrays.stream(Object.class.getDeclaredMethods())
        .sorted(Comparator.comparing(Method::getName))
        .filter(each -> Modifier.isNative(each.getModifiers()))
        .map(Method::getName)
        .forEach(System.out::println);
  }

  @Test
  public void test6() {
    System.out.println("+-------- public");
    System.out.println("|+------- protected");
    System.out.println("||+------ private");
    System.out.println("|||");
    System.out.println("|||  +--- final");
    System.out.println("|||  |+-- native");
    System.out.println("|||  ||+- static");
    System.out.println("|||  |||");
    Arrays.stream(Object.class.getDeclaredMethods())
        .sorted(Comparator.comparing(Method::getName))
        .map(each -> modifiers(each) + " " + each.getName() + " " + Arrays.toString(each.getParameterTypes()))
        .forEach(System.out::println);
  }

  @Test
  public void testMultipleInheritances() {
    System.out.println("C1 with string:");
    new C1Interface() {
    }.method("value");
    System.out.println("C1 with Object:");
    new C1Interface() {
    }.method(new Object());
    System.out.println("C2 with string:");
    new C2Interface() {
    }.method("value");
    System.out.println("C2 with Object:");
    new C2Interface() {
    }.method(new Object());
  }

  private static String modifiers(Method method) {
    return String.format("%s  %s%s%s",
        scopeOf(method),
        isFinal(method),
        isNative(method),
        isStatic(method));
  }

  private static String isFinal(Method method) {
    if (Modifier.isFinal(method.getModifiers()))
      return "X";
    return " ";
  }

  private static String isNative(Method method) {
    if (Modifier.isNative(method.getModifiers()))
      return "X";
    return " ";
  }

  private static String isStatic(Method method) {
    if (Modifier.isStatic(method.getModifiers()))
      return "X";
    return " ";
  }

  private static String scopeOf(Method method) {
    // public
    //  protected
    //   private
    // xxx
    if (Modifier.isPublic(method.getModifiers()))
      return "X  ";
    if (Modifier.isProtected(method.getModifiers()))
      return " X ";
    if (Modifier.isPrivate(method.getModifiers()))
      return "  X";
    return "   ";
  }

  public interface I1 {
    void method1();
  }

  public interface AInterface {
    default void method(String s) {
      System.out.println("  AInterface");
    }

    default void stringMethod(String s) {
      System.out.println("  AInterface");
    }
  }

  public interface BInterface {
    default void method(Object v) {
      System.out.println("  BInterface");
    }

    default void stringMethod(String s) {
      System.out.println("  BInterface");
    }
  }

  public interface C1Interface extends AInterface, BInterface {
    default void stringMethod(String s) {

    }
  }

  public interface C2Interface extends BInterface, AInterface {
    default void stringMethod(String s) {

    }
  }
}
