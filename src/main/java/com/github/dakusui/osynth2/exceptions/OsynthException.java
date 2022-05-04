package com.github.dakusui.osynth2.exceptions;

public class OsynthException extends RuntimeException {
  public OsynthException(String message) {
    super(message);
  }

  public OsynthException(String message, Throwable cause) {
    super(message, cause);
  }
}
