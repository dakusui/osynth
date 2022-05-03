package com.github.dakusui.osynth.sandbox;

import org.junit.Test;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Comparator;

import static com.github.dakusui.osynth.core.ProxyFactory.createMethodHandlesLookupFor;

@SuppressWarnings({ "SuspiciousInvocationHandlerImplementation", "Convert2Lambda", "RedundantThrows", "NewClassNamingConvention" })
public class Sandbox {
  @Test
  public void test() {
    Proxy p1 = createProxy((proxy, method, args) -> {
          //            if (method.getName().equals("toString"))
          //              return "proxy:" + this.toString();
          //            if (method.getName().equals("getClass"))
          //              return "getClass!";
          Class<?> klass = proxy.getClass();
          System.out.println("proxy.getClass():(" + klass + ")");
          //            System.out.println("proxy:(" + proxy.toString() + ")");
          System.out.println("proxy instanceof I1:" + (proxy instanceof I1));
          return null;
        },
        I1.class);
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

  /**
   * This test prints following.
   *
   * ----
   * method: public default void com.github.dakusui.osynth.sandbox.Sandbox$AInterface.method(java.lang.String)
   * method: public default void com.github.dakusui.osynth.sandbox.Sandbox$BInterface.method(java.lang.Object)
   * ----
   *
   * This means the one `proxy` object is implementing two interaces which has a method with the same name and parameter types
   * which will be complained of by a Java's compiler.
   */
  @Test
  public void testMultipleInheritanceWithProxy() {
    Proxy proxy = createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("method: " + method);
        return null;
      }
    }, AInterface.class, BInterface.class);
    AInterface aInterface = (AInterface) proxy;
    aInterface.method("Hello");
    BInterface bInterface = (BInterface) proxy;
    bInterface.method("Wow");
  }

  @Test
  public void testMultipleInheritanceWithProxy2() {
    Proxy proxy = createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("method: " + method);
        return null;
      }
    }, A2Interface.class, BInterface.class);
    AInterface aInterface = (AInterface) proxy;
    aInterface.method("Hello");
    BInterface bInterface = (BInterface) proxy;
    bInterface.method("Wow");
  }

  @Test
  public void testMethodHandlesLookup() throws NoSuchMethodException, IllegalAccessException {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(A2Interface.class);
    System.out.println("Lookup");
    System.out.println(lookup);
    System.out.println("Lookup.in(AInterface)");
    System.out.println(lookup.in(AInterface.class));
    System.out.println(lookup.in(AInterface.class).unreflect(AInterface.class.getMethod("method", String.class)));
    System.out.println("Lookup.in(A2Interface)");
    System.out.println(lookup.in(A2Interface.class));
    System.out.println(lookup.in(A2Interface.class).unreflect(AInterface.class.getMethod("method", String.class)));
    System.out.println(lookup.in(A2Interface.class).unreflect(A2Interface.class.getMethod("method", String.class)));
  }

  @Test
  public void testOverriddenMethod() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(RInterface.class);
    lookup.in(RInterface.class)
        .unreflect(PInterface.class.getMethod("aMethod")).invoke();
  }

  @Test
  public void testOverriddenMethod$verticalBehavior1() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(SInterface.class);
    System.out.println("in P get from P: " + lookup.in(PInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println("in Q get from P: " + lookup.in(QInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println("in R get from P: " + lookup.in(RInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
  }

  @Test
  public void testOverriddenMethod$verticalBehavior2() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(PInterface.class);
    System.out.println("in P get from P: " + lookup.in(PInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println("in P get from Q: " + lookup.in(PInterface.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println("in P get from R: " + lookup.in(PInterface.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println("in P get from S: " + lookup.in(PInterface.class).unreflect(SInterface.class.getMethod("aMethod")));
  }

  @Test
  public void testOverriddenMethod$verticalBehavior2p() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(PInterface.class);
    System.out.println("in P get from P: " + lookup.unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println("in P get from Q: " + lookup.unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println("in P get from R: " + lookup.unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println("in P get from S: " + lookup.unreflect(SInterface.class.getMethod("aMethod")));
  }

  @Test
  public void testOverriddenMethod$verticalBehavior2q() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(QInterface.class);
    System.out.println("in Q get from P: " + lookup.unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println("in Q get from Q: " + lookup.unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println("in Q get from R: " + lookup.unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println("in Q get from S: " + lookup.unreflect(SInterface.class.getMethod("aMethod")));
  }

  @Test
  public void testOverriddenMethod$verticalBehavior3() throws Throwable {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(RInterface.class);
    System.out.println("in P get from P: " + lookup.in(PInterface.class).unreflectSpecial(PInterface.class.getMethod("aMethod"), PInterface.class));
    System.out.println("in P get from Q: " + lookup.in(PInterface.class).unreflectSpecial(QInterface.class.getMethod("aMethod"), QInterface.class));
    System.out.println("in P get from R: " + lookup.in(PInterface.class).unreflectSpecial(RInterface.class.getMethod("aMethod"), RInterface.class));
    System.out.println("in P get from S: " + lookup.in(PInterface.class).unreflectSpecial(SInterface.class.getMethod("aMethod"), SInterface.class));
  }

  @Test
  public void playWithMethodHandle() throws NoSuchMethodException, IllegalAccessException {
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(RInterface.class);
    MethodHandle methodHandle = lookup.in(PInterface.class).unreflect(PInterface.class.getMethod("aMethod"));
    System.out.println("methodHandle.type(): " + methodHandle.type());
    System.out.println("methodHandle.asFixedArity(): " + methodHandle.asFixedArity());
    System.out.println("methodHandle.toString(): " + methodHandle.toString());
  }

  @Test
  public void testMultipleInheritanceWithProxy3() {
    Proxy proxy = createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("method: declaringClass: " + method.getDeclaringClass().getSimpleName() + " name: " + method.getName());
        return null;
      }
    }, PInterface.class, QInterface.class, RInterface.class);
    PInterface pInterface = (PInterface) proxy;
    pInterface.aMethod();
    QInterface qInterface = (QInterface) proxy;
    qInterface.aMethod();
    RInterface rInterface = (RInterface) proxy;
    rInterface.aMethod();
  }

  @Test
  public void testMultipleInheritanceWithProxy4() {
    Proxy proxy = createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("method: declaringClass: " + method.getDeclaringClass().getSimpleName() + " name: " + method.getName());
        return null;
      }
    }, RInterface.class, QInterface.class, PInterface.class);
    PInterface pInterface = (PInterface) proxy;
    pInterface.aMethod();
    QInterface qInterface = (QInterface) proxy;
    qInterface.aMethod();
    RInterface rInterface = (RInterface) proxy;
    rInterface.aMethod();
  }

  /**
   * This test prints the following output.
   *
   * ----
   * method: declaringClass: XInterface name: aMethod
   * method: declaringClass: XInterface name: aMethod
   * method: declaringClass: XInterface name: aMethod
   * method: declaringClass: ZInterface name: aMethod
   * method: declaringClass: ZInterface name: aMethod
   * method: declaringClass: ZInterface name: aMethod
   * ----
   *
   * This means that the dynamic proxy mechanism identifies the method to be invoked
   * by the name and parameters only<<1>>, but not b the interface class through which
   * the method call is requested on the caller side!
   *
   *
   * [[[1, Proxy]]] "java.lang.reflect.Proxy" https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Proxy.html
   */
  @SuppressWarnings("JavadocLinkAsPlainText")
  @Test
  public void testMultipleInheritanceWithProxy6() {
    {
      Proxy proxy = createProxy(new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          System.out.println("method: declaringClass: " + method.getDeclaringClass().getSimpleName() + " name: " + method.getName());
          return null;
        }
      }, XInterface.class, YInterface.class, ZInterface.class);
      XInterface xInterface = (XInterface) proxy;
      xInterface.aMethod();
      YInterface yInterface = (YInterface) proxy;
      yInterface.aMethod();
      ZInterface zInterface = (ZInterface) proxy;
      zInterface.aMethod();
    }

    {
      Proxy proxy = createProxy(new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
          System.out.println("method: declaringClass: " + method.getDeclaringClass().getSimpleName() + " name: " + method.getName());
          return null;
        }
      }, ZInterface.class, YInterface.class, XInterface.class);
      XInterface xInterface = (XInterface) proxy;
      xInterface.aMethod();
      YInterface yInterface = (YInterface) proxy;
      yInterface.aMethod();
      ZInterface zInterface = (ZInterface) proxy;
      zInterface.aMethod();
    }
  }

  @Test
  @CallerSensitive
  public void testWhatIsCallerClass() {
    System.out.println(new Object() {
      @Override
      @CallerSensitive
      public String toString() {
        return Reflection.getCallerClass().toString();
      }
    });
  }

  @Test
  public void whatIfGetLookUpThroughNormalMethod() throws NoSuchMethodException, IllegalAccessException {
    MethodHandles.Lookup lookup = MethodHandles.lookup();
    System.out.println("N");
    System.out.println(lookup.unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.unreflect(SInterface.class.getMethod("aMethod")));
    System.out.println("O");
    System.out.println(lookup.in(Object.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(Object.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(Object.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(Object.class).unreflect(SInterface.class.getMethod("aMethod")));
    System.out.println("P");
    System.out.println(lookup.in(PInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(PInterface.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(PInterface.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(PInterface.class).unreflect(SInterface.class.getMethod("aMethod")));
    System.out.println("Q");
    System.out.println(lookup.in(QInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(QInterface.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(QInterface.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(QInterface.class).unreflect(SInterface.class.getMethod("aMethod")));
    System.out.println("R");
    System.out.println(lookup.in(RInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(RInterface.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(RInterface.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(RInterface.class).unreflect(SInterface.class.getMethod("aMethod")));
    System.out.println("S");
    System.out.println(lookup.in(SInterface.class).unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(SInterface.class).unreflect(QInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(SInterface.class).unreflect(RInterface.class.getMethod("aMethod")));
    System.out.println(lookup.in(SInterface.class).unreflect(SInterface.class.getMethod("aMethod")));
  }

  @Test
  public void whatIfGetLookUpThroughNormalMethodAndGoForward() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(PInterface.class);
    System.out.println("N");
    System.out.println(lookup.unreflect(PInterface.class.getMethod("aMethod")));
    System.out.println(lookup.unreflectSpecial(PInterface.class.getMethod("aMethod"), PInterface.class).bindTo(createDummyProxy(PInterface.class)).invoke());
    System.out.println(lookup.unreflectSpecial(PInterface.class.getMethod("aMethod"), QInterface.class).bindTo(createDummyProxy(PInterface.class)).invoke());
    System.out.println(lookup.unreflectSpecial(PInterface.class.getMethod("aMethod"), RInterface.class).bindTo(createDummyProxy(PInterface.class)).invoke());
    System.out.println(lookup.unreflectSpecial(PInterface.class.getMethod("aMethod"), SInterface.class).bindTo(createDummyProxy(PInterface.class)).invoke());
  }

  @Test
  public void whatIfGetLookUpThroughNormalMethodAndGoForwardWithS() throws Throwable {
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(PInterface.class);
    System.out.println("N");
    System.out.println(lookup.unreflect(PInterface.class.getMethod("aMethod")));
    //    System.out.println(lookup.unreflectSpecial(SInterface.class.getMethod("aMethod"), PInterface.class).bindTo(createDummyProxy()).invoke());
    //    System.out.println(lookup.unreflectSpecial(SInterface.class.getMethod("aMethod"), QInterface.class).bindTo(createDummyProxy()).invoke());
    //    System.out.println(lookup.unreflectSpecial(SInterface.class.getMethod("aMethod"), RInterface.class).bindTo(createDummyProxy()).invoke());
    System.out.println(lookup.unreflectSpecial(SInterface.class.getMethod("aMethod"), SInterface.class).bindTo(createDummyProxy(PInterface.class)).invoke());
  }

  @Test
  public void whatIfGetLookUpThroughFunkyMethodAndGoForward() throws Throwable {

    Class<?> anInterfaceClass = RInterface.class;
    MethodHandles.Lookup lookup = createMethodHandlesLookupFor(anInterfaceClass);
    System.out.println("N");
    System.out.println(lookup.unreflect(anInterfaceClass.getMethod("aMethod")));
    System.out.println(lookup.unreflectSpecial(anInterfaceClass.getMethod("aMethod"), anInterfaceClass).bindTo(createDummyProxy(anInterfaceClass)).invoke());
  }

  @Test
  public void whatIfGetLookUpThroughSaneMethodAndGoForward() throws Throwable {

    Class<?> anInterfaceClass = QInterface.class;
    MethodHandles.Lookup lookup = MethodHandles.lookup().in(anInterfaceClass);
    System.out.println(lookup.unreflect(anInterfaceClass.getMethod("aMethod")));
    System.out.println(lookup.unreflectSpecial(anInterfaceClass.getMethod("aMethod"), anInterfaceClass).bindTo(createDummyProxy(anInterfaceClass)).invoke());
  }

  @Test
  public void whatIfGetLookUpThroughSaneMethodAndInvokeSpecial$() throws Throwable {
    for (Class<?> anInterfaceClass : new Class[] { PInterface.class, QInterface.class, RInterface.class, SInterface.class }) {
      System.out.println(invokeSpecialMethod(anInterfaceClass, createDummyProxy(anInterfaceClass), "aMethod"));
    }
  }

  @Test
  public void whatIfGetLookUpThroughSaneMethodAndInvokeNonSpecial$() throws Throwable {
    for (Class<?> anInterfaceClass : new Class[] { PInterface.class, QInterface.class, RInterface.class, SInterface.class }) {
      try {
        System.out.println(invokeMethod(anInterfaceClass, createDummyProxy(anInterfaceClass), "aMethod"));
      } catch (RuntimeException e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
      }
    }
  }

  @Test
  public void whatIfGetLookUpThroughSaneMethodAndInvokeNonSpecialWithSInterface() throws Throwable {
    for (Class<?> anInterfaceClass : new Class[] { PInterface.class, QInterface.class, RInterface.class, SInterface.class }) {
      try {
        System.out.println(invokeMethod(anInterfaceClass, createDummyProxy(anInterfaceClass), "aMethod"));
      } catch (RuntimeException e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private Proxy createDummyProxy(Class<?> anInterfaceClass) {
    return createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("I am dummy");
        return null;
      }
    }, anInterfaceClass);
  }

  @Test
  public void testProxyWithReflectionMethodInvocation() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Proxy proxy = createProxy(new InvocationHandler() {
      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("hello : " + method.getName());
        return "Hello: " + method.getName();
      }
    }, PInterface.class, QInterface.class, RInterface.class, SInterface.class);
    System.out.println(proxy.getClass().getMethod("aMethod").invoke(proxy));
  }

  @Test
  public void whatIsSuperclassOfSerializable() {
    System.out.println(Serializable.class.getSuperclass());
  }

  @Test
  public void whatIsSuperclassOfProxy() {
    System.out.println(createDummyProxy(Serializable.class).getClass().getSuperclass());
    System.out.println(createDummyProxy(Serializable.class).getClass().getSuperclass().getSuperclass());
  }

  @Test(expected = IllegalArgumentException.class)
  public void canIcreateProxyForNonInterfaceClass() {
    System.out.println(createDummyProxy(String.class));
  }

  static Proxy createProxy(InvocationHandler h, Class<?>... interfaces) {
    return (Proxy) Proxy.newProxyInstance(
        Sandbox.class.getClassLoader(),
        interfaces,
        h
    );
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

  @CallerSensitive
  public static void main(String... args) {
    System.out.println(new Object() {
      @Override
      @CallerSensitive
      public String toString() {
        return Reflection.getCallerClass().toString();
      }
    });
  }

  private static Object invokeSpecialMethod(Class<?> anInterfaceClass, Proxy proxy, String methodName) {
    try {
      Method method = anInterfaceClass.getMethod(methodName);
      String prefix = "--" + anInterfaceClass.getSimpleName() + ":";
      if (method.isDefault()) {
        return prefix +
            MethodHandles.lookup()
                .in(anInterfaceClass)
                .unreflectSpecial(method, anInterfaceClass)
                .bindTo(proxy)
                .invoke();
      }
      return prefix + "It was an abstract method";
    } catch (AbstractMethodError e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  private static Object invokeMethod(Class<?> anInterfaceClass, Proxy proxy, String methodName) {
    try {
      return MethodHandles.lookup()
          .in(anInterfaceClass)
          .unreflect(anInterfaceClass.getMethod(methodName))
          .bindTo(proxy)
          .invoke();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
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

  public interface A2Interface extends AInterface {
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

  public interface PInterface {
    void aMethod();
  }

  interface QInterface extends PInterface {
    default void aMethod() {
      System.out.println("A default implementation in QInterface");
    }
  }

  public interface RInterface extends QInterface {
    default void aMethod() {
      System.out.println("A default implementation in RInterface");
    }
  }

  public interface SInterface extends RInterface {
  }

  interface XInterface {
    void aMethod();
  }

  interface YInterface {
    default void aMethod() {
      System.out.println("A default implementation in QInterface");
    }
  }

  interface ZInterface {
    default void aMethod() {
      System.out.println("A default implementation in RInterface");
    }
  }
}