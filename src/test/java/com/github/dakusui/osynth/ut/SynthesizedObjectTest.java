package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.SynthesizedObject;
import org.junit.Test;

import static com.github.dakusui.osynth.ObjectSynthesizer.methodCall;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Predicates.isEqualTo;
import static com.github.dakusui.pcond.forms.Predicates.not;

public class SynthesizedObjectTest {
  @Test
  public void descriptorSameReference() {
    SynthesizedObject a = new ObjectSynthesizer().fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), isEqualTo(a.descriptor()));
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
    SynthesizedObject a = new ObjectSynthesizer().handle(methodCall("hello").with((m, args) -> null)).fallbackTo("hello").synthesize();
    SynthesizedObject b = new ObjectSynthesizer().handle(methodCall("hello").with((m, args) -> null)).fallbackTo("hello").synthesize();

    assertThat(a.descriptor(), isEqualTo(b.descriptor()));
  }
}
