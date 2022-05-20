package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.SynthesizedObject;
import com.github.dakusui.pcond.forms.Printables;
import org.junit.Ignore;
import org.junit.Test;

import java.io.PrintStream;
import java.io.Serializable;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class SynthesizedObjectTest {
  @Test
  public void descriptorSameReference() {
    SynthesizedObject a = new ObjectSynthesizer().fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), allOf(
        isEqualTo(a.descriptor()),
        Printables.predicate("callEqualsDirectly", v -> v.equals(a.descriptor()))));
  }

  @Test
  public void nonDescriptorSameReference() {
    SynthesizedObject a = new ObjectSynthesizer().fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), not(isEqualTo("Hello")));
  }

  @Test
  public void differentHandlerDecorator() {
    SynthesizedObject a = new ObjectSynthesizer().fallbackTo("hello").synthesize();
    SynthesizedObject b = new ObjectSynthesizer().enableAutoLogging().fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), not(isEqualTo(b.descriptor())));
  }

  @Test
  public void differentMethodHandlerSet() {
    SynthesizedObject a = new ObjectSynthesizer().fallbackTo("hello").synthesize();
    SynthesizedObject b = new ObjectSynthesizer().handle(methodCall("hello").with((m, args) -> null)).fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), not(isEqualTo(b.descriptor())));
  }

  @Test
  public void equalMethodHandlerSet() {
    MethodHandler methodHandler = (m, args) -> null;
    SynthesizedObject a = new ObjectSynthesizer().handle(methodCall("hello").with(methodHandler)).fallbackTo("hello").synthesize();
    SynthesizedObject b = new ObjectSynthesizer(a.descriptor()).disableValidation().synthesize();

    assertThat(a.descriptor(), isEqualTo(b.descriptor()));
  }

  interface TestInterface {
  }

  @Ignore
  @Test
  public void differentInterfaces() {
    MethodHandler methodHandler = (m, args) -> null;
    SynthesizedObject a = new ObjectSynthesizer()
        .handle(methodCall("hello").with(methodHandler))
        .fallbackTo("hello")
        .synthesize();
    SynthesizedObject b = new ObjectSynthesizer(a.descriptor())
        .disableValidation()
        .addInterface(TestInterface.class)
        .synthesize();

    System.out.println("---- a ---- : " + System.identityHashCode(a));
    printDescriptor(System.out, a.descriptor());
    System.out.println("---- b ---- : " + System.identityHashCode(b));
    printDescriptor(System.out, b.descriptor());
    assertThat(a.descriptor(), not(isEqualTo(b.descriptor())));
  }

  @SuppressWarnings("SameParameterValue")
  private static void printDescriptor(PrintStream out, SynthesizedObject.Descriptor descriptor) {
    out.println("identityHashCode");
    out.println("  " + System.identityHashCode(descriptor));
    out.println("interfaces");
    descriptor.interfaces().forEach(i -> out.println("  " + i));
    out.println("methodHandlerEntries");
    descriptor.methodHandlerEntries().forEach(i -> out.println("  " + i));
    out.println("fallbackObject");
    out.println("  " + descriptor.fallbackObject());
  }

  @Ignore
  @Test
  public void differentHandlers() {
    MethodHandler methodHandler = (m, args) -> null;
    SynthesizedObject a = new ObjectSynthesizer()
        .handle(methodCall("hello").with(methodHandler))
        .fallbackTo("hello").synthesize();
    SynthesizedObject b = new ObjectSynthesizer(a.descriptor())
        .disableValidation()
        .handle(methodCall("hi").with((v, args) -> null))
        .addInterface(Serializable.class).synthesize();

    assertThat(a.descriptor(), not(isEqualTo(b.descriptor())));
  }
}
