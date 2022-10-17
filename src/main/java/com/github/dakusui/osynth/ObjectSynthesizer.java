package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.FallbackHandlerFactory;
import com.github.dakusui.osynth.core.MethodHandler;
import com.github.dakusui.osynth.core.ProxyDescriptor;
import com.github.dakusui.osynth.core.ProxyFactory;
import com.github.dakusui.osynth.utils.InternalFunctions;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.github.dakusui.osynth.utils.InternalFunctions.listOf;
import static com.github.dakusui.osynth.utils.InternalPredicates.*;
import static com.github.dakusui.osynth.utils.Messages.notAnInterface;
import static com.github.dakusui.pcond.Requires.*;
import static com.github.dakusui.pcond.forms.Experimentals.nest;
import static com.github.dakusui.pcond.forms.Experimentals.toContextPredicate;
import static com.github.dakusui.pcond.forms.Functions.stream;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class ObjectSynthesizer {
  enum ValidationMode {
    SYNTHESIZE {
      @Override
      ObjectSynthesizer validate(ObjectSynthesizer target) {
        target.interfaces.forEach(each -> requireState(target.handlerObjects, transform(stream()).check(noneMatch(isInstanceOf(each)))));
        return target;
      }
    },
    TWEAK {
      @Override
      ObjectSynthesizer validate(ObjectSynthesizer target) {
        target.interfaces.forEach(each -> requireState(each, transform(InternalFunctions.methods().andThen(stream())).check(noneMatch(isDefaultMethod()))));
        return target;
      }
    },

    /**
     * No validation is made.
     */
    NOTHING {
      @Override
      ObjectSynthesizer validate(ObjectSynthesizer target) {
        return target;
      }
    };

    abstract ObjectSynthesizer validate(ObjectSynthesizer target);


  }

  public static final FallbackHandlerFactory DEFAULT_FALLBACK_HANDLER_FACTORY = desc -> (Method method) -> Optional.empty();
  private final       List<Class<?>>         interfaces                       = new LinkedList<>();
  private final       List<Object>           handlerObjects                   = new LinkedList<>();
  private final       List<MethodHandler>    handlers                         = new LinkedList<>();
  private             FallbackHandlerFactory fallbackHandlerFactory;
  private             ValidationMode         validationMode;

  public ObjectSynthesizer() {
    this.validationMode = ValidationMode.NOTHING;
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

  public ObjectSynthesizer validationMode(ValidationMode validationMode) {
    this.validationMode = requireNonNull(validationMode);
    return this;
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize() {
    return (T) this.synthesize(Object.class);
  }

  @SuppressWarnings("unchecked")
  public <T> T synthesize(Class<T> aClass) {
    requireNonNull(aClass);
    requireArgument(
        aClass,
        or(isEqualTo(Object.class),
            isInterfaceClass()
                .and(transform(
                    listOf()
                        .andThen(stream())
                        .andThen(nest(interfaces)))
                    .check(anyMatch(toContextPredicate(isAssignableFrom()))))));
    return (T) this.validationMode
        .validate(this)
        .createProxyFactory(this.createProxyDescriptor())
        .create();
  }

  @SuppressWarnings("unchecked")
  public <T> T resynthesizeFrom(T base) {
    Synthesized synthesized = (Synthesized) requireArgument(base, isNotNull().and(isInstanceOf(Synthesized.class)));
    return (T) createProxyFactory(
        synthesized.osynthProxyDescriptor()
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

  public static ObjectSynthesizer synthesizer() {
    return create(false).validationMode(ValidationMode.SYNTHESIZE);
  }

  public static ObjectSynthesizer tweaker() {
    return create(true).validationMode(ValidationMode.TWEAK);
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
}
