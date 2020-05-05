package org.unicode.unittest;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.text.utility.Settings;
import org.unicode.text.utility.Utility;

import com.ibm.icu.dev.test.TestFmwk;

public class TestSettings extends TestFmwk{
	
	static final Set<String> Settings_to_skip = new HashSet(Arrays.asList(
		"latestVersion", 
		"lastVersion"
	));
	
	static final Set<String> CLDRPaths_to_skip = new HashSet(Arrays.asList(
			"COMPARE_PROGRAM", 
			"UNICODE_VERSION"
		));

	public static void main(String[] args) {
		new TestSettings().run(args);
	}
	
	public void TestDirs() throws IllegalArgumentException, IllegalAccessException {
		checkDirs(Settings.class, Settings_to_skip);
		checkDirs(CLDRPaths.class, CLDRPaths_to_skip);
//		for (String dir : TO_TEST) {
//			File file = new File(dir);
//			//assertTrue(++i + ") " + dir + " exists", file.exists());
//			assertTrue(++i + ") " + dir + " is dir", file.isDirectory());
//		}
	}

	private void checkDirs(Class<?> class1, Set<String> skips) throws IllegalAccessException {
		String[] fullClassNames = class1.getName().split("\\.");
		String className = fullClassNames[fullClassNames.length-1];
		for (Field field : class1.getDeclaredFields()) {
		    int modifiers = field.getModifiers();
			if (java.lang.reflect.Modifier.isStatic(modifiers)
		    		&& java.lang.reflect.Modifier.isPublic(modifiers)) {
				String name = field.getName();
				if (skips.contains(name)) {
					continue;
				}
		    	Class<?> fieldClass = field.getType();
		    	if (fieldClass != String.class) {
		    		continue;
		    	}
		    	Object fieldGet = field.get(null);
		    	if (fieldGet == null) {
		    		errln(className + "." + name + " is null");
		    		continue;
		    	}
				String dir = fieldGet.toString();
				File file = new File(dir);
				assertTrue(className + "." + name + ", " + dir + " is dir", file.isDirectory());
		    }
		}
	}
}
