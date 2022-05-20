package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import com.github.dakusui.osynth.utils.TestForms;
import com.github.dakusui.pcond.forms.Predicates;
import org.junit.Test;

import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.thincrest_pcond.functions.Printable.function;

public class MethodHandlerDecoratorTest {
  @Test
  public void examineEqualsMethod() {
    MethodHandlerDecorator decorator = new ObjectSynthesizer().methodHandlerDecorator();
    assertThat(decorator, TestForms.objectIsEqualTo(decorator));
  }

  @Test
  public void examineHashCodeMethod() {
    MethodHandlerDecorator decorator = new ObjectSynthesizer().methodHandlerDecorator();
    assertThat(
        decorator,
        when()
            .applyFunction(function("objectHashCode", Object::hashCode))
            .applyFunction(function("objectToString", Object::toString))
            .applyFunction(function("integerParseInt", Integer::parseInt))
            .then()
            .asInteger()
            .testPredicate(Predicates.alwaysTrue())
        .verify());
  }
}
