/*
 * Copyright (c) 2009, 2025 Oracle and/or its affiliates and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

/*
 * $Id$
 */

package com.sun.ts.tests.el.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Enumeration;
import java.util.Hashtable;


import com.sun.ts.tests.el.common.elcontext.SimpleELContext;

import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;

import java.lang.System.Logger;

public final class ExprEval {

  private static final Logger logger = System.getLogger(ExprEval.class.getName());

  // Suppress default constructor for non-instantiability
  private ExprEval() {
  }

  public static String buildElExpr(boolean deferred, String operation) {

    String sandwich = null;
    if (operation == null)
      sandwich = "{A}";
    else if ("unary_minus".equals(operation))
      sandwich = "{-A}";
    else if ("unary_not".equals(operation))
      sandwich = "{not A}";
    else if ("unary_bang".equals(operation))
      sandwich = "{! A}";
    else if ("empty".equals(operation))
      sandwich = "{empty A}";
    else if ("conditional".equals(operation))
      sandwich = "{A " + "?" + "B" + ":" + " C}";
    else if ("null_coalescing".equals(operation))
        sandwich = "{A " + "??" + "B" + "}";
    else if ("elvis".equals(operation))
        sandwich = "{A " + "?:" + "B" + "}";
    else // binary operation
      sandwich = "{A " + operation + " B}";

    return (deferred) ? "#" + sandwich : "$" + sandwich;
  }

  /**
   * Evaluates the ValueExpression expression relative to the provided context
   * and resolverType, then returns the resulting value.
   *
   * @param exprStr
   *          - the String for the expression to be evaluated e.g. "${A + B}"
   * @param nameVals
   *          - an array of NameValuePair objects, each of which contains an
   *          expression variable and the value to which it is to be set.
   * @param expectedClass
   *          - the type of the result produced by evaluating the expression.
   * @param resolverType
   *          - The type of ELResolver to use for expression resolution.
   *
   * @return - The result of the expression evaluation.
   *
   * @throws jakarta.el.ELException
   * @throws jakarta.el.PropertyNotFoundException
   * @throws jakarta.el.PropertyNotWritableException
   */
  public static Object evaluateValueExpression(String exprStr,
      NameValuePair[] nameVals, Class<?> expectedClass, ResolverType resolverType)
      throws ELException, PropertyNotFoundException,
      PropertyNotWritableException {

    // get what we need to create the expression
    ExpressionFactory expFactory = ExpressionFactory.newInstance();
    ELContext context = (new SimpleELContext(resolverType)).getELContext();
    ELResolver resolver = context.getELResolver();
    ExprEval.cleanup();

    // set the values of variables to be used in the expression
    if (nameVals == null) {
      resolver.setValue(context, null, "A", null);
      logger.log(Logger.Level.TRACE, "setting var 'A' to value null");
      resolver.setValue(context, null, "B", null);
      logger.log(Logger.Level.TRACE, "setting var 'B' to value null");

    } else {
      for (int i = 0; i < nameVals.length; ++i) {
        String name = nameVals[i].getName();
        Object val = nameVals[i].getValue();
        logger.log(Logger.Level.TRACE, "setting var " + name + " to value " + val);
        resolver.setValue(context, null, name, val);
      }
    }

    // create the expression
    logger.log(Logger.Level.TRACE, "Creating ValueExpression");
    logger.log(Logger.Level.TRACE, "context is " + context.getClass().toString());
    logger.log(Logger.Level.TRACE, "exprStr is " + exprStr);
    logger.log(Logger.Level.TRACE, "expectedClass is " + expectedClass.toString());
    logger.log(Logger.Level.TRACE,
        "resolver is " + context.getELResolver().getClass().toString());
    ValueExpression vexp = expFactory.createValueExpression(context, exprStr,
        expectedClass);

    // now evaluate
    return vexp.getValue(context);
  }

  /**
   * Evaluates the ValueExpression expression relative to the provided context
   * and the resolverType of
   * com.sun.ts.tests.el.common.elresolver.VariableELResolver, then returns the
   * resulting value.
   *
   * @param exprStr
   *          - the String for the expression to be evaluated e.g. "${A + B}"
   * @param nameVals
   *          - an array of NameValuePair Objects, each of which contains an
   *          expression variable and the value to which it is to be set
   * @param expectedClass
   *          - the type of the result produced by evaluating the expression.
   *
   * @return - The result of the expression evaluation.
   *
   * @throws jakarta.el.ELException
   * @throws jakarta.el.PropertyNotFoundException
   * @throws jakarta.el.PropertyNotWritableException
   */
  public static Object evaluateValueExpression(String exprStr,
      NameValuePair[] nameVals, Class<?> expectedClass) throws ELException,
      PropertyNotFoundException, PropertyNotWritableException {

    return ExprEval.evaluateValueExpression(exprStr, nameVals, expectedClass,
        ResolverType.VARIABLE_ELRESOLVER);
  }

  /**
   * Used to evaluate MethodExpression()
   *
   * @param exprStr
   *          - Expression to be parsed as a MethodExpression.
   * @param params
   *          - Parameters to pass to the method, or null if no parameters.
   * @param expectedClass
   *          - Expected return type.
   * @param resolverType
   *          - ELResolver to use for expression resolution.
   *
   * @return - the result of the method invocation.
   *
   * @throws jakarta.el.ELException
   * @throws jakarta.el.PropertyNotFoundException
   * @throws jakarta.el.PropertyNotWritableException
   */
  public static Object evaluateMethodExpression(String exprStr, Class<?>[] params,
      Class<?> expectedClass, ResolverType resolverType) throws ELException,
      PropertyNotFoundException, PropertyNotWritableException {

    // get what we need to create the expression
    ExpressionFactory expFactory = ExpressionFactory.newInstance();
    ELContext context = (new SimpleELContext(resolverType)).getELContext();

    // create the expression
    MethodExpression mexp = expFactory.createMethodExpression(context, exprStr,
        expectedClass, params);

    // now evaluate it
    return mexp.invoke(context, params);
  }

  /**
   * @param exprStr
   *          - Expression to be parsed.
   * @param exprVal
   *          - The value passed to ValueExpression.setValue().
   * @param expectedClass
   *          - Expected return type.
   *
   * @return - the result of the method invocation.
   *
   * @throws jakarta.el.ELException
   * @throws jakarta.el.PropertyNotFoundException
   * @throws jakarta.el.PropertyNotWritableException
   *
   */
  public static Object evaluateCoerceValueExpression(String exprStr,
      Object exprVal, Class<?> expectedClass) throws ELException,
      PropertyNotFoundException, PropertyNotWritableException {

    // get what we need to create the expression
    ExpressionFactory expFactory = ExpressionFactory.newInstance();
    ELContext context = (new SimpleELContext(ResolverType.VARIABLE_ELRESOLVER))
        .getELContext();

    // create the expression
    ValueExpression vexp = expFactory.createValueExpression(context, exprStr,
        expectedClass);

    // set expression value
    vexp.setValue(context, exprVal);

    // grab the value for evaluation.
    return vexp.getValue(context);
  }

  /**
   * Parse a ValueExpression once, evaluate it many times, while adding Context
   * Objects to the ELContext one at a time.
   *
   * @param exprStr
   *          - Expression to be parsed.
   * @param exprVal
   *          - The value passed to ValueExpression.setValue().
   * @param expectedClass
   *          - Expected return type.
   * @param contextobj
   *          - a Hashtable of context objects in the form of key=Class,
   *          value=value.
   * @throws jakarta.el.ELException
   * @throws jakarta.el.PropertyNotFoundException
   * @throws jakarta.el.PropertyNotWritableException
   * @throws java.lang.ClassNotFoundException
   * @return - status of the evaluation.
   */
  public static boolean evaluateManyValueExpression(String exprStr,
      Object exprVal, Class<?> expectedClass, Hashtable<Class<?>,Object> contextobj)
      throws ELException, PropertyNotFoundException,
      PropertyNotWritableException, ClassNotFoundException {

    boolean result = true;

    // get what we need to create the expression
    ExpressionFactory expFactory = ExpressionFactory.newInstance();
    ELContext context = (new SimpleELContext(ResolverType.VARIABLE_ELRESOLVER))
        .getELContext();

    // create the expression
    ValueExpression vexp = expFactory.createValueExpression(context, exprStr,
        expectedClass);

    // set expression value
    vexp.setValue(context, exprVal);

    // start adding objects to the Context.
    Enumeration<Class<?>> keys = contextobj.keys();
    while (keys.hasMoreElements()) {
      Class<?> cl = keys.nextElement();
      Object value = contextobj.get(cl);

      // add the object
      context.putContext(cl, value);
      logger.log(Logger.Level.TRACE, "Adding value: " + "\"" + value + "\"" + " To Context!");

      // test the expression
      if (!(ExprEval.compareValue(exprVal, vexp.getValue(context)))) {
        logger.log(Logger.Level.ERROR, "Expression Failed! After Adding: " + "\"" + value
            + "\"" + " To Context");
        result = false;
      }
    }

    // return the status of the evaluation.
    return result;

  }

  public static boolean compareClass(Object obj, Class<?> expectedClass) {

    boolean isInstance = expectedClass.isInstance(obj);
    if (!isInstance) {
      logger.log(Logger.Level.ERROR, "Unexpected type for expression evaluation");
      logger.log(Logger.Level.ERROR, "Expected type: " + expectedClass.toString());
      logger.log(Logger.Level.ERROR, "Computed type: " + obj.getClass().toString());
    }
    return isInstance;
  }

  public static boolean compareValue(Object val, Object expectedVal) {

    if (!val.equals(expectedVal)) {
      logger.log(Logger.Level.ERROR, "Unexpected value for expression evaluation");
      logger.log(Logger.Level.ERROR, "Expected value: " + expectedVal.toString());
      logger.log(Logger.Level.ERROR, "Computed value: " + val.toString());
      return false;
    }

    return true;
  }

  public static boolean compareValue(Boolean val, Boolean expectedVal) {

    if (!val.equals(expectedVal)) {
      logger.log(Logger.Level.ERROR, "Unexpected value for expression evaluation");
      logger.log(Logger.Level.ERROR, "Expected value: " + expectedVal.toString());
      logger.log(Logger.Level.ERROR, "Computed value: " + val.toString());
      return false;
    }

    return true;
  }

  public static boolean compareValue(Double val, Float expectedVal, int eps) {

    BigDecimal a = new BigDecimal(val.doubleValue());
    BigDecimal b = new BigDecimal(expectedVal.doubleValue());

    return compareValue(a, b, eps);
  }

  public static boolean compareValue(Float val, Float expectedVal, int eps) {

    BigDecimal a = new BigDecimal(val.doubleValue());
    BigDecimal b = new BigDecimal(expectedVal.doubleValue());

    return compareValue(a, b, eps);
  }

  public static boolean compareValue(Long val, Long expectedVal, int eps) {

    BigDecimal a = new BigDecimal(val.longValue());
    BigDecimal b = new BigDecimal(expectedVal.longValue());

    return compareValue(a, b, eps);
  }

  public static boolean compareValue(BigInteger val, BigInteger expectedVal,
      int eps) {

    BigDecimal a = new BigDecimal(val);
    BigDecimal b = new BigDecimal(expectedVal);

    return compareValue(a, b, eps);
  }

  public static boolean compareValue(BigDecimal val, BigDecimal expectedVal,
      int eps) {

    MathContext mc = new MathContext(eps, RoundingMode.DOWN);
    BigDecimal a = val.abs(mc);
    BigDecimal b = expectedVal.abs(mc);

    if (!(0 == a.compareTo(b))) {
      logger.log(Logger.Level.ERROR, "Unexpected value for expression evaluation");
      logger.log(Logger.Level.ERROR, "Expected value: " + expectedVal);
      logger.log(Logger.Level.ERROR, "Computed value: " + val);
      return false;
    }

    return true;
  }

  // This method is used to remove resolvable items from the static
  // hashtable used by the SimpleELContext's VariableELResolver.
  // It is meant to be called at the end of a test.
  public static void cleanup() {
    (new SimpleELContext()).cleanup();
  }

}
