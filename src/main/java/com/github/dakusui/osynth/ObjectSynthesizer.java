package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.*;

import java.lang.reflect.Method;
import java.util.*;

import static com.github.dakusui.osynth.utils.Messages.*;
import static com.github.dakusui.osynth.utils.InternalUtils.rethrow;
import static com.github.dakusui.pcond.Preconditions.requireArgument;
import static com.github.dakusui.pcond.functions.Predicates.isInstanceOf;
import static com.github.dakusui.pcond.functions.Predicates.isNotNull;
import static java.util.Objects.requireNonNull;

public class ObjectSynthesizer {
  public static final FallbackHandlerFactory DEFAULT_FALLBACK_HANDLER_FACTORY = desc -> (Method method) -> Optional.empty();
  public static final Method                 DESCRIPTOR_METHOD                = retrieveDescriptorMethod();
  private final        List<Class<?>>         interfaces                       = new LinkedList<>();
  private              List<Object>           handlerObjects                   = new LinkedList<>();
  private              List<MethodHandler>    handlers                         = new LinkedList<>();
  private              FallbackHandlerFactory fallbackHandlerFactory;

  public ObjectSynthesizer() {
    this.fallbackHandlerFactory(DEFAULT_FALLBACK_HANDLER_FACTORY);
  }

  public ObjectSynthesizer addInterface(Class<?> anInterface) {
    if (!requireNonNull(anInterface).isInterface())
      throw new IllegalArgumentException(notAnInterface(anInterface));
    if (!this.interfaces.contains(anInterface))
      this.interfaces.add(anInterface);
    return this;
  }

  /**
   * In a situation, where a method in the {@code handlerObject} calls another method in the interface directly or indirectly
   * and you intend to override the callee method's behavior by the {@link ObjectSynthesizer}, it results in a counter-intuitive
   * behavior.
   * <p>
   * Since implementation of a default method is "synthesized" in an implementation class when the implementing class is compiled,
   * call on the callee method from the caller is invoked directly on "this", not through the dynamic proxy.
   * Hence the method handler is ignored even if it is given to the object synthesize for the callee method.
   * <p>
   * Best practice to avoid this is not to add a handler object that implements an interface that has default methods.
   * That is,
   * 1. Create a separated interface S that contains only no default methods and extend it by your original interface.
   * 2. Instantiate an object that implements S and add it to the synthesizer through this method.
   * Another approach is,
   * Create an object that has a public method (or methods) you desire to override its behavior and add it to the synthesizer through this method.
   * The second approach is easier but a bit more error-prone since it cannot rely on the compiler for the correctness of the method's signature.
   *
   * @param handlerObject An object that handles method invocation
   * @return This object
   */
  public ObjectSynthesizer addHandlerObject(Object handlerObject) {
    this.handlerObjects.add(requireNonNull(handlerObject));
    return this;
  }

  public ObjectSynthesizer handle(MethodHandler handler) {
    this.handlers.add(requireNonNull(handler));
    return this;
  }

  public ObjectSynthesizer fallbackHandlerFactory(FallbackHandlerFactory fallbackHandlerFactory) {
    this.fallbackHandlerFactory = requireNonNull(fallbackHandlerFactory);
    return this;
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize() {
    return (T) this.synthesize(Object.class);
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize(Class<T> anInterface) {
    requireNonNull(anInterface);
    if (this.interfaces.stream().noneMatch(anInterface::isAssignableFrom)) {
      throw new IllegalArgumentException(noMatchingInterface(anInterface, interfaces));
    }
    return (T) this.createProxyFactory(this.createProxyDescriptor()).create();
  }

  @SuppressWarnings("unchecked")
  public <T> T resynthesizeFrom(T base) {
    Synthesized synthesized = (Synthesized) requireArgument(base, isNotNull().and(isInstanceOf(Synthesized.class)));
    return (T) createProxyFactory(
        synthesized.descriptor()
            .overrideWith(this.createProxyDescriptor()))
        .create();
  }

  private ProxyFactory createProxyFactory(ProxyDescriptor proxyDescriptor) {
    return new ProxyFactory(proxyDescriptor);
  }

  private ProxyDescriptor createProxyDescriptor() {
    return createProxyDescriptor(interfaces, handlers, handlerObjects, fallbackHandlerFactory);
  }

  protected ProxyDescriptor createProxyDescriptor(List<Class<?>> interfaces, List<MethodHandler> handlers, List<Object> handlerObjects, FallbackHandlerFactory fallbackHandlerFactory) {
    return new ProxyDescriptor(
        interfaces,
        handlers,
        handlerObjects,
        fallbackHandlerFactory);
  }

  public static ObjectSynthesizer create(boolean auto) {
    return auto ?
        new ObjectSynthesizer() {
          @Override
          public ObjectSynthesizer addHandlerObject(Object handlerObject) {
            requireNonNull(handlerObject);
            for (Class<?> eachInterface : handlerObject.getClass().getInterfaces())
              addInterface(eachInterface);
            return super.addHandlerObject(handlerObject);
          }
        } :
        new ObjectSynthesizer();
  }

  public static MethodHandler.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return MethodHandler.builderByNameAndParameterTypes(requireNonNull(methodName), requireNonNull(parameterTypes));
  }


  private static Method retrieveDescriptorMethod() {
    try {
      return Synthesized.class.getMethod("descriptor");
    } catch (NoSuchMethodException e) {
      throw rethrow(e);
    }
  }

}
