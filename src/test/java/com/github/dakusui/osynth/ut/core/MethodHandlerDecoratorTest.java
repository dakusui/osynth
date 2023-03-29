package com.github.dakusui.osynth.ut.core;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.MethodHandlerDecorator;
import org.junit.Test;

import static com.github.dakusui.osynth.core.MethodHandlerDecorator.filterOutPredefinedMethods;
import static com.github.dakusui.osynth.utils.TestForms.*;
import static com.github.dakusui.pcond.fluent.Fluents.objectValue;
import static com.github.dakusui.pcond.forms.Predicates.alwaysTrue;
import static com.github.dakusui.thincrest.TestAssertions.assertThat;
import static com.github.dakusui.thincrest.TestFluents.assertStatement;

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
    assertStatement(
        objectValue(decorator)
            .toObject(v -> (Object)v)
            .toObject(objectHashCode())
            .toObject(objectToString())
            .toObject(integerParseInt())
            .then()
            .checkWithPredicate(alwaysTrue())); // Whatever the integer, it's okay
  }
}
