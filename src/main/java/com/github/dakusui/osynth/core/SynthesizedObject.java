package com.github.dakusui.osynth.core;

import com.github.dakusui.osynth.annotations.BuiltInHandlerFactory;
import com.github.dakusui.osynth.annotations.ReservedByOSynth;
import com.github.dakusui.osynth.core.utils.AssertionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.dakusui.osynth.core.AbstractObjectSynthesizer.DEFAULT_FALLBACK_OBJECT;
import static com.github.dakusui.osynth.core.MethodHandlerDecorator.chainMethodHandlerDecorators;
import static com.github.dakusui.osynth.core.SynthesizedObject.InternalUtils.builtIndMethodSignatures;
import static com.github.dakusui.osynth.core.SynthesizedObject.InternalUtils.reservedMethodSignatures;
import static com.github.dakusui.osynth.core.utils.MessageUtils.messageForAttemptToCastToUnavailableInterface;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.valid8j.Requires.require;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public interface SynthesizedObject {
  Set<Method> RESERVED_METHODS = reservedMethodSignatures();
  Set<Method> BUILT_IN_METHODS = builtIndMethodSignatures();

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

    static Set<Method> reservedMethodSignatures() {
      return methodsAnnotatedBy(ReservedByOSynth.class);
    }

    static Set<Method> builtIndMethodSignatures() {
      return methodsAnnotatedBy(BuiltInHandlerFactory.class);
    }

    private static Set<Method> methodsAnnotatedBy(Class<? extends Annotation> annotationClass) {
      return Arrays.stream(SynthesizedObject.class.getMethods())
          .filter(each -> each.isAnnotationPresent(annotationClass))
          .collect(toSet());
    }
  }

  /**
   * A class to describe attributes of a synthesized object.
   */
  final class Descriptor {
    final List<MethodHandlerEntry> methodHandlers;
    final List<Class<?>>           interfaces;
    final MethodHandlerDecorator   methodHandlerDecorator;
    final Object                   fallbackObject;

    public Descriptor(
        List<Class<?>> interfaces,
        List<MethodHandlerEntry> methodHandlers,
        MethodHandlerDecorator methodHandlerDecorator,
        Object fallbackObject) {
      // Not using pcond library to avoid unintentional `toString` call back on failure.
      this.methodHandlers = new LinkedList<>(Objects.requireNonNull(unmodifiableList(methodHandlers)));
      this.interfaces = new LinkedList<>(Objects.requireNonNull(interfaces));
      this.fallbackObject = Objects.requireNonNull(fallbackObject);
      this.methodHandlerDecorator = Objects.requireNonNull(methodHandlerDecorator);
    }

    public static List<MethodHandlerEntry> nonBuiltInMethodHandlerEntriesOf(Descriptor b) {
      return b.methodHandlerEntries().stream().filter(methodHandlerEntry -> !methodHandlerEntry.isBuiltIn()).collect(toList());
    }

    /**
     * Returns a new descriptor merging two descriptors.
     * The merge happens in a manner, where "a overrides b".
     *
     * @param a A descriptor to merge.
     * @param b Another descriptor to merge.
     * @return A merged new descriptor object.
     */
    public static Descriptor merge(Descriptor a, Descriptor b) {
      Builder builder = a.toBuilder();
      if (a.fallbackObject() == DEFAULT_FALLBACK_OBJECT)
        builder.fallbackObject(b.fallbackObject());
      nonBuiltInMethodHandlerEntriesOf(b).forEach(builder::addMethodHandler);
      b.interfaces().forEach(builder::addInterface);
      builder.methodHandlerDecorator(chainMethodHandlerDecorators(a.methodHandlerDecorator(), b.methodHandlerDecorator()));
      return builder.build();
    }

    public List<Class<?>> interfaces() {
      return unmodifiableList(this.interfaces);
    }

    public MethodHandlerDecorator methodHandlerDecorator() {
      return this.methodHandlerDecorator;
    }

    public Object fallbackObject() {
      return this.fallbackObject;
    }

    public List<MethodHandlerEntry> methodHandlerEntries() {
      return this.methodHandlers;
    }

    @Override
    public int hashCode() {
      return this.fallbackObject.hashCode();
    }

    @Override
    public boolean equals(Object anotherObject) {
      if (this == anotherObject)
        return true;
      if (!(anotherObject instanceof Descriptor)) {
        return false;
      }
      Descriptor another = (Descriptor) anotherObject;
      Set<MethodHandlerEntry> methodHandlerSet = new HashSet<>(nonBuiltInMethodHandlerEntriesOf(this));
      Set<MethodHandlerEntry> methodHandlerSetFromAnother = new HashSet<>(nonBuiltInMethodHandlerEntriesOf(another));
      return Objects.equals(fallbackObject, another.fallbackObject) &&
          Objects.equals(methodHandlerDecorator, another.methodHandlerDecorator) &&
          Objects.equals(interfaces, another.interfaces) &&
          Objects.equals(methodHandlerSet, methodHandlerSetFromAnother);
    }

    @Override
    public String toString() {
      return format("{methodHandlers=%s,interfaces=%s,fallback:%s}",
          nonBuiltInMethodHandlerEntriesOf(this),
          this.interfaces(),
          this.fallbackObject());
    }

    public Descriptor.Builder toBuilder() {
      return new Descriptor.Builder() {
        {
          Descriptor.this.interfaces().forEach(this::addInterface);
          this.fallbackObject = Descriptor.this.fallbackObject;
          this.methodHandlerDecorator = Descriptor.this.methodHandlerDecorator();
          this.methodHandlers.addAll(nonBuiltInMethodHandlerEntriesOf(this.build()));
        }
      }.fallbackObject(this.fallbackObject());
    }

    public static class Builder {
      final List<Class<?>>           interfaces;
      final List<MethodHandlerEntry> methodHandlers;
      MethodHandlerDecorator methodHandlerDecorator;
      Object                 fallbackObject;

      public Builder() {
        interfaces = new LinkedList<>();
        methodHandlers = new LinkedList<>();
      }

      public Builder(Descriptor descriptor) {
        this();
        this.interfaces.addAll(descriptor.interfaces());
        this.methodHandlers.addAll(descriptor.methodHandlerEntries());
        this.methodHandlerDecorator = descriptor.methodHandlerDecorator();
        this.fallbackObject = descriptor.fallbackObject();
      }

      public Builder fallbackObject(Object fallbackObject) {
        this.fallbackObject = fallbackObject;
        return this;
      }

      public Builder addInterface(Class<?> interfaceClass) {
        this.interfaces.add(interfaceClass);
        return this;
      }

      public Builder methodHandlerDecorator(MethodHandlerDecorator methodHandlerDecorator) {
        this.methodHandlerDecorator = methodHandlerDecorator;
        return this;
      }

      public void addMethodHandler(MethodHandlerEntry methodHandlerEntry) {
        this.methodHandlers.add(methodHandlerEntry);
      }


      public List<Class<?>> interfaces() {
        return unmodifiableList(this.interfaces);
      }

      public MethodHandlerDecorator methodHandlerDecorator() {
        return this.methodHandlerDecorator;
      }

      public Descriptor build() {
        return new Descriptor(this.interfaces, this.methodHandlers, this.methodHandlerDecorator, this.fallbackObject);
      }
    }
  }
}
