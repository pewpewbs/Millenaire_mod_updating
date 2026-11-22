package org.millenaire.common.utilities;
public class MillLog { public static void major(Object o,String text){} public static void error(Object o,String text){throw new AssertionError(text);} }