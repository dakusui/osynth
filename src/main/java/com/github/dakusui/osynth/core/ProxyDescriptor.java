package com.github.dakusui.osynth.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.dakusui.osynth.core.MethodHandler.*;
import static com.github.dakusui.osynth.utils.Messages.noHandlerFound;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

public class ProxyDescriptor {
  private final List<Class<?>>               interfaces;
  private final List<MethodHandler>          handlers;
  private final List<MethodHandler>          builtInHandlers;
  private final List<Object>                 handlerObjects;
  private final FallbackMethodHandlerFactory fallbackMethodHandlerFactory;

  public ProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects, FallbackMethodHandlerFactory fallbackMethodHandlerFactory) {
    this.interfaces = interfaces;
    this.handlers = handlers;
    this.handlerObjects = handlerObjects;
    this.interfaces.add(Describable.class);
    this.builtInHandlers = asList(
        hashCodeHandler(this),
        equalsHandler(this, describeIfPossible()),
        toStringHandler(this, v -> "proxy:" + v.toString()),
        builderByNameAndParameterTypes("describe").with((self, args) -> this)
    );
    this.fallbackMethodHandlerFactory = fallbackMethodHandlerFactory;
  }

  private Function<Object, Object> describeIfPossible() {
    return v -> v instanceof Describable ?
        ((Describable) v).describe() :
        v;
  }

  public Stream<MethodHandler> streamAllHandlers() {
    return Stream.concat(this.handlers.stream(), this.builtInHandlers.stream());
  }

  @Override
  public int hashCode() {
    return handlers.hashCode();
  }

  @Override
  public boolean equals(Object anotherObject) {
    if (this == anotherObject)
      return true;
    ProxyDescriptor another;
    if ((anotherObject instanceof ProxyDescriptor)) {
      another = (ProxyDescriptor) anotherObject;
      return this.interfaces().equals(another.interfaces()) &&
          this.handlers().equals(another.handlers()) &&
          this.handlerObjects().equals(another.handlerObjects()) &&
          this.fallbackMethodHandlerFactory.equals(another.fallbackMethodHandlerFactory);
    }
    return false;
  }

  @Override
  public String toString() {
    return "osynth:" + this.getClass().getCanonicalName() + "@" + System.identityHashCode(this);
  }

  protected List<Class<?>> interfaces() {
    return unmodifiableList(this.interfaces);
  }

  protected List<MethodHandler> handlers() {
    return unmodifiableList(this.handlers);
  }

  public List<Object> handlerObjects() {
    return unmodifiableList(this.handlerObjects);
  }

  public BiFunction<Object, Object[], Object> fallbackMethodHandlerFor(Method method) {
    return this.fallbackMethodHandlerFactory
        .apply(this)
        .apply(method)
        .orElseThrow(() -> new IllegalArgumentException(noHandlerFound(this.handlerObjects, method)));
  }

  public ProxyDescriptor overrideWith(ProxyDescriptor proxyDescriptor) {
    return new ProxyDescriptor(
        Stream.concat(proxyDescriptor.interfaces().stream(), this.interfaces().stream()).distinct().collect(toList()),
        Stream.concat(proxyDescriptor.handlers().stream(), this.handlers().stream()).distinct().collect(toList()),
        Stream.concat(proxyDescriptor.handlerObjects().stream(), this.handlerObjects().stream()).distinct().collect(toList()),
        this.fallbackMethodHandlerFactory.compose(proxyDescriptor.fallbackMethodHandlerFactory));
  }
}
