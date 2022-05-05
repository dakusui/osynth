package com.github.dakusui.osynth2.core;

import java.lang.reflect.Method;

import static com.github.dakusui.pcond.Preconditions.require;
import static com.github.dakusui.pcond.forms.Predicates.*;

public interface MethodMatcher {
  default boolean matches(Method m) {
    return matches(MethodSignature.create(m));
  }

  boolean matches(MethodSignature s);

  interface Descriptor {
    interface BySignature extends Descriptor {
      String methodName();

      Class<?>[] parameterTypes();
    }
  }

  interface Factory {
    MethodMatcher create(Descriptor descriptor);

    interface FromSignature extends Factory {
      @Override
      default MethodMatcher create(Descriptor descriptor) {
        Descriptor.BySignature desc = (Descriptor.BySignature) require(descriptor,
            and(isNotNull(), isInstanceOf(Descriptor.BySignature.class)));
        return createBySignature(desc);
      }

      MethodMatcher createBySignature(Descriptor.BySignature descriptor);
    }

    interface Exact extends FromSignature {
      default MethodMatcher createBySignature(Descriptor.BySignature descriptor) {
        return new MethodSignature.Impl(descriptor.methodName(), descriptor.parameterTypes());
      }
    }

    interface Lenient extends FromSignature {
      default MethodMatcher createBySignature(Descriptor.BySignature descriptor) {
        return new MethodSignature.Impl(descriptor.methodName(), descriptor.parameterTypes());
      }
    }
  }
}
