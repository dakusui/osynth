package com.github.dakusui.osynth3;

import java.lang.reflect.Proxy;
import java.util.*;

import static com.github.dakusui.osynth3.ObjectSynthesizer.Utils.*;
import static com.github.dakusui.osynth.utils.AssertionUtils.*;
import static com.github.dakusui.pcond.Assertions.postcondition;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Postconditions.ensure;
import static com.github.dakusui.pcond.Preconditions.*;
import static com.github.dakusui.pcond.functions.Functions.*;
import static com.github.dakusui.pcond.functions.Predicates.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ObjectSynthesizer {
  interface Validator {
    Validator DEFAULT = (objectSynthesizer, descriptor) -> {
      assert that(descriptor, isNotNull());
      require(
          descriptor,
          withMessage(() -> String.format("You tried to override reserved methods.: %n%s",
                  reservedMethodMisOverridings(descriptor.methodHandlers.keySet())
                      .stream()
                      .map(MethodSignature::toString)
                      .collect(joining("%n  ", "  ", ""))),
              transform(descriptorMethodHandlers()
                  .andThen(mapKeySet(parameter()))
                  .andThen(stream()))
                  .check(noneMatch(collectionContainsValue(SynthesizedObject.RESERVED_METHOD_SIGNATURES, parameter())))));
      return descriptor;
    };

    SynthesizedObject.Descriptor validate(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);
  }

  interface Preprocessor {
    Preprocessor DEFAULT = (objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      SynthesizedObject.Descriptor.Builder b = builder;
      SynthesizedObject.PrivateUtils.createMethodHandlersForBuiltInMethods(descriptor, builder::addMethodHandler)
          .forEach(each -> b.addMethodHandler(each.signature(), each.handler()));
      if (descriptor.classLoader() == null)
        builder = builder.classLoader(SynthesizedObject.class.getClassLoader());
      if (!builder.interfaces.contains(SynthesizedObject.class))
        builder.addInterface(SynthesizedObject.class);
      return builder.build();
    };

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

  public ObjectSynthesizer preprocessWith(Preprocessor preprocessor) {
    this.preprocessor = requireNonNull(preprocessor);
    return this;
  }

  public SynthesizedObject synthesize(Object fallbackObject) {
    return this.fallbackObject(fallbackObject).synthesize();
  }

  public SynthesizedObject synthesize() {
    return (SynthesizedObject) Utils.createProxy(prepprocessDescriptor(validateDescriptor(this.descriptorBuilder.build())));
  }

  public static MethodHandlerEntry.Builder method(String methodName, Class<?>... parameterTypes) {
    return method(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder method(MethodSignature signature) {
    return new MethodHandlerEntry.Builder().signature(signature);
  }

  private SynthesizedObject.Descriptor prepprocessDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.preprocessor, isNotNull());
    return ensure(this.preprocessor.preprocess(this, descriptor), isNotNull());
  }

  private SynthesizedObject.Descriptor validateDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.validator, isNotNull());
    SynthesizedObject.Descriptor ret = this.validator.validate(this, descriptor);
    assert postcondition(ret, allOf(
        transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
        transform(descriptorClassLoader()).check(isEqualTo(descriptor.classLoader())),
        transform(descriptorMethodHandlers()).check(isEqualTo(descriptor.methodHandlers())),
        transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject()))
    ));
    return ret;
  }

  public enum Utils {
    ;

    static Object createProxy(SynthesizedObject.Descriptor descriptor) {
      return Proxy.newProxyInstance(
//descriptor.classLoader(),
          ObjectSynthesizer.class.getClassLoader(),
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
  }
}
