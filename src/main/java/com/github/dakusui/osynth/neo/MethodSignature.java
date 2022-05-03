package com.github.dakusui.osynth.neo;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import static com.github.dakusui.pcond.Preconditions.requireNonNull;
import static java.util.stream.Collectors.joining;

public final class MethodSignature {
  final String     name;
  final Class<?>[] parameterClasses;

  public MethodSignature(String name, Class<?>[] parameterClasses) {
    this.name = requireNonNull(name);
    this.parameterClasses = requireNonNull(parameterClasses);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.name) + Objects.hashCode(this.parameterClasses);
  }

  @Override
  public boolean equals(Object anotherObject) {
    if (!(anotherObject instanceof MethodSignature))
      return false;
    MethodSignature another = (MethodSignature) anotherObject;
    return Objects.equals(this.name, another.name) && Arrays.equals(this.parameterClasses, another.parameterClasses);
  }

  @Override
  public String toString() {
    return String.format("%s(%s)",
        this.name,
        Arrays.stream(this.parameterClasses)
            .map(Class::getSimpleName)
            .collect(joining(",")));
  }

  public String name() {
    return this.name;
  }

  public Class<?>[] parameterClasses() {
    return Arrays.copyOf(parameterClasses, parameterClasses.length);
  }

  public static MethodSignature create(String name, Class<?>... parameterClasses) {
    return new MethodSignature(name, parameterClasses);
  }

  public static MethodSignature create(Method method) {
    return create(method.getName(), method.getParameterTypes());
  }
}
