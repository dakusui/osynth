package com.github.dakusui.osynth;

import com.github.dakusui.osynth.core.*;
import com.github.dakusui.osynth.core.utils.AssertionUtils;
import com.github.dakusui.osynth.exceptions.ValidationException;
import com.github.dakusui.osynth.invocationcontrollers.MatchingBasedInvocationController;
import com.github.dakusui.osynth.invocationcontrollers.StandardInvocationController;
import com.github.dakusui.pcond.Validations;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.github.dakusui.osynth.ObjectSynthesizer.InternalUtils.reservedMethodMisOverridings;
import static com.github.dakusui.osynth.ObjectSynthesizer.InternalUtils.validateValue;
import static com.github.dakusui.osynth.annotations.BuiltInHandlerFactory.MethodHandlerFactory.createMethodHandlersForBuiltInMethods;
import static com.github.dakusui.osynth.core.MethodHandlerDecorator.filterOutPredefinedMethods;
import static com.github.dakusui.osynth.core.SynthesizedObject.RESERVED_METHOD_SIGNATURES;
import static com.github.dakusui.osynth.core.utils.AssertionUtils.*;
import static com.github.dakusui.osynth.core.utils.MessageUtils.messageForReservedMethodOverridingValidationFailure;
import static com.github.dakusui.pcond.Assertions.that;
import static com.github.dakusui.pcond.Postconditions.ensure;
import static com.github.dakusui.pcond.Preconditions.*;
import static com.github.dakusui.pcond.forms.Predicates.*;
import static com.github.dakusui.pcond.internals.InternalUtils.formatObject;

/**
 * The main entry pont of the `osynth` object synthesizer library.
 */
public class ObjectSynthesizer {
  private static final Object DEFAULT_FALLBACK_OBJECT = new Object() {
    @Override
    public String toString() {
      return "autoCreated:<" + super.toString() + ">";
    }
  };
  private final SynthesizedObject.Descriptor.Builder descriptorBuilder;
  private       Validator                                     validator;
  private       Preprocessor                                  preprocessor;
  private       ClassLoader                                   classLoader;
  private       InvocationControllerFactory                   invocationControllerFactory;
  private final AtomicReference<SynthesizedObject.Descriptor> finalizedDescriptor = new AtomicReference<>(null);

  public ObjectSynthesizer() {
    this.descriptorBuilder = new SynthesizedObject.Descriptor.Builder().fallbackObject(DEFAULT_FALLBACK_OBJECT);
    this.classLoader(this.getClass().getClassLoader())
        .handleMethodCallsWithExactSignatures()
        .validateWith(Validator.DEFAULT)
        .preprocessWith(Preprocessor.DEFAULT)
        .disableMethodHandlerDecorator();
  }

  public ObjectSynthesizer addInterface(Class<?> interfaceClass) {
    descriptorBuilder.addInterface(interfaceClass);
    return this;
  }

  public ObjectSynthesizer classLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  public ObjectSynthesizer fallbackTo(Object fallbackObject) {
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

  public ObjectSynthesizer createInvocationHandlerWith(InvocationControllerFactory factory) {
    this.invocationControllerFactory = requireNonNull(factory);
    return this;
  }

  public ObjectSynthesizer methodHandlerDecorator(MethodHandlerDecorator methodHandlerDecorator) {
    this.descriptorBuilder.methodHandlerDecorator(requireNonNull(methodHandlerDecorator));
    return this;
  }

  public ObjectSynthesizer disableMethodHandlerDecorator() {
    return this.methodHandlerDecorator(MethodHandlerDecorator.IDENTITY);
  }

  public ObjectSynthesizer enableAutoLogging() {
    return enableAutoLoggingWritingTo(System.out::println);
  }

  /**
   * Note that this method is using {@link this#defaultLogEntryPrinter(Consumer)},
   * which is not meant for production usages.
   * This method should also not be used in the production.
   *
   * @param out A consumer to which log records are sent.
   * @return This object.
   */
  public ObjectSynthesizer enableAutoLoggingWritingTo(Consumer<String> out) {
    return enableAutoLoggingWith(defaultLogEntryPrinter(out));
  }

  public ObjectSynthesizer enableAutoLoggingWith(AutoLogger autoLogger) {
    return this.methodHandlerDecorator(AutoLogger.create(autoLogger));
  }

  /**
   * Note that the {@link AutoLogger} instance returned by this method is meant
   * only for demonstrating how the feature works, not for real-production usage.
   *
   * @param out A consumer log records sent to.
   * @return A default log entry printer instance.
   */
  static AutoLogger defaultLogEntryPrinter(Consumer<String> out) {
    return entry -> {
      out.accept(InternalUtils.formatLogEntry(entry));
      if (entry.type() == AutoLogger.Entry.Type.EXCEPTION) {
        assert entry.value() instanceof Throwable;
        ((Throwable) entry.value()).printStackTrace(InternalUtils.toPrintStream(out));
      }
    };
  }

  public ObjectSynthesizer handleMethodsWithSignatureMatching() {
    return this.createInvocationHandlerWith(objectSynthesizer -> new MatchingBasedInvocationController(objectSynthesizer.finalizedDescriptor()));
  }

  public ObjectSynthesizer handleMethodCallsWithExactSignatures() {
    return this.createInvocationHandlerWith(objectSynthesizer -> new StandardInvocationController(objectSynthesizer.finalizedDescriptor()));
  }

  public SynthesizedObject synthesize(Object fallbackObject) {
    return this.fallbackTo(fallbackObject).synthesize();
  }

  public SynthesizedObject synthesize() {
    finalizeDescriptor(
        preprocessDescriptor(
            validateDescriptor(
                this.descriptorBuilder.methodHandlerDecorator(
                        filterOutPredefinedMethods(this.descriptorBuilder.methodHandlerDecorator()))
                    .build())));
    return (SynthesizedObject) InternalUtils.createProxy(this);
  }

  public Preprocessor preprocessor() {
    return this.preprocessor;
  }

  public Validator validator() {
    return this.validator;
  }

  public MethodHandlerDecorator methodHandlerDecorator() {
    return this.descriptorBuilder.methodHandlerDecorator();
  }

  public SynthesizedObject.Descriptor finalizedDescriptor() {
    return Optional.ofNullable(finalizedDescriptor.get())
        .orElseThrow(IllegalAccessError::new);
  }

  public boolean isDescriptorFinalized() {
    return finalizedDescriptor.get() != null;
  }

  public static MethodHandlerEntry.Builder methodCall(String methodName, Class<?>... parameterTypes) {
    return methodCall(MethodSignature.create(methodName, parameterTypes));
  }

  public static MethodHandlerEntry.Builder methodCall(MethodSignature methodRequest) {
    return new MethodHandlerEntry.Builder().handle(methodRequest);
  }

  private void finalizeDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.isDescriptorFinalized(), isFalse());
    this.finalizedDescriptor.set(descriptor);
  }

  private SynthesizedObject.Descriptor validateDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.validator, isNotNull());
    SynthesizedObject.Descriptor ret = this.validator.apply(this, descriptor);
    ensure(ret, withMessage("Validation must not change the content of the descriptor.", allOf(
        transform(descriptorInterfaces()).check(isEqualTo(descriptor.interfaces())),
        transform(descriptorMethodHandlerEntries()).check(isEqualTo(descriptor.methodHandlerEntries())),
        transform(descriptorFallbackObject()).check(isEqualTo(descriptor.fallbackObject())))));
    return ret;
  }

  private SynthesizedObject.Descriptor preprocessDescriptor(SynthesizedObject.Descriptor descriptor) {
    requireState(this.preprocessor, isNotNull());
    return ensure(this.preprocessor.apply(this, descriptor), isNotNull());
  }

  interface Stage extends BiFunction<ObjectSynthesizer, SynthesizedObject.Descriptor, SynthesizedObject.Descriptor> {
  }

  interface Validator extends Stage {
    Validator DEFAULT = toNamed("defaultValidator", (objectSynthesizer, descriptor) -> {
      assert that(objectSynthesizer, isNotNull());
      assert that(descriptor, isNotNull());
      validateValue(
          descriptor,
          withMessage(
              () -> messageForReservedMethodOverridingValidationFailure(reservedMethodMisOverridings(descriptor.methodHandlerEntries())),
              transform(descriptorMethodHandlerEntries()
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
              transform(descriptorMethodHandlerEntries()).check(isEqualTo(descriptor.methodHandlerEntries())),
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

  interface Preprocessor {
    Preprocessor INCLUDE_BUILTIN_METHOD_HANDLERS = toNamed("builtInMethodHandlers", ((objectSynthesizer, descriptor) -> {
      SynthesizedObject.Descriptor.Builder builder = new SynthesizedObject.Descriptor.Builder(descriptor);
      createMethodHandlersForBuiltInMethods(objectSynthesizer::finalizedDescriptor)
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


    SynthesizedObject.Descriptor apply(
        ObjectSynthesizer objectSynthesizer,
        SynthesizedObject.Descriptor descriptor);

    static Preprocessor sequence(Preprocessor... preprocessors) {
      return toNamed("preprocessorSequence:" + Arrays.toString(preprocessors), (objectSynthesizer, descriptor) -> {
        SynthesizedObject.Descriptor ret = descriptor;
        for (Preprocessor each : preprocessors) {
          ret = ensure(requireNonNull(each).apply(objectSynthesizer, ret), isNotNull());
        }
        return ret;
      });
    }

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

  enum InternalUtils {
    ;

    static Object createProxy(ObjectSynthesizer objectSynthesizer) {
      SynthesizedObject.Descriptor descriptor = objectSynthesizer.finalizedDescriptor();
      return Proxy.newProxyInstance(
          objectSynthesizer.classLoader,
          descriptor.interfaces().toArray(new Class[0]),
          objectSynthesizer.invocationControllerFactory.apply(objectSynthesizer));
    }

    public static List<Object> reservedMethodMisOverridings(Collection<MethodHandlerEntry> methodHandlerEntries) {
      return methodHandlerEntries
          .stream()
          .map((MethodHandlerEntry methodHandlerEntry) -> new ReservedMethodViolation(
              methodHandlerEntry,
              RESERVED_METHOD_SIGNATURES
                  .stream()
                  .filter((MethodSignature eachReservedMethodSignature) -> methodHandlerEntry.matcher().matches(eachReservedMethodSignature))
                  .collect(Collectors.toList())))
          .filter(reservedMethodViolation -> !reservedMethodViolation.violatedReservedMethods.isEmpty())
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

    private static String formatLogEntry(AutoLogger.Entry logEntry) {
      String valueType = logEntry.type().outputValueLabel();

      return String.format(
          "%-10s class:<%s> method:<%s> object:<%10s>  %s:<%s>",
          logEntry.type() + ":",
          logEntry.method().getDeclaringClass().getSimpleName(),
          MethodSignature.create(logEntry.method()),
          formatObject(logEntry.object(), 20),
          valueType,
          formatObject(logEntry.value(), 80));
    }

    private static PrintStream toPrintStream(Consumer<String> out) {
      return new PrintStream(new OutputStream() {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final char LINE_SEPARATOR_CHAR = String.format("%n").charAt(0);

        @Override
        public void write(int b) {
          if (b == LINE_SEPARATOR_CHAR) {
            lineBreak();
          } else {
            bos.write(b);
          }
        }

        private void lineBreak() {
          out.accept(bos.toString());
          bos.reset();
        }

        @Override
        public void close() throws IOException {
          super.close();
          lineBreak();
        }
      });
    }

    static class ReservedMethodViolation {
      final List<MethodSignature> violatedReservedMethods;
      final MethodHandlerEntry    violatingEntry;

      ReservedMethodViolation(MethodHandlerEntry violatingEntry, List<MethodSignature> violatedReservedMethods) {
        this.violatedReservedMethods = violatedReservedMethods;
        this.violatingEntry = violatingEntry;
      }

      @Override
      public String toString() {
        return String.format("violation:entry:%s -> %s", violatingEntry, violatedReservedMethods);
      }
    }
  }
}
