package com.github.dakusui.osynth.neo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.neo.ObjectSynthesizer.Utils.*;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.core.refl.MethodQuery.instanceMethod;
import static com.github.dakusui.pcond.functions.Functions.*;
import static com.github.dakusui.pcond.functions.Predicates.*;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ObjectSynthesizer {
  private final SynthesizedObject.Descriptor.Builder descriptorBuilder;

  public ObjectSynthesizer() {
    this.descriptorBuilder = new SynthesizedObject.Descriptor.Builder();
    this.classLoader(this.getClass().getClassLoader());
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

  public SynthesizedObject synthesize(Object fallbackObject) {
    return this.fallbackObject(fallbackObject).synthesize();
  }

  public SynthesizedObject synthesize() {
    return (SynthesizedObject) Utils.createProxy(updateDescriptor(validateDescriptor(this.descriptorBuilder.build())));
  }

  protected SynthesizedObject.Descriptor updateDescriptor(SynthesizedObject.Descriptor descriptor) {
    SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
    SynthesizedObject.Descriptor.Builder b = builder;
    createMethodHandlersFor(SynthesizedObject.class, descriptor, builder::addMethodHandler)
        .forEach(each -> b.addMethodHandler(each.signature(), each.handler()));
    if (descriptor.classLoader() == null)
      builder = builder.classLoader(SynthesizedObject.class.getClassLoader());
    return builder.build();
  }

  protected SynthesizedObject.Descriptor validateDescriptor(SynthesizedObject.Descriptor descriptor) {
    assert that(descriptor, isNotNull());
    require(
        descriptor,
        withMessage(() -> format("You tried to override reserved methods.: %n%s",
                reservedMethodMisOverridings(descriptor.methodHandlers.keySet())
                    .stream()
                    .map(MethodSignature::toString)
                    .collect(joining("%n  ", "  ", ""))),
            transform(descriptorMethodHandlers()
                .andThen(mapKeySet(parameter()))
                .andThen(stream()))
                .check(noneMatch(collectionContainsValue(SynthesizedObject.RESERVED_METHOD_SIGNATURES, parameter())))));
    return descriptor;
  }

  public enum Utils {
    ;

    static Object createProxy(SynthesizedObject.Descriptor descriptor) {
      return Proxy.newProxyInstance(descriptor.classLoader(), descriptor.interfaces.toArray(new Class[0]), createInvocationHandler(descriptor));
    }

    static InvocationHandler createInvocationHandler(SynthesizedObject.Descriptor descriptor) {
      return (proxy, method, args) -> {
        assert that(proxy, isInstanceOf(SynthesizedObject.class));
        SynthesizedObject synthesizedObject = (SynthesizedObject) proxy;
        return findMethodHandlerFor(MethodSignature.create(method), descriptor)
            .map(h -> h.apply(synthesizedObject, args))
            .orElseGet(() -> invokeMethod(synthesizedObject.fallbackObject(), method, args));
      };
    }

    private static Object invokeMethod(Object object, Method method, Object[] args) {
      try {
        return method.invoke(object, args);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    static Optional<MethodHandler> findMethodHandlerFor(MethodSignature methodSignature, SynthesizedObject.Descriptor descriptor) {
      return Optional.ofNullable(descriptor.methodHandlers.get(methodSignature));
    }

    static Stream<MethodHandlerEntry> createMethodHandlersFor(Class<?> targetClass, SynthesizedObject.Descriptor descriptor, BiConsumer<MethodSignature, MethodHandler> updater) {
      return Arrays.stream(targetClass.getMethods())
          .filter(each -> each.isAnnotationPresent(BuiltInHandlerFactory.class))
          .map(each -> MethodHandlerEntry.create(MethodSignature.create(each), createMethodHandlerFor(each, descriptor)));
    }

    private static MethodHandler createMethodHandlerFor(Method method, SynthesizedObject.Descriptor descriptor) {
      assert that(method, and(
          isNotNull(),
          callp(instanceMethod(parameter(), "getAnnotation", BuiltInHandlerFactory.class))));
      BuiltInHandlerFactory annotation = method.getAnnotation(BuiltInHandlerFactory.class);
      try {
        return annotation.value().newInstance().create(descriptor);
      } catch (InstantiationException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    static Function<Object, Map<MethodSignature, MethodHandler>> descriptorMethodHandlers() {
      return call(instanceMethod(parameter(), "methodHandlers"));
    }

    static Function<Object, Collection<?>> mapKeySet(Object value) {
      return call(instanceMethod(value, "keySet"));
    }

    static Predicate<Object> collectionContainsValue(Collection<?> targetSet, Object value) {
      return callp(instanceMethod(targetSet, "contains", value));
    }

    static List<MethodSignature> reservedMethodMisOverridings(Set<MethodSignature> methodSignatures) {
      return methodSignatures.stream()
          .filter(SynthesizedObject.RESERVED_METHOD_SIGNATURES::contains)
          .collect(toList());
    }
  }
}
