package com.github.dakusui.osynth;

import org.junit.Before;
import org.junit.Test;

import java.util.Objects;

import static com.github.dakusui.crest.Crest.*;
import static com.github.dakusui.osynth.SimpleObjectSynthesizer.methodCall;

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
        .addHandlerObject(handlerObject)
        .synthesize();
    assertThat(
        x,
        allOf(
            asString("aMethod").equalTo("a is called").$(),
            asString("bMethod").equalTo("b is called").$(),
            asString("toString").startsWith("com.github.dakusui.osynth.SimpleObjectSynthesizerTest$1@0").$(),
            asString("cMethod").equalTo("cMethod").$(),
            asString("xMethod").equalTo("xMethod").$(),
            asInteger(call("xMethod").andThen("toString").andThen("length").$()).equalTo(7).$()
        ));
  }

  @Test
  public void whenEqualsOnSameObject$thenTrue() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(handlerObject)
        .synthesize();
    assertThat(
        x,
        allOf(
            asBoolean("equals", x).isTrue().$(),
            asBoolean("equals", handlerObject).isTrue().$()
        ));
  }

  @Test
  public void whenEqualsOnAnotherObjectNotEqual$thenFalse() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(handlerObject)
        .synthesize();
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
        .addHandlerObject(handlerObject)
        .synthesize();
    assertThat(
        x,
        asBoolean("equals", createX("Hello")).isFalse().$()
    );
  }

  @Test
  public void whenEqualsOnAnotherProxiedObjectEqualToIt$thenTrue() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(handlerObject)
        .synthesize();
    X x2 = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(createX(""))
        .synthesize();
    assertThat(
        x,
        asBoolean("equals", x2).isTrue().$()
    );
  }

  @Test
  public void whenEqualsOnAnotherObjectEqualToIt$thenTrue() {
    X x = SimpleObjectSynthesizer.create(X.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(handlerObject)
        .synthesize();
    X x2 = createX("");
    assertThat(
        x,
        asBoolean("equals", x2).isTrue().$()
    );
  }

  @Test
  public void whenDefaultMethodCalled$thenValueReturned() {
    Y y = SimpleObjectSynthesizer.create(Y.class)
        .handle(methodCall("aMethod").with((self, args) -> "a is called"))
        .handle(methodCall("bMethod").with((self, args) -> "b is called"))
        .addHandlerObject(handlerObject)
        .synthesize();
    assertThat(
        y.yMethod(),
        asString().equalTo("yMethod").$()
    );
  }

  @Test
  public void thenPass() {
    Y y = createY();
    Y x1 = createProxyFor(Y.class, y);
    Y x2 = createProxyFor(Y.class, y);
    System.out.println(x1);
    System.out.println(x2);
    System.out.println(x1.equals(x2));
    assertThat(x1, asObject().equalTo(x2).$());
  }

  private static <T> T createProxyFor(Class<T> klass, T obj) {
    return SimpleObjectSynthesizer.create(klass).addHandlerObject(obj).synthesize(klass);
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
