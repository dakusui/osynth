package com.github.dakusui.osynth.comb.model;

public enum ExceptionType {
  RUNTIME_EXCEPTION {
    @Override
    Throwable create(String meesage) {
      throw new IntentionalRuntimeException(meesage);
    }
  },
  ERROR {
    @Override
    Throwable create(String message) {
      throw new IntentionalError(message);
    }
  },
  CHECKED_EXCEPTION {
    @Override
    Throwable create(String message) throws Throwable {
      throw new IntentionalCheckedException(message);
    }
  },
  NONE {
    @Override
    Throwable create(String message) {
      throw new RuntimeException();
    }
  };

  abstract Throwable create(String message) throws Throwable;

  public static class IntentionalRuntimeException extends RuntimeException {
    public IntentionalRuntimeException(String message) {
      super(message);
    }
  }

  public static class IntentionalError extends Error {
    IntentionalError(String message) {
      super(message);
    }
  }

  public static class IntentionalCheckedException extends Exception {
    IntentionalCheckedException(String message) {
      super(message);
    }
  }
}
