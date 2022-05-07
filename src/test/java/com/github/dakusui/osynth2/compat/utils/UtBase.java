package com.github.dakusui.osynth2.compat.utils;

import org.junit.After;
import org.junit.Before;

/**
 * A base class for all unit tests of this project.
 * It suppresses stdout and stderr before executing every test and restore it
 * after it finishes if it is executed under surefire.
 *
 * This is useful because it allows to print data to stdout/stderr when you run a
 * test under your IDE and to understand what a test is doing through the output.
 */
public abstract class UtBase {
  @Before
  public void before() {
    UtUtils.suppressStdOutErrIfRunUnderSurefireOrPitest();
  }

  @After
  public void after() {
    UtUtils.restoreStdOutErr();
  }
}