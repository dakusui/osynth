package com.github.dakusui.osynth.scenario;

import com.github.dakusui.crest.Crest;
import com.github.dakusui.osynth.MethodHandler;
import com.github.dakusui.osynth.ObjectSynthesizer;
import org.junit.Test;

import static com.github.dakusui.crest.Crest.asString;
import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;

/**
 * I1,
 * I2,
 * H1,
 * H2,
 * F1
 * m(p1,p2)
 * <p>
 * I: 0, 1, 2    ... registered interfaces
 * H: 0, 1, 2    ... registered method handlers
 * F: 0, 1       ... registered fallback
 * Ma: 0, 1, 2   ... num args of the reacting method
 * Me: 0, 1      ... thrown exception or not
 */
//@RunWith(JCUnit8.class)
public class ScenarioTest {
  interface I {
    default String apply0() {
      return String.format("apply0() on %s", implementorName());
    }

    default String apply1(int a0) {
      return String.format("apply1(%s) on %s", a0, implementorName());
    }

    default String apply2(int a0, int a1) {
      return String.format("apply2(%s,%s) on %s", a0, a1, implementorName());
    }

    String implementorName();
  }

  interface I1N extends I {
    @Override
    default String implementorName() {
      return "I1N";
    }
  }

  interface I2N extends I {
    @Override
    default String implementorName() {
      return "I2N";
    }
  }

  interface I1E extends I {
    @Override
    default String implementorName() {
      throw new RuntimeException("I1E");
    }
  }

  interface I2E extends I {
    @Override
    default String implementorName() {
      throw new RuntimeException("I2E");
    }
  }

  @Test
  public void example() {
    Object obj = new ObjectSynthesizer()
        .handle(createMethodHandler())
        .addHandlerObject(new Object())
        .addInterface(I1N.class)
        .addInterface(I2N.class)
        .fallbackHandlerFactory(ObjectSynthesizer.DEFAULT_FALLBACK_HANDLER_FACTORY)
        .synthesize();
    Crest.assertThat(
        obj,
        asString("apply1", 100).equalTo("apply1").$()
    );
  }

  @Test
  public void test2() {
    ObjectSynthesizer objectSynthesizer = new ObjectSynthesizer();
    Object obj = new ObjectSynthesizerWrapper(objectSynthesizer)
        .addInterfaces()
        .addHandlerObject()
        .addMethodHandlers()
        .setFallbackHandlerFactory()
        .synthesize();
    Crest.assertThat(
        obj,
        asString("apply1", 100).equalTo("apply1").$()
    );
  }

  public static MethodHandler createMethodHandler() {
    return methodCall("apply0").with((self, args) -> "apply0() on methodHandler");
  }

  static class ObjectSynthesizerWrapper {
    final ObjectSynthesizer objectSynthesizer;

    ObjectSynthesizerWrapper(ObjectSynthesizer objectSynthesizer) {
      this.objectSynthesizer = objectSynthesizer;
    }

    private ObjectSynthesizerWrapper addInterfaces() {
      return new ObjectSynthesizerWrapper(objectSynthesizer.addInterface(I1N.class).addInterface(I2N.class));
    }

    public ObjectSynthesizerWrapper addHandlerObject() {
      return new ObjectSynthesizerWrapper(objectSynthesizer.addHandlerObject(new Object()));
    }

    public ObjectSynthesizerWrapper setFallbackHandlerFactory() {
      return new ObjectSynthesizerWrapper(objectSynthesizer.fallbackHandlerFactory(ObjectSynthesizer.DEFAULT_FALLBACK_HANDLER_FACTORY));
    }

    public ObjectSynthesizerWrapper addMethodHandlers() {
      return new ObjectSynthesizerWrapper(objectSynthesizer.handle(createMethodHandler()));
    }

    public <T> T synthesize() {
      return this.objectSynthesizer.synthesize();
    }

  }
}
