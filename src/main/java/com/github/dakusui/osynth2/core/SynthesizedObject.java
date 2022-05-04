package com.github.dakusui.osynth2.core;

import com.github.dakusui.osynth2.annotations.BuiltInHandlerFactory;
import com.github.dakusui.osynth2.annotations.ReservedByOSynth;
import com.github.dakusui.osynth2.core.utils.AssertionUtils;

import java.util.*;

import static com.github.dakusui.osynth2.core.SynthesizedObject.InternalUtils.reservedMethodSignatures;
import static com.github.dakusui.osynth2.core.utils.MessageUtils.messageForAttemptToCastToUnavailableInterface;
import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toSet;

public interface SynthesizedObject {
  Set<MethodSignature> RESERVED_METHOD_SIGNATURES = reservedMethodSignatures();

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForDescriptor.class)
  @ReservedByOSynth
  Descriptor descriptor();

  @ReservedByOSynth
  default <T> T castTo(Class<T> classInUse) {
    require(classInUse, and(
        isNotNull(),
        or(AssertionUtils.classIsInterface(), isEqualTo(Object.class))));
    return descriptor().interfaces().stream()
        .filter(classInUse::isAssignableFrom)
        .findFirst()
        .map(each -> classInUse.cast(this))
        .orElseThrow(() -> new ClassCastException(messageForAttemptToCastToUnavailableInterface(classInUse, descriptor().interfaces())));
  }

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForEquals.class)
  @Override
  boolean equals(Object object);

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForHashCode.class)
  @Override
  int hashCode();

  @BuiltInHandlerFactory(BuiltInHandlerFactory.ForToString.class)
  @Override
  String toString();

  enum InternalUtils {
    ;

    static Set<MethodSignature> reservedMethodSignatures() {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(ReservedByOSynth.class))
          .map(MethodSignature::create)
          .collect(toSet());
    }

  }

  final class Descriptor {
    final List<Class<?>>           interfaces;
    final Object                   fallbackObject;
    final List<MethodHandlerEntry> methodHandlers;

    public Descriptor(List<Class<?>> interfaces, List<MethodHandlerEntry> methodHandlers, Object fallbackObject) {
      // Not using pcond library to avoid unintentional `toString` call back on failure.
      this.methodHandlers = new LinkedList<>(Objects.requireNonNull(unmodifiableList(methodHandlers)));
      this.interfaces = new LinkedList<>(Objects.requireNonNull(interfaces));
      this.fallbackObject = Objects.requireNonNull(fallbackObject);
    }

    public List<Class<?>> interfaces() {
      return unmodifiableList(this.interfaces);
    }

    public Object fallbackObject() {
      return this.fallbackObject;
    }

    public List<MethodHandlerEntry> methodHandlers() {
      return this.methodHandlers;
    }

    @Override
    public String toString() {
      return String.format("{methodHandlers=%s,interfaces=%s,fallback:%s}",
          this.methodHandlers(),
          this.interfaces(),
          this.fallbackObject());
    }

    public static class Builder {
      final List<Class<?>>           interfaces;
      final List<MethodHandlerEntry> methodHandlers;
      Object fallbackObject;

      public Builder() {
        interfaces = new LinkedList<>();
        methodHandlers = new LinkedList<>();
      }

      public Builder(Descriptor descriptor) {
        this();
        this.interfaces.addAll(descriptor.interfaces());
        this.methodHandlers.addAll(descriptor.methodHandlers());
        this.fallbackObject = descriptor.fallbackObject;
      }

      public Builder fallbackObject(Object fallbackObject) {
        this.fallbackObject = fallbackObject;
        return this;
      }

      public Builder addInterface(Class<?> interfaceClass) {
        this.interfaces.add(interfaceClass);
        return this;
      }

      public void addMethodHandler(MethodSignature forMethod, MethodHandler methodHandler) {
        this.methodHandlers.add(MethodHandlerEntry.create(forMethod, methodHandler));
      }

      public Descriptor build() {
        return new Descriptor(this.interfaces, this.methodHandlers, this.fallbackObject);
      }

      public List<Class<?>> interfaces() {
        return unmodifiableList(this.interfaces);
      }
    }
  }
}
