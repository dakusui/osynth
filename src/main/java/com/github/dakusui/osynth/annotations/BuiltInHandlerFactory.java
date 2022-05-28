package com.github.dakusui.osynth.annotations;

import com.github.dakusui.osynth.core.*;

import java.lang.annotation.Retention;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.core.utils.AssertionUtils.methodIsAnnotationPresent;
import static com.github.dakusui.osynth.core.utils.MethodUtils.execute;
import static com.github.dakusui.osynth.core.utils.MethodUtils.withName;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.forms.Predicates.and;
import static com.github.dakusui.pcond.forms.Predicates.isNotNull;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.stream.Collectors.joining;

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
          .map((Method eachMethod) -> {
            MethodSignature targetMethodSignature = MethodSignature.create(eachMethod);
            return MethodHandlerEntry.create(
                MethodMatcher.create(mm -> String.format("builtInFor:%s%s", eachMethod.getName(), Arrays.toString(eachMethod.getParameterTypes())),
                    (Method candidate) -> targetMethodSignature.equals(MethodSignature.create(candidate))),
                createBuiltInMethodHandlerFor(eachMethod, descriptorSupplier), true);
          });
    }

  }

  class ForToString implements MethodHandlerFactory {
    @Override
    public MethodHandler.BuiltIn create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> composeFormattedString(synthesizedObject);
    }

    private static String composeFormattedString(SynthesizedObject synthesizedObject) {
      return String.format("osynth(%s):%s",
          synthesizedObject.descriptor().interfaces()
              .stream()
              .map(Class::getSimpleName)
              .collect(joining(",")),
          synthesizedObject.descriptor());
    }
  }

  class ForHashCode implements MethodHandlerFactory {
    @Override
    public MethodHandler.BuiltIn create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> synthesizedObject.descriptor().hashCode();
    }
  }

  class ForEquals implements MethodHandlerFactory {
    @Override
    public MethodHandler.BuiltIn create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> {
        if (synthesizedObject == objects[0])
          return true;
        if (!(objects[0] instanceof SynthesizedObject))
          return false;
        SynthesizedObject another = (SynthesizedObject) objects[0];
        return Objects.equals(synthesizedObject.descriptor(), another.descriptor());
      };
    }
  }

  class ForDescriptor implements MethodHandlerFactory {
    @Override
    public MethodHandler.BuiltIn create(Supplier<SynthesizedObject.Descriptor> descriptorSupplier) {
      return (synthesizedObject, objects) -> descriptorSupplier.get();
    }
  }
}

