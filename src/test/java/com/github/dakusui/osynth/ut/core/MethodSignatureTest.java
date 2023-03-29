package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.core.MethodSignature;
import org.junit.Test;

import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;

public class MethodSignatureTest {

  @Test
  public void equalMethodSignatures() {
    MethodSignature mm1 = MethodSignature.create("hello", Object.class);
    MethodSignature mm2 = MethodSignature.create("hello", Object.class);
    MethodSignature mm3 = MethodSignature.create("Hello", Object.class);

    assertThat(mm1, allOf(isEqualTo(mm2), not(isEqualTo(mm3))));
  }

  @Test
  public void differentNameMethodSignatures() {
    MethodSignature mm1 = MethodSignature.create("hello", String.class);
    MethodSignature mm2 = MethodSignature.create("Hello", String.class);

    assertThat(mm1, allOf(isEqualTo(mm2).negate()));
  }

  @Test
  public void overloadedMethodSignature() {
    MethodSignature mm1 = MethodSignature.create("hello", String.class);
    MethodSignature mm2 = MethodSignature.create("hello", Integer.class);

    assertThat(mm1, allOf(isEqualTo(mm2).negate()));
  }

  @Test
  public void sameReferencesEqual() {
    MethodSignature mm1 = MethodSignature.create("hello", Object.class);
    assertThat(mm1, m -> m.equals(mm1));
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  @Test
  public void nonMethodSignatureInstance() {
    MethodSignature mm1 = MethodSignature.create("hello", Object.class);
    assertThat(mm1, m -> !m.equals("")); // Intentional non MethodSignature instance.
  }
}
