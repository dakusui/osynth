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
  public enum HandlerSelectionOrder {
    V1_0 {
      @Override
      BiFunction<Object, Object[], Object> createMethodCallHandler(ProxyFactory proxyFactory, Method method) {
        return proxyFactory.createV10MethodCallHandler(method);
      }
    },
    V2_0 {
      @Override
      BiFunction<Object, Object[], Object> createMethodCallHandler(ProxyFactory proxyFactory, Method method) {
        return proxyFactory.createV20MethodCallHandler(method);
      }
    };

    abstract BiFunction<Object, Object[], Object> createMethodCallHandler(ProxyFactory proxyFactory, Method method);
  }

  private final List<Class<?>>         interfaces;
  private final List<MethodHandler>    handlers;
  private final List<MethodHandler>    builtInHandlers;
  private final List<Object>           handlerObjects;
  private final FallbackHandlerFactory fallbackHandlerFactory;
  private final HandlerSelectionOrder  handlerSelectionOrder;

  public ProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects, FallbackHandlerFactory fallbackHandlerFactory, HandlerSelectionOrder handlerSelectionOrder) {
    this.interfaces = interfaces;
    this.handlers = handlers;
    this.handlerObjects = handlerObjects;
    this.handlerSelectionOrder = handlerSelectionOrder;
    this.interfaces.add(Describable.class);
    this.builtInHandlers = asList(
        hashCodeHandler(this),
        equalsHandler(this, describeIfPossible()),
        toStringHandler(this, v -> "proxy:" + v.toString()),
        builderByNameAndParameterTypes("describe").with((self, args) -> this)
    );
    this.fallbackHandlerFactory = fallbackHandlerFactory;
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
          this.fallbackHandlerFactory.equals(another.fallbackHandlerFactory);
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

  public BiFunction<Object, Object[], Object> fallbackHandler(Method method) {
    return this.fallbackHandlerFactory.apply(this)
        .apply(method)
        .orElseThrow(() -> new IllegalArgumentException(noHandlerFound(this.handlerObjects, method)));
  }

  public HandlerSelectionOrder handlerSelectionOrder() {
    return this.handlerSelectionOrder;
  }

  public ProxyDescriptor overrideWith(ProxyDescriptor proxyDescriptor) {
    return new ProxyDescriptor(
        Stream.concat(proxyDescriptor.interfaces().stream(), this.interfaces().stream()).distinct().collect(toList()),
        Stream.concat(proxyDescriptor.handlers().stream(), this.handlers().stream()).distinct().collect(toList()),
        Stream.concat(proxyDescriptor.handlerObjects().stream(), this.handlerObjects().stream()).distinct().collect(toList()),
        this.fallbackHandlerFactory.compose(proxyDescriptor.fallbackHandlerFactory),
        this.handlerSelectionOrder);
  }
}
