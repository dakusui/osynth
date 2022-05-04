package com.github.dakusui.osynth2;

import com.github.dakusui.osynth.Synthesized;
import com.github.dakusui.osynth2.annotations.BuiltInHandlerFactory;
import com.github.dakusui.osynth2.core.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static com.github.dakusui.osynth2.ObjectSynthesizer.InternalUtils.*;
import static com.github.dakusui.osynth.utils.AssertionUtils.*;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Postconditions.ensure;
import static com.github.dakusui.pcond.Preconditions.*;
import static com.github.dakusui.pcond.functions.Functions.*;
import static com.github.dakusui.pcond.functions.Predicates.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ObjectSynthesizer {
  interface Validator {
    Validator DEFAULT                = toNamedValidator("defaultValidator", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      require(
          descriptor,
          withMessage(() -> String.format("You tried to override reserved methods.: %n%s",
                  reservedMethodMisOverridings(descriptor.methodHandlers().keySet())
                      .stream()
                      .map(MethodSignature::toString)
                      .collect(joining("%n  ", "  ", ""))),
              transform(descriptorMethodHandlers()
                  .andThen(mapKeySet(parameter()))
                  .andThen(stream()))
                  .check(noneMatch(collectionContainsValue(SynthesizedObject.RESERVED_METHOD_SIGNATURES, parameter())))));
      return descriptor;
    });
    Validator ENFORCE_NO_DUPLICATION = toNamedValidator("noDuplicationEnforcingValidator", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      require(descriptor, transform(descriptorInterfaces().andThen(collectionDuplicatedElements())).check(isEmpty()));
      return descriptor;
    });

    Validator PASS_THROUGH = toNamedValidator("passThroughValidator", new Validator() {
      @Override
      public SynthesizedObject.Descriptor validate(ObjectSynthesizer objectSynthesizer, SynthesizedObject.Descriptor descriptor) {
        return descriptor;
      }
    });

    static Validator sequence(Validator... validators) {
      return toNamedValidator("validatorSequence:" + Arrays.toString(validators), (objectSynthesizer, descriptor) -> {
        SynthesizedObject.Descriptor ret = descriptor;
        for (Validator each : validators) {
          ret = requireNonNull(each).validate(objectSynthesizer, descriptor);
          ensure(ret, withMessage("Validation must not change the content of the descriptor.", allOf(
              transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
              transform(descriptorClassLoader()).check(isEqualTo(descriptor.classLoader())),
              transform(descriptorMethodHandlers()).check(isEqualTo(descriptor.methodHandlers())),
              transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject())))));
        }
        return ret;
      });
    }

    static Validator toNamedValidator(String name, Validator validator) {
      require(name, isNotNull());
      require(validator, isNotNull());
      return new Validator() {
        @Override
        public SynthesizedObject.Descriptor validate(ObjectSynthesizer objectSynthesizer, SynthesizedObject.Descriptor descriptor) {
          return validator.validate(objectSynthesizer, descriptor);
        }

        @Override
        public String toString() {
          return name;
        }
      };
    }

    SynthesizedObject.Descriptor validate(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);
  }


  interface Preprocessor {
    Preprocessor DEFAULT = toNamedPreprocessor("defaultPreprocessor", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      SynthesizedObject.Descriptor.Builder b = builder;
      createMethodHandlersForBuiltInMethods(descriptor, builder::addMethodHandler)
          .forEach(each -> b.addMethodHandler(each.signature(), each.handler()));
      if (descriptor.classLoader() == null)
        builder = builder.classLoader(SynthesizedObject.class.getClassLoader());
      if (!builder.interfaces().contains(SynthesizedObject.class))
        builder.addInterface(SynthesizedObject.class);
      return builder.build();
    });

    Preprocessor INCLUDE_INTERFACES_FROM_FALLBACK = toNamedPreprocessor("interfacesFromFallbackIncludingPreprocessor", (objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      Set<Class<?>> interfacesInOriginalDescriptor = new HashSet<>(descriptor.interfaces());
      List<Class<?>> interfacesNotInOriginalDescriptor = new LinkedList<>();
      Arrays.stream(descriptor.fallbackObject().getClass().getInterfaces())
          .filter(eachInterfaceInFallback -> !interfacesInOriginalDescriptor.contains(eachInterfaceInFallback))
          .forEach(builder::addInterface);
      return builder.build();
    });

    Preprocessor PASS_THROUGH = toNamedPreprocessor("passThrough", (objectSynthesizer, descriptor) -> descriptor);

    static Preprocessor sequence(Preprocessor... preprocessors) {
      return toNamedPreprocessor("preprocessorSequence:" + Arrays.toString(preprocessors), (objectSynthesizer, descriptor) -> {
        SynthesizedObject.Descriptor ret = descriptor;
        for (Preprocessor each : preprocessors) {
          ret = ensure(requireNonNull(each).preprocess(objectSynthesizer, descriptor), isNotNull());
        }
        return ret;
      });
    }

    static Preprocessor toNamedPreprocessor(String name, Preprocessor preprocessor) {
      require(name, isNotNull());
      require(preprocessor, isNotNull());
      return new Preprocessor() {
        @Override
        public SynthesizedObject.Descriptor preprocess(ObjectSynthesizer objectSynthesizer, SynthesizedObject.Descriptor descriptor) {
          return preprocessor.preprocess(objectSynthesizer, descriptor);
        }

        @Override
        public String toString() {
          return name;
        }

      };
    }

    SynthesizedObject.Descriptor preprocess(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);
  }

  private final SynthesizedObject.Descriptor.Builder descriptorBuilder;
  private       Validator                            validator;
  private       Preprocessor                         preprocessor;

  public ObjectSynthesizer() {
    this.descriptorBuilder = new SynthesizedObject.Descriptor.Builder();
    this.classLoader(this.getClass().getClassLoader())
        .validateWith(Validator.DEFAULT)
        .preprocessWith(Preprocessor.DEFAULT);
  }

  public ObjectSynthesizer addInterface(Class<?> interfaceClass) {
    descriptorBuilder.addInterface(interfaceClass);
    return this;
  }

  public ObjectSynthesizer classLoader(ClassLoader classLoader) {
    descriptorBuilder.classLoader(requireNonNull(classLoader));
    return this;
  }

  public ObjectSynthesizer fallbackObject(Object fallbackObject) {
    this.descriptorBuilder.fallbackObject(fallbackObject);
    return this;
  }

  public ObjectSynthesizer handle(MethodHandlerEntry handlerEntry) {
    requireNonNull(handlerEntry);
    this.descriptorBuilder.addMethodHandler(handlerEntry.signature(), handlerEntry.handler());
    return this;
  }

  public ObjectSynthesizer validateWith(Validator validator) {
    this.validator = requireNonNull(validator);
    return this;
  }

  public ObjectSynthesizer enableDuplicatedInterfaceCheck() {
    return this.validateWith(Validator.sequence(this.validator(), Validator.ENFORCE_NO_DUPLICATION));

  }

  public ObjectSynthesizer disableValidation() {
    return this.validateWith(Validator.PASS_THROUGH);
  }

  public ObjectSynthesizer preprocessWith(Preprocessor preprocessor) {
    this.preprocessor = requireNonNull(preprocessor);
    return this;
  }

  public ObjectSynthesizer includeInterfacesFrom() {
    return this.preprocessWith(Preprocessor.sequence(this.preprocessor(), Preprocessor.INCLUDE_INTERFACES_FROM_FALLBACK));
  }

  public ObjectSynthesizer disablePreprocessing() {
    return this.preprocessWith(Preprocessor.PASS_THROUGH);
  }

  public SynthesizedObject synthesize(Object fallbackObject) {
    return this.fallbackObject(fallbackObject).synthesize();
  }

  public SynthesizedObject synthesize() {
    return (SynthesizedObject) InternalUtils.createProxy(preprocessDescriptor(validateDescriptor(this.descriptorBuilder.build())));
  }

  public Preprocessor preprocessor() {
    return this.preprocessor;
  }

  public Validator validator() {
    return this.validator;
  }

  public static MethodHandlerEntry.Builder method(String methodName, Class<?>... parameterTypes) {
    return method(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder method(MethodSignature signature) {
    return new MethodHandlerEntry.Builder().signature(signature);
  }

  private SynthesizedObject.Descriptor validateDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.validator, isNotNull());
    SynthesizedObject.Descriptor ret = this.validator.validate(this, descriptor);
    ensure(ret, withMessage("Validation must not change the content of the descriptor.", allOf(
        transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
        transform(descriptorClassLoader()).check(isEqualTo(descriptor.classLoader())),
        transform(descriptorMethodHandlers()).check(isEqualTo(descriptor.methodHandlers())),
        transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject())))));
    return ret;
  }

  private SynthesizedObject.Descriptor preprocessDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.preprocessor, isNotNull());
    return ensure(this.preprocessor.preprocess(this, descriptor), isNotNull());
  }

  enum InternalUtils {
    ;

    static Object createProxy(SynthesizedObject.Descriptor descriptor) {
      return Proxy.newProxyInstance(
          descriptor.classLoader(),
          descriptor.interfaces().toArray(new Class[0]),
          new OsynthInvocationHandler(
              descriptor.methodHandlers(),
              descriptor.interfaces(),
              descriptor.fallbackObject()));
    }

    static List<MethodSignature> reservedMethodMisOverridings(Set<MethodSignature> methodSignatures) {
      return methodSignatures.stream()
          .filter(SynthesizedObject.RESERVED_METHOD_SIGNATURES::contains)
          .collect(toList());
    }

    static Stream<MethodHandlerEntry> createMethodHandlersForBuiltInMethods(SynthesizedObject.Descriptor descriptor, BiConsumer<MethodSignature, MethodHandler> updater) {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(BuiltInHandlerFactory.class))
          .map((Method eachMethod) -> MethodHandlerEntry.create(
              MethodSignature.create(eachMethod),
              createBuiltInMethodHandlerFor(eachMethod, descriptor)));
    }

    static MethodHandler createBuiltInMethodHandlerFor(Method method, SynthesizedObject.Descriptor descriptor) {
      assert that(method, and(
          isNotNull(),
          methodIsAnnotationPresent(BuiltInHandlerFactory.class)));
      BuiltInHandlerFactory annotation = method.getAnnotation(BuiltInHandlerFactory.class);
      try {
        return annotation.value().newInstance().create(descriptor);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
