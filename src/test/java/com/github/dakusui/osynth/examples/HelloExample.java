package com.github.dakusui.osynth.examples;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;

@SuppressWarnings("NewClassNamingConvention")
public class HelloExample {
  interface Hello {
    String hello();
  }

  @Test
  public void overrideHelloMethodAfterInstantiation() {
    Hello helloFallback = () -> "Hello!";
    Hello hello = new ObjectSynthesizer()
        .handle(methodCall("hello").with((SynthesizedObject sobj, Object[] args) -> "Hello, world"))
        .fallbackTo(helloFallback)
        .synthesize()
        .castTo(Hello.class);
    // This prints "Hello, world", not "Hello!".
    System.out.println(hello.hello());
  }
}
