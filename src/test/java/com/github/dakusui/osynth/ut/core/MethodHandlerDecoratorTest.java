package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import com.github.dakusui.pcond.forms.Predicates;
import org.junit.Test;

import static com.github.dakusui.osynth.core.MethodHandlerDecorator.filterOutPredefinedMethods;
import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;

public class MethodHandlerDecoratorTest {
  @Test
  public void examineEqualsMethodForFilteringOutPredefinedMethods() {
    MethodHandlerDecorator decorator = filterOutPredefinedMethods(new ObjectSynthesizer().methodHandlerDecorator());
    assertThat(decorator, objectIsEqualTo(decorator));
  }

  @Test
  public void examineEqualsMethodForFilteringOutPredefinedMethodsIfObjectIsNotDecorator() {
    MethodHandlerDecorator decorator = filterOutPredefinedMethods(new ObjectSynthesizer().methodHandlerDecorator());
    assertThat(decorator, objectIsEqualTo("").negate());
  }

  @Test
  public void examineHashCodeMethodForFilteringOutPredefinedMethods() {
    MethodHandlerDecorator decorator = filterOutPredefinedMethods(new ObjectSynthesizer().methodHandlerDecorator());
    assertThat(
        decorator,
        when()
            .applyFunction(objectHashCode())
            .applyFunction(objectToString())
            .applyFunction(integerParseInt())
            .then()
            .asInteger()
            .testPredicate(Predicates.alwaysTrue()) // Whatever the integer, it's okay
            .verify());
  }
}
