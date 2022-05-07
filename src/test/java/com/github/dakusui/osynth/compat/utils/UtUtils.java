package com.github.dakusui.osynth.compat.utils;


import com.github.dakusui.thincrest_pcond.functions.Printable;
import org.hamcrest.CoreMatchers;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Predicate;

import static org.junit.Assume.assumeThat;


public enum UtUtils {
  ;
  static final        PrintStream STDOUT = System.out;
  static final        PrintStream STDERR = System.err;
  public static final PrintStream NOP    = new PrintStream(new OutputStream() {
    @Override
    public void write(int b) {
    }
  });


  /**
   * A method to pretend to run a shell command. This just prints out given format
   * and args using the {@code String.format} method.
   * <p>
   * Note that this method automatically adds a carriage return to the end of the line.
   *
   * @param fmt  A format of the string to be printed.
   * @param args Arguments to be embedded in a format string {@code fmt}.
   */
  public static void runShell(String fmt, Object... args) {
    System.out.println(String.format(fmt, args));
  }

  /**
   * Typically called from a method annotated with {@literal @}{@code Before} method.
   */
  public static void suppressStdOutErrIfRunUnderSurefireOrPitest() {
    if (isRunUnderSurefire() || isRunUnderPitest()) {
      System.setOut(NOP);
      System.setErr(NOP);
    }
  }

  private static boolean isRunUnderPitest() {
    return Objects.equals(System.getProperty("underpitest", "no"), "yes");
  }

  /**
   * Typically called from a method annotated with {@literal @}{@code After} method.
   */
  public static void restoreStdOutErr() {
    System.setOut(STDOUT);
    System.setErr(STDERR);
  }

  public static boolean isRunUnderSurefire() {
    return System.getProperty("surefire.real.class.path") != null;
  }

  /**
   * Call this method from a test which is known to be passing under normal condition
   * (i.e. under normal maven's surefire or under your IDE) but not under
   * pitest.
   */
  public static void assumeThatNotUnderPitest() {
    assumeThat(
        isRunUnderPitest(),
        CoreMatchers.equalTo(false)
    );
  }

  public static Throwable rootCause(Throwable t) {
    if (t.getCause() == null)
      return t;
    return rootCause(t.getCause());
  }

  public static Predicate<String> nonEmptyString() {
    return Printable.predicate("nonEmptyString", v -> v.length() > 0);
  }
}
