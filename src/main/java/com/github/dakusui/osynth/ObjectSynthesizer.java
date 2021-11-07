package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.MethodHandlerEntry;
import com.github.dakusui.osynth.core.SynthesizedObject;

import java.lang.reflect.Proxy;
import java.util.function.IntFunction;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.utils.InternalPredicates.isInterfaceClass;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.functions.Predicates.and;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;

public interface ObjectSynthesizer {
  static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandlerEntry.builderByNameAndParameterTypes(requireNonNull(methodName), requireNonNull(parameterTypes));
  }

  static ObjectSynthesizer create() {
    SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder();
    return () -> builder;
  }

  static ObjectSynthesizer from(Object fallbackObject) {
    return create().fallbackTo(fallbackObject);
  }

  static SynthesizedObject createSynthesizedObject(SynthesizedObject.Descriptor descriptor) {
    //SynthesizedObject synthesizedObject = new SynthesizedObject.Impl(descriptor);
    return new SynthesizedObject.Impl(Proxy.newProxyInstance(
        descriptor.classLoader(),
        Stream.concat(
                descriptor.registeredInterfaceClasses().stream(),
                Stream.of(SynthesizedObject.class))
            .distinct()
            .toArray((IntFunction<Class<?>[]>) Class[]::new),
        (proxy, method, args) -> ((SynthesizedObject) proxy).handleMethodInvocationRequest(method, args)),
        descriptor);
  }

  default ObjectSynthesizer addInterface(Class<?> interfaceClass) {
    require(interfaceClass, and(isNotNull(), isInterfaceClass()));
    descriptorBuilder().addInterfaceClass(interfaceClass);
    return this;
  }

  default ObjectSynthesizer handle(MethodHandlerEntry methodHandlerEntry) {
    requireNonNull(methodHandlerEntry);
    descriptorBuilder().addMethodHandler(methodHandlerEntry);
    return this;
  }

  default ObjectSynthesizer fallbackTo(Object fallbackObject) {
    descriptorBuilder().fallbackObject(fallbackObject);
    return this;
  }

  default SynthesizedObject synthesize() {
    return createSynthesizedObject(descriptorBuilder().build());
  }

  SynthesizedObject.Descriptor.Builder descriptorBuilder();
}
