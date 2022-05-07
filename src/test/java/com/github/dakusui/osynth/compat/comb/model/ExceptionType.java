package com.github.dakusui.osynth.compat.comb.model;

import com.github.dakusui.osynth.comb.def.*;
import com.github.dakusui.osynth.compat.comb.def.*;
import com.github.dakusui.osynth.ut.compat.comb.def.*;
import com.github.dakusui.osynth.ut.compat.osynth.comb.def.*;
import com.github.dakusui.osynth.ut.osynth.comb.def.*;

public enum ExceptionType {
  RUNTIME_EXCEPTION {
    @Override
    Throwable createException(String meesage) {
      throw new IntentionalRuntimeException(meesage);
    }

    @Override
    public Class<?>[] createInterfaces() {
      return new Class[]{ I1RuntimeException.class, I2RuntimeException.class};
    }
  },
  ERROR {
    @Override
    Throwable createException(String message) {
      throw new IntentionalError(message);
    }

    @Override
    public Class<?>[] createInterfaces() {
      return new Class[]{ I1Error.class, I2Error.class};
    }
  },
  CHECKED_EXCEPTION {
    @Override
    Throwable createException(String message) throws Throwable {
      throw new IntentionalCheckedException(message);
    }

    @Override
    public Class<?>[] createInterfaces() {
      return new Class[]{ I1CheckedException.class, I2CheckedException.class};
    }
  },
  NONE {
    @Override
    Throwable createException(String message) {
      throw new RuntimeException();
    }

    @Override
    public Class<?>[] createInterfaces() {
      return new Class[]{ I1N.class, I2N.class};
    }
  };

  abstract Throwable createException(String message) throws Throwable;

  public abstract Class<?>[] createInterfaces();

  public static class IntentionalRuntimeException extends RuntimeException {
    public IntentionalRuntimeException(String message) {
      super(message);
    }
  }

  public static class IntentionalError extends Error {
    public IntentionalError(String message) {
      super(message);
    }
  }

  public static class IntentionalCheckedException extends Exception {
    public IntentionalCheckedException(String message) {
      super(message);
    }
  }
}
