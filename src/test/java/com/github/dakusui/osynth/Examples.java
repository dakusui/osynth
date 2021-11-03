package com.github.dakusui.osynth;

import com.github.dakusui.osynth.compat.CompatObjectSynthesizer;
import org.junit.Test;

import static com.github.dakusui.osynth.compat.ObjectSynthesizer.methodCall;

@SuppressWarnings("deprecation")
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
    HelloWorld helloWorld = new CompatObjectSynthesizer()
        .addInterface(HelloWorld.class)
        .handle(methodCall("bye").with((self, args) -> "Bye."))
        .synthesize();
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example2() {
    HelloWorld helloWorld = CompatObjectSynthesizer.create(false)
        .addInterface(HelloWorld.class)
        .addHandlerObject((HelloWorld) () -> "Bye.")
        .synthesize();
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example3() {
    HelloWorld helloWorld = CompatObjectSynthesizer.create(true)
        .addHandlerObject((HelloWorld) () -> "Bye.")
        .synthesize();
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example4() {
    HelloWorld helloWorld = CompatObjectSynthesizer.create(true)
        .addHandlerObject((HelloWorld) () -> "Bye.")
        .synthesize();
    System.out.println(helloWorld.hello("world"));
    System.out.println(helloWorld.bye());
  }

  @Test
  public void example5() {
    Hello hello = new CompatObjectSynthesizer()
        .addInterface(Hello.class)
        .addInterface(Bye.class)
        .addHandlerObject((HelloWorld) () -> "Bye.")
        .synthesize();
    Bye bye = (Bye) hello;
    System.out.println(hello.hello("world"));
    System.out.println(bye.bye());
  }
}
