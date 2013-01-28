package org.unicode.text.utility;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.icu.dev.util.BagFormatter;

public class CallArgs {
	static BagFormatter bf = new BagFormatter();

	public static String getPrefix(Class c) {
		final String prefix = c.getName();
		final int pos = prefix.lastIndexOf('.');
		if (pos < 0) {
			return "";
		}
		return prefix.substring(0,pos+1);
	}

	public static void call(String[] args, String prefix) throws Exception {

		for (final String arg2 : args) {
			String arg = arg2;
			if (arg.startsWith("#"))
			{
				break; // comments out rest of line
			}
			String[] methodArgs = null;
			final int par = arg.indexOf('(');
			if (par >= 0) {
				methodArgs = Utility.split(arg.substring(par+1, arg.length()-1),',');
				arg = arg.substring(0,par);
			}
			final int pos = arg.indexOf('.');
			Method method = null;
			String className = "Main";
			String methodName = "";

			if (pos >= 0) {
				className = prefix + arg.substring(0,pos);
				methodName = arg.substring(pos+1);
				method = tryMethod(className, methodName, methodArgs);
			} else {
				method = tryMethod(className, arg, methodArgs);
				if (method == null) {
					className = arg;
					methodName = "main";
					method = tryMethod(className, methodName, methodArgs);
				}
			}
			if (method == null) {
				throw new IllegalArgumentException("Bad parameter: " + className + ", " + methodName);
			}
			System.out.println(method.getName() + "\t" + bf.join(methodArgs));
			method.invoke(null,methodArgs);
		}
	}
	private static Method tryMethod(String className, String methodName, String[] methodArgs)
			throws IllegalAccessException, InvocationTargetException {
		try {
			final Class foo = Class.forName(className);
			Class[] parameterTypes = null;
			if (methodArgs != null) {
				parameterTypes = new Class[methodArgs.length];
				for (int i = 0; i < methodArgs.length; ++i) {
					parameterTypes[i] = String.class;
				}
			}
			return foo.getDeclaredMethod(methodName,parameterTypes);
		} catch (final Exception e) {
			return null;
		}
	}
}