package com.github.dakusui.osynth;

import java.util.List;

public class SimpleObjectSynthesizer<T> extends ObjectSynthesizer {

  private final Class<T> primaryInterface;

  public SimpleObjectSynthesizer(Class<T> anInterface) {
    this.primaryInterface = anInterface;
    this.addInterface(anInterface);
  }

  public static <T> SimpleObjectSynthesizer<T> create(Class<T> anInterface) {
    return new SimpleObjectSynthesizer<>(anInterface);
  }

  @Override
  @SuppressWarnings("unchecked")
  public SimpleObjectSynthesizer<T> addHandlerObject(Object handlerObject) {
    return (SimpleObjectSynthesizer<T>) super.addHandlerObject(handlerObject);
  }

  @Override
  @SuppressWarnings("unchecked")
  public SimpleObjectSynthesizer<T> handle(MethodHandler handler) {
    return (SimpleObjectSynthesizer<T>) super.handle(handler);
  }

  @Override
  ProxyDescriptor createProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects) {
    return new ProxyDescriptor(interfaces, handlers, handlerObjects) {
      @Override
      protected boolean equalsLeniently(Object anotherObject) {
        return this.handlerObects().contains(anotherObject);
      }
    };
  }

  @Override
  public T synthesize() {
    return super.synthesize(this.primaryInterface);
  }
}
