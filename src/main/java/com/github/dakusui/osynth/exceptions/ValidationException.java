package com.github.dakusui.osynth.exceptions;

/**
 * An exception class thrown when the `osynth` library's own validation fails.
 */
public class ValidationException extends OsynthException {
  public ValidationException(String message) {
    super(message);
  }
}
