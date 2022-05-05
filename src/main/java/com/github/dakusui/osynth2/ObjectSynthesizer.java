package com.github.dakusui.osynth2;

import com.github.dakusui.osynth2.core.*;
import com.github.dakusui.osynth2.core.utils.AssertionUtils;
import com.github.dakusui.osynth2.exceptions.ValidationException;
import com.github.dakusui.pcond.Validations;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.dakusui.osynth2.ObjectSynthesizer.InternalUtils.reservedMethodMisOverridings;
import static com.github.dakusui.osynth2.ObjectSynthesizer.InternalUtils.validateValue;
import static com.github.dakusui.osynth2.core.SynthesizedObject.RESERVED_METHOD_SIGNATURES;
import static com.github.dakusui.osynth2.core.utils.AssertionUtils.*;
import static com.github.dakusui.osynth2.core.utils.MessageUtils.messageForReservedMethodOverridingValidationFailure;
import static com.github.dakusui.osynth2.annotations.BuiltInHandlerFactory.MethodHandlerFactory.createMethodHandlersForBuiltInMethods;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Postconditions.ensure;
import static com.github.dakusui.pcond.Preconditions.*;
import static com.github.dakusui.pcond.forms.Functions.parameter;
import static com.github.dakusui.pcond.forms.Functions.stream;
import static com.github.dakusui.pcond.forms.Predicates.*;

public class ObjectSynthesizer {
  interface Stage extends BiFunction<ObjectSynthesizer, SynthesizedObject.Descriptor, SynthesizedObject.Descriptor> {
  }

  interface Validator extends Stage {
    Validator DEFAULT = toNamed("defaultValidator", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      validateValue(
          descriptor,
          withMessage(
              () -> messageForReservedMethodOverridingValidationFailure(reservedMethodMisOverridings(descriptor.methodHandlers())),
              transform(descriptorMethodHandlers()
                  .andThen(InternalUtils::reservedMethodMisOverridings))
                  .check(isEmpty())));
      return descriptor;
    });

    Validator ENFORCE_NO_DUPLICATION = toNamed("noDuplicationEnforcingValidator", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      validateValue(descriptor, transform(descriptorInterfaces().andThen(AssertionUtils.collectionDuplicatedElements())).check(isEmpty()));
      return descriptor;
    });

    Validator PASS_THROUGH = toNamed("passThroughValidator", (objectSynthesizer, descriptor) -> descriptor);

    static Validator sequence(Validator... validators) {
      return toNamed("validatorSequence:" + Arrays.toString(validators), (objectSynthesizer, descriptor) -> {
        SynthesizedObject.Descriptor ret = descriptor;
        for (Validator each : validators) {
          ret = requireNonNull(each).apply(objectSynthesizer, descriptor);
          ensure(ret, withMessage("Validation must not change the content of the descriptor.", allOf(
              transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
              transform(descriptorMethodHandlers()).check(isEqualTo(descriptor.methodHandlers())),
              transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject())))));
        }
        return ret;
      });
    }

    static Validator toNamed(String name, Validator validator) {
      require(name, isNotNull());
      require(validator, isNotNull());
      return new Validator() {
        @Override
        public SynthesizedObject.Descriptor apply(ObjectSynthesizer objectSynthesizer, SynthesizedObject.Descriptor descriptor) {
          return validator.apply(objectSynthesizer, descriptor);
        }

        @Override
        public String toString() {
          return name;
        }
      };
    }

    SynthesizedObject.Descriptor apply(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);
  }

  interface InvocationHandlerFactory extends Function<ObjectSynthesizer, OsynthInvocationHandler> {

  }

  interface Preprocessor {
    Preprocessor INCLUDE_BUILTIN_METHOD_HANDLERS = toNamed("builtInMethodHandlers", ((objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      createMethodHandlersForBuiltInMethods(() -> objectSynthesizer.finalizedDescriptor())
          .forEach(builder::addMethodHandler);
      return builder.build();
    }));
    Preprocessor INCLUDE_BUILTIN_INTERFACES      = toNamed("builtInInterfaces", ((objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      if (!builder.interfaces().contains(SynthesizedObject.class))
        builder.addInterface(SynthesizedObject.class);
      return builder.build();
    }));

    Preprocessor DEFAULT                          = toNamed("defaultPreprocessor", sequence(
        INCLUDE_BUILTIN_METHOD_HANDLERS,
        INCLUDE_BUILTIN_INTERFACES
    ));
    Preprocessor INCLUDE_INTERFACES_FROM_FALLBACK = toNamed("interfacesFromFallback", (objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      Set<Class<?>> interfacesInOriginalDescriptor = new HashSet<>(descriptor.interfaces());
      Arrays.stream(descriptor.fallbackObject().getClass().getInterfaces())
          .filter(eachInterfaceInFallback -> !interfacesInOriginalDescriptor.contains(eachInterfaceInFallback))
          .forEach(builder::addInterface);
      return builder.build();
    });

    Preprocessor PASS_THROUGH = toNamed("passThrough", (objectSynthesizer, descriptor) -> descriptor);

    static Preprocessor sequence(Preprocessor... preprocessors) {
      return toNamed("preprocessorSequence:" + Arrays.toString(preprocessors), (objectSynthesizer, descriptor) -> {
        SynthesizedObject.Descriptor ret = descriptor;
        for (Preprocessor each : preprocessors) {
          ret = ensure(requireNonNull(each).apply(objectSynthesizer, ret), isNotNull());
        }
        return ret;
      });
    }

    SynthesizedObject.Descriptor apply(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);

    static Preprocessor toNamed(String name, Preprocessor preprocessor) {
      require(name, isNotNull());
      require(preprocessor, isNotNull());
      return new Preprocessor() {
        @Override
        public SynthesizedObject.Descriptor apply(ObjectSynthesizer objectSynthesizer, SynthesizedObject.Descriptor descriptor) {
          return preprocessor.apply(objectSynthesizer, descriptor);
        }

        @Override
        public String toString() {
          return name;
        }

      };
    }
  }

  private final SynthesizedObject.Descriptor.Builder          descriptorBuilder;
  private       Validator                                     validator;
  private       Preprocessor                                  preprocessor;
  private       ClassLoader                                   classLoader;
  private       InvocationHandlerFactory                      invocationHandlerFactory;
  private final AtomicReference<SynthesizedObject.Descriptor> finalizedDescriptor = new AtomicReference<>(null);

  public ObjectSynthesizer() {
    this.descriptorBuilder = new SynthesizedObject.Descriptor.Builder().fallbackObject(new Object() {
      @Override
      public String toString() {
        return "autoCreated:" + super.toString();
      }
    });
    this.classLoader(this.getClass().getClassLoader())
        .createInvocationHandlerWith(objectSynthesizer -> new OsynthInvocationHandler(
            objectSynthesizer
                .finalizedDescriptor()))
        .validateWith(Validator.DEFAULT)
        .preprocessWith(Preprocessor.DEFAULT);
  }

  public ObjectSynthesizer addInterface(Class<?> interfaceClass) {
    descriptorBuilder.addInterface(interfaceClass);
    return this;
  }

  public ObjectSynthesizer classLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public ObjectSynthesizer fallbackObject(Object fallbackObject) {
    this.descriptorBuilder.fallbackObject(fallbackObject);
    return this;
  }

  public ObjectSynthesizer handle(MethodHandlerEntry handlerEntry) {
    requireNonNull(handlerEntry);
    this.descriptorBuilder.addMethodHandler(handlerEntry);
    return this;
  }

  public ObjectSynthesizer validateWith(Validator validator) {
    this.validator = requireNonNull(validator);
    return this;
  }

  public ObjectSynthesizer enableDuplicatedInterfaceCheck() {
    return this.validateWith(Validator.sequence(this.validator(), Validator.ENFORCE_NO_DUPLICATION));

  }

  public ObjectSynthesizer disableValidation() {
    return this.validateWith(Validator.PASS_THROUGH);
  }

  public ObjectSynthesizer preprocessWith(Preprocessor preprocessor) {
    this.preprocessor = requireNonNull(preprocessor);
    return this;
  }

  public ObjectSynthesizer includeInterfacesFrom() {
    return this.preprocessWith(Preprocessor.sequence(this.preprocessor(), Preprocessor.INCLUDE_INTERFACES_FROM_FALLBACK));
  }

  public ObjectSynthesizer disablePreprocessing() {
    return this.preprocessWith(Preprocessor.PASS_THROUGH);
  }

  public ObjectSynthesizer createInvocationHandlerWith(InvocationHandlerFactory factory) {
    this.invocationHandlerFactory = requireNonNull(factory);
    return this;
  }

  public SynthesizedObject synthesize(Object fallbackObject) {
    return this.fallbackObject(fallbackObject).synthesize();
  }

  public SynthesizedObject synthesize() {
    finalizeDescriptor(preprocessDescriptor(validateDescriptor(this.descriptorBuilder.build())));
    return (SynthesizedObject) InternalUtils.createProxy(this);
  }

  private void finalizeDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.isDescriptorFinalized(), isFalse());
    this.finalizedDescriptor.set(descriptor);
  }

  public Preprocessor preprocessor() {
    return this.preprocessor;
  }

  public Validator validator() {
    return this.validator;
  }

  public SynthesizedObject.Descriptor finalizedDescriptor() {
    return Optional.ofNullable(finalizedDescriptor.get())
        .orElseThrow(IllegalAccessError::new);
  }

  public boolean isDescriptorFinalized() {
    return finalizedDescriptor.get() != null;
  }

  public static MethodHandlerEntry.Builder method(String methodName, Class<?>... parameterTypes) {
    return method(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder method(MethodMatcher matcher) {
    return new MethodHandlerEntry.Builder().matcher(matcher);
  }

  private SynthesizedObject.Descriptor validateDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.validator, isNotNull());
    SynthesizedObject.Descriptor ret = this.validator.apply(this, descriptor);
    ensure(ret, withMessage("Validation must not change the content of the descriptor.", allOf(
        transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
        transform(descriptorMethodHandlers()).check(isEqualTo(descriptor.methodHandlers())),
        transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject())))));
    return ret;
  }

  private SynthesizedObject.Descriptor preprocessDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.preprocessor, isNotNull());
    return ensure(this.preprocessor.apply(this, descriptor), isNotNull());
  }

  enum InternalUtils {
    ;

    static Object createProxy(ObjectSynthesizer objectSynthesizer) {
      SynthesizedObject.Descriptor descriptor = objectSynthesizer.finalizedDescriptor();
      return Proxy.newProxyInstance(
          objectSynthesizer.classLoader,
          descriptor.interfaces().toArray(new Class[0]),
          objectSynthesizer.invocationHandlerFactory.apply(objectSynthesizer));
    }

    public static List<ObjectSynthesizer.Violation> reservedMethodMisOverridings(Collection<MethodHandlerEntry> methodHandlerEntries) {
      return methodHandlerEntries
          .stream()
          .map((MethodHandlerEntry methodHandlerEntry) -> new ObjectSynthesizer.Violation(
              methodHandlerEntry,
              RESERVED_METHOD_SIGNATURES
                  .stream()
                  .filter(eachReservedMethodSignature -> methodHandlerEntry.matcher().matches(eachReservedMethodSignature))
                  .collect(Collectors.toList())))
          .filter(violation -> !violation.violatedReservedMethods.isEmpty())
          .collect(Collectors.toList());
    }

    static <V> void validateValue(V value, Predicate<V> predicate) {
      Validations.validate(
          value,
          predicate,
          s -> {
            throw new ValidationException(s);
          }
      );
    }
  }

  public static class Violation {
    final List<MethodSignature> violatedReservedMethods;
    final MethodHandlerEntry    violatingEntry;

    Violation(MethodHandlerEntry violatingEntry, List<MethodSignature> violatedReservedMethods) {
      this.violatedReservedMethods = violatedReservedMethods;
      this.violatingEntry = violatingEntry;
    }

    @Override
    public String toString() {
      return String.format("violation:entry:%s -> %s", violatingEntry, violatedReservedMethods);
    }
  }
}
