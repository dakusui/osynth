package com.github.dakusui.osynth2.compat.ut;

import com.github.dakusui.osynth2.compat.SimpleObjectSynthesizer;
import com.github.dakusui.osynth2.compat.utils.UtBase;
import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth2.compat.SimpleObjectSynthesizer.methodCall;

public class SimpleObjectSynthesizerTest extends UtBase {
  private X handlerObject;

  interface A {
    // ReflectivelyCalled
    @SuppressWarnings("unused")
    String aMethod();
  }

  interface B {
    // ReflectivelyCalled
    @SuppressWarnings("unused")
    String bMethod();
  }

  interface C {
    // ReflectivelyCalled
    @SuppressWarnings("unused")
    String cMethod();
  }

  interface X extends A, B, C {
    // ReflectivelyCalled
    @SuppressWarnings("unused")
    String xMethod();

  }

  public interface Y extends X {
    default String yMethod() {
      return "yMethod";
    }
  }

  @Before
  public void before() {
    super.before();
    this.handlerObject = createX("");
  }

  private X createX(String value) {
    return new X() {
      @Override
      public String xMethod() {
        return "xMethod" + value;
      }

      @Override
      public String cMethod() {
        return "cMethod";
      }

      @Override
      public String bMethod() {
        return "bMethod";
      }

      @Override
      public String aMethod() {
        return "aMethod";
      }

      @Override
      public int hashCode() {
        return value.hashCode();
      }

      @Override
      public boolean equals(Object anotherObject) {
        if (anotherObject instanceof X) {
          X another = (X) anotherObject;
          return Objects.equals(another.xMethod(), this.xMethod());
        }
        return false;
      }
    };
  }

  @Test
  public void whenMethodsCalled$thenProxiedToIntendedMethods() {
    X x = new SimpleObjectSynthesizer<>(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackObject(handlerObject)
        .synthesize()
        .castTo(X.class);
    assertThat(
        x,
        allOf(
            asString("aMethod").equalTo("a is called").$(),
            asString("bMethod").equalTo("b is called").$(),
            asString("toString").startsWith("proxy:osynth:").$(),
            asString("cMethod").equalTo("cMethod").$(),
            asString("xMethod").equalTo("xMethod").$(),
            asInteger(call("xMethod").andThen("toString").andThen("length").$()).equalTo(7).$()
        ));
  }

  @Test
  public void whenEqualsOnAnotherObjectNotEqual$thenFalse() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackObject(handlerObject)
        .synthesize()
        .castTo(X.class);
        assertThat(
        x,
        asBoolean("equals", "Hello").isFalse().$()
    );
  }

  @Test
  public void whenEqualsOnAnotherXNotEqual$thenFalse() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackObject(handlerObject)
        .synthesize()
        .castTo(X.class);
    assertThat(
        x,
        asBoolean("equals", createX("Hello")).isFalse().$()
    );
  }

  @Test
  public void whenDefaultMethodCalled$thenValueReturned() {
    Y y = SimpleObjectSynthesizer.create(Y.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .fallbackObject(handlerObject)
        .synthesize()
        .castTo(Y.class);
    assertThat(
        y.yMethod(),
        asString().equalTo("yMethod").$()
    );
  }

  @Test
  public void thenPass() {
    Y y = createY();
    Y y1 = createProxyFor(Y.class, y);
    Y y2 = createProxyFor(Y.class, y);
    System.out.println(y1);
    System.out.println(y2);
    System.out.println(y1.equals(y2));
    assertThat(y1, asObject().equalTo(y2).$());
  }

  private static <T> T createProxyFor(@SuppressWarnings("SameParameterValue") Class<T> klass, T obj) {
    return SimpleObjectSynthesizer.create(klass).fallbackObject(obj).synthesize(klass).castTo(klass);
  }

  private Y createY() {
    return new Y() {
      @Override
      public String xMethod() {
        return "xMethod";
      }

      @Override
      public String cMethod() {
        return "cMethod";
      }

      @Override
      public String bMethod() {
        return "bMethod";
      }

      @Override
      public String aMethod() {
        return "aMethod";
      }
    };
  }
}
