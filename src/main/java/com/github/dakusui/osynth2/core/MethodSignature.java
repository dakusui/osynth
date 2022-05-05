package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

public interface MethodSignature extends MethodMatcher {
  String name();

  Class<?>[] parameterClasses();

  static com.github.dakusui.osynth2.core.MethodSignature create(String name, Class<?>... parameterClasses) {
    return new MethodSignature.Impl(name, parameterClasses);
  }

  static com.github.dakusui.osynth2.core.MethodSignature create(Method method) {
    return create(method.getName(), method.getParameterTypes());
  }

  final class Impl implements MethodMatcher, com.github.dakusui.osynth2.core.MethodSignature {
    final String     name;
    final Class<?>[] parameterClasses;

    public Impl(String name, Class<?>[] parameterClasses) {
      this.name = Objects.requireNonNull(name);
      this.parameterClasses = Objects.requireNonNull(parameterClasses);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(this.name);
    }

    @Override
    public boolean equals(Object anotherObject) {
      if (!(anotherObject instanceof MethodSignature))
        return false;
      MethodSignature another = (MethodSignature) anotherObject;
      return Objects.equals(this.name(), another.name()) && Arrays.equals(this.parameterClasses(), another.parameterClasses());
    }

    @Override
    public String toString() {
      return String.format("%s(%s)", this.name, Arrays.stream(this.parameterClasses).map(Class::getSimpleName).collect(joining(",")));
    }

    @Override
    public String name() {
      return this.name;
    }

    @Override
    public Class<?>[] parameterClasses() {
      return Arrays.copyOf(parameterClasses, parameterClasses.length);
    }

    @Override
    public boolean matches(com.github.dakusui.osynth2.core.MethodSignature m) {
      Objects.requireNonNull(m);
      return Objects.equals(this.name, m.name()) && Arrays.equals(this.parameterClasses, m.parameterClasses());
    }
  }
}
