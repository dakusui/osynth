package com.github.dakusui.osynth.neo;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static com.github.dakusui.pcond.core.refl.MethodQuery.instanceMethod;
import static com.github.dakusui.pcond.functions.Functions.parameter;
import static com.github.dakusui.pcond.functions.Predicates.*;
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
    return (SynthesizedObject) Utils.createProxy(update(this.descriptorBuilder.build()));
  }

  protected SynthesizedObject.Descriptor update(SynthesizedObject.Descriptor descriptor) {
    return validate(descriptor);
  }

  protected SynthesizedObject.Descriptor validate(SynthesizedObject.Descriptor descriptor) {
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

    private static List<MethodHandler> createMethodHandlersFor(Class<?> targetClass, SynthesizedObject.Descriptor descriptor) {
      return Arrays.stream(targetClass.getMethods())
          .filter(each -> each.isAnnotationPresent(BuiltInHandlerFactory.class))
          .map(each -> createMethodHandlerFor(each, descriptor))
          .collect(toList());
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
  }
}
