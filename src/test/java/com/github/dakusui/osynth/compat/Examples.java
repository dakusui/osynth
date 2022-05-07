package com.github.dakusui.osynth.compat;

import org.junit.Test;

import static com.github.dakusui.osynth.compat.ObjectSynthesizer.methodCall;

@SuppressWarnings("NewClassNamingConvention")
public class Examples {
  interface Hello {
    String hello(String name);
  }

  interface HelloWithDefault extends Hello {
    @Override
    default String hello(String name) {
      return "Hello, " + name + ".";
    }
  }

  interface Bye {
    String bye();
  }

  interface HelloWorld extends HelloWithDefault, Bye {
  }

  @Test
  public void example1() {
    HelloWorld helloWorld = new ObjectSynthesizer()
        .addInterface(HelloWorld.class)
        .handle(methodCall("bye").with((self, args) -> "Bye."))
        .synthesize()
        .castTo(HelloWorld.class);
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example2() {
    HelloWorld helloWorld = ObjectSynthesizer.create(false)
        .addInterface(HelloWorld.class)
        .fallbackTo((HelloWorld) () -> "Bye.")
        .synthesize()
        .castTo(HelloWorld.class);
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example3() {
    HelloWorld helloWorld = ObjectSynthesizer.create(true)
        .fallbackTo((HelloWorld) () -> "Bye.")
        .synthesize()
        .castTo(HelloWorld.class);
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example4() {
    HelloWorld helloWorld = ObjectSynthesizer.create(true)
        .fallbackTo((HelloWorld) () -> "Bye.")
        .synthesize()
        .castTo(HelloWorld.class);
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example5() {
    Hello hello = new ObjectSynthesizer()
        .addInterface(Hello.class)
        .addInterface(Bye.class)
        .fallbackTo((HelloWorld) () -> "Bye.")
        .synthesize()
        .castTo(HelloWorld.class);
    Bye bye = (Bye) hello;
    System.out.println(hello.hello("world"));
    System.out.println(bye.bye());
  }
}
