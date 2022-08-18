package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import com.github.dakusui.pcond.forms.Predicates;
import org.junit.Test;

import static com.github.dakusui.osynth.core.MethodHandlerDecorator.filterOutPredefinedMethods;
import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.fluent.Fluents.when;
import static com.github.dakusui.pcond.forms.Predicates.alwaysTrue;

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
        when().asObject()
            .exercise(objectHashCode())
            .exercise(objectToString())
            .exercise(integerParseInt())
            .then()
            .asInteger()
            .testPredicate(alwaysTrue())); // Whatever the integer, it's okay
  }
}
