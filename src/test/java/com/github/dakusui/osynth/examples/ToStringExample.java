package com.github.dakusui.osynth.examples;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import org.junit.Test;

import java.util.function.Supplier;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.Requires.require;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;

@SuppressWarnings("NewClassNamingConvention")
public
class ToStringExample {
  interface Hello {
    String hello();
  }

  @Test
  public void test() {
    class Impl implements Hello {
      @Override
      public String hello() {
        return "Hello!";
      }
    }
    Hello hello = ToStringExample.create(() -> "Hello, world", new Impl()).castTo(Hello.class);
    System.out.println(hello.hello());
    System.out.println(hello);
  }

  static SynthesizedObject create(Supplier<String> descriptionComposer, Object obj) {
    require(obj, isNotNull());
    require(descriptionComposer, isNotNull());
    return new ObjectSynthesizer()
        .handle(methodCall("toString").with((SynthesizedObject sobj, Object[] args) -> descriptionComposer.get()))
        .includeInterfacesFromFallbackObject()
        .fallbackTo(obj)
        .synthesize();
  }
}
