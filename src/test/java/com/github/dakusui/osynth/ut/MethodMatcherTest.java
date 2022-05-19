package com.github.dakusui.osynth.ut;

import com.github.dakusui.osynth.ObjectSynthesizer;
import com.github.dakusui.osynth.core.*;
import com.github.dakusui.osynth.ut.core.utils.UtBase;
import com.github.dakusui.pcond.forms.Printables;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;

import static com.github.dakusui.pcond.Fluents.value;
import static com.github.dakusui.pcond.Fluents.when;
import static com.github.dakusui.pcond.TestAssertions.assertThat;
import static com.github.dakusui.pcond.forms.Functions.findString;
import static com.github.dakusui.pcond.forms.Predicates.*;

@RunWith(Enclosed.class)
public class MethodMatcherTest {
  public static class ToStringTest extends UtBase {
    @Test
    public void givenNameMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.nameMatchingExactly("helloMethod");

      System.out.println(mm);

      assertThat(mm.toString(), allOf(
          transform(
              findString("nameMatchingExactly").andThen(
                  findString("helloMethod"))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenMatchingExactly() {
      MethodMatcher mm = ObjectSynthesizer.matchingExactly(MethodSignature.create("helloMethod", int.class));

      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("matchingExactly").andThen(
                  findString("nameMatchingExactly").andThen(
                      findString("helloMethod")))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenNameMatchingLeniently() {
      MethodMatcher mm = ObjectSynthesizer.matchingLeniently(MethodSignature.create("helloMethod", int.class));

      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("matchingLeniently").andThen(
                  findString("nameMatchingRegex").andThen(
                      findString("helloMethod")))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenAnnotatedWith1() {
      MethodMatcher mm = ObjectSynthesizer.annotatedWith(Test.class);
      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("annotatedWith").andThen(
                  findString("Test"))
          ).check(isNotNull())
      ));
    }

    @Test
    public void givenAnnotatedWith2() {
      MethodMatcher mm = ObjectSynthesizer.annotatedWith(Test.class, ann -> ann.expected().equals(Throwable.class));
      System.out.println(mm);
      assertThat(mm.toString(), allOf(
          transform(
              findString("annotatedWith").andThen(
                  findString("Test").andThen(
                      findString("satisfying:").andThen(
                          findString("lambda:").andThen(
                              findString("declared in").andThen(
                                  findString(MethodMatcher.class.getCanonicalName())
                              ))))))
              .check(isNotNull())
      ));
    }
  }

  public static class CompositionTest {
    interface TestInterface {
      default String aMethod() {
        return "defaultMethod:aMethod";
      }

      default String bMethod() {
        return "defaultMethod:bMethod";
      }

      default String cMethod() {
        return "defaultMethod:cMethod";
      }
    }

    @Test
    public void examineOr() {
      TestInterface testObject = new ObjectSynthesizer()
          .addInterface(TestInterface.class)
          .handle(MethodHandlerEntry.create(
              ((MethodMatcher) m -> m.getName().equals("aMethod"))
                  .or(m -> m.getName().equals("bMethod")),
              (synthesizedObject, args) -> "handledMethodIsCalled"))
          .synthesize()
          .castTo(TestInterface.class);

      assertThat(
          testObject,
          allOf(
              when((TestInterface) value())
                  .applyFunction(Printables.function("aMethod", TestInterface::aMethod))
                  .thenAsString()
                  .isEqualTo("handledMethodIsCalled")
                  .verify(),
              when((TestInterface) value())
                  .applyFunction(Printables.function("bMethod", TestInterface::bMethod))
                  .thenAsString()
                  .isEqualTo("handledMethodIsCalled")
                  .verify(),
              when((TestInterface) value())
                  .applyFunction(Printables.function("cMethod", TestInterface::cMethod))
                  .thenAsString()
                  .isEqualTo("defaultMethod:cMethod")
                  .verify()
          )
      );
    }

    @Test
    public void examineNegateAndAnd() {
      TestInterface testObject = new ObjectSynthesizer()
          .addInterface(TestInterface.class)
          .handle(MethodHandlerEntry.create(
              ((MethodMatcher) m -> m.getName().equals("aMethod"))
                  .negate().and(m -> m.getName().endsWith("Method")),
              (synthesizedObject, args) -> "handledMethodIsCalled"))
          .synthesize()
          .castTo(TestInterface.class);

      assertThat(
          testObject,
          allOf(
              when((TestInterface) value())
                  .applyFunction(Printables.function("aMethod", TestInterface::aMethod))
                  .thenAsString()
                  .isEqualTo("defaultMethod:aMethod")
                  .verify(),
              when((TestInterface) value())
                  .applyFunction(Printables.function("bMethod", TestInterface::bMethod))
                  .thenAsString()
                  .isEqualTo("handledMethodIsCalled")
                  .verify(),
              when((TestInterface) value())
                  .applyFunction(Printables.function("cMethod", TestInterface::cMethod))
                  .thenAsString()
                  .isEqualTo("handledMethodIsCalled")
                  .verify()
          )
      );
    }
  }
}
