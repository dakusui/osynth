package com.github.dakusui.osynth2.annotations;

import com.github.dakusui.osynth2.core.*;

import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.dakusui.osynth2.core.utils.AssertionUtils.methodIsAnnotationPresent;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.execute;
import static com.github.dakusui.osynth2.core.utils.MethodUtils.withName;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.forms.Predicates.and;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface BuiltInHandlerFactory {
  Class<? extends MethodHandlerFactory> value();

  interface MethodHandlerFactory {
    MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier);

    static MethodHandler createBuiltInMethodHandlerFor(Method method, Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      assert that(method, and(
          isNotNull(),
          methodIsAnnotationPresent(BuiltInHandlerFactory.class)));
      BuiltInHandlerFactory annotation = method.getAnnotation(BuiltInHandlerFactory.class);
      return execute(() -> withName("builtIn-" + method.getName(), annotation.value().newInstance().create(descriptorSupplier)));
    }

    static Stream<MethodHandlerEntry> createMethodHandlersForBuiltInMethods(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(BuiltInHandlerFactory.class))
          .map((Method eachMethod) -> MethodHandlerEntry.create(
              MethodMatcher.MethodSignatureMatcher.create(MethodSignature.create(eachMethod), MethodMatcher.Factory.STRICT),
              createBuiltInMethodHandlerFor(eachMethod, descriptorSupplier)));
    }

  }

  class ForToString implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> composeFormattedString(synthesizedObject);
    }

    private static String composeFormattedString(SynthesizedObject synthesizedObject) {
      return String.format("osynth:%s", synthesizedObject.descriptor());
    }
  }

  class ForHashCode implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> synthesizedObject.descriptor().fallbackObject().hashCode();
    }
  }

  class ForEquals implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> synthesizedObject == objects[0];
    }
  }

  class ForDescriptor implements MethodHandlerFactory {
    @Override
    public MethodHandler create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> descriptorSupplier.get();
    }
  }
}

