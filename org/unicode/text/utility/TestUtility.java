/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/TestUtility.java,v $
 *
 *******************************************************************************
 */

package org.unicode.text.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.unicode.cldr.util.Counter;
import org.unicode.text.UCD.Default;
import org.unicode.text.UCD.UCD_Types;

import com.ibm.icu.dev.util.DataInputCompressor;
import com.ibm.icu.dev.util.DataOutputCompressor;
import com.ibm.icu.dev.util.ICUPropertyFactory;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeMapIterator;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class TestUtility {
	/*
	 static public class MyEnum extends EnumBase {
	 public static MyEnum
	 ZEROED = (MyEnum) makeNext(myEnum.getClass()),
	 SHIFTED = (MyEnum) makeNext(),
	 NON_IGNORABLE = (MyEnum) makeNext(),

	 FIRST_ENUM = ZEROED,
	 LAST_ENUM = NON_IGNORABLE;
	 public MyEnum next(int value) {
	 return (MyEnum) internalNext(value);
	 }
	 protected MyEnum() {}
	 }
	 */
	static final boolean USE_FILE = true;
	static final boolean DEBUG = false;

	static public void main(String[] args) throws Exception {
		tryFileUnicodeProperty();
		check();
		final int iterations = 1;
		//testStreamCompressor();
		UnicodeMap umap = new UnicodeMap();
		umap.put(0,"abcdefg");
		if (false) {
			for (int i = 0; i < 256; ++i) {
				umap.put(i, String.valueOf(i&0xF0));
			}
		}
		int total = testUnicodeMapSerialization(1, iterations, "dummy", umap);

		//if (true) return;
		//UnicodeLabel ul;

		final ICUPropertyFactory p = ICUPropertyFactory.make();
		total = 0;
		final BreakIterator bk = BreakIterator.getWordInstance(Locale.ENGLISH);
		final Matcher nameMatch = Pattern.compile("Name").matcher("");

		final UnicodeProperty gc = p.getProperty("General_Category");
		final UnicodeSet checkSet = gc.getSet("Cn").addAll(gc.getSet("Co")).addAll(gc.getSet("Cs")).complement();
		final UnicodeSetIterator checkSetIterator = new UnicodeSetIterator(checkSet);
		final UnicodeProperty hangulSyllableType = p.getProperty("Hangul_Syllable_Type");
		final UnicodeSet hangulSyllable = hangulSyllableType.getSet("LVT_Syllable").addAll(hangulSyllableType.getSet("LV_Syllable"));


		for (final Iterator pnames = p.getAvailableNames().iterator(); pnames
				.hasNext();) {
			final String pname = (String) pnames.next();
			if (!nameMatch.reset(pname).matches()) {
				continue;
			}
			System.out.println();
			final UnicodeProperty up = p.getProperty(pname);
			final int ptype = up.getType();
			System.out.print("Name:\t" + pname + "\tType:\t" + UnicodeProperty.getTypeName(ptype));
			if (up.isType(UnicodeProperty.STRING_MASK)) {
				final boolean excludeHangul = pname.startsWith("isNF");
				umap = new UnicodeMap();
				checkSetIterator.reset();
				while (checkSetIterator.next()) {
					final int i = checkSetIterator.codepoint;
					if (excludeHangul && hangulSyllable.contains(i)) {
						continue;
					}
					final String value = up.getValue(i);
					if (equals(i, value)) {
						continue;
					}
					umap.put(i, value);
					//System.out.println("Adding " + Utility.hex(i) + ", " + Utility.hex(value));
				}
			} else {
				final UnicodeProperty sampleProp = p.getProperty(pname);
				umap = sampleProp.getUnicodeMap();
				if (pname.equals("Name")) {
					umap = fixNameMap(bk, umap);
				}
			}
			total = testUnicodeMapSerialization(iterations, total, pname, umap);
		}
		final String[] hanProps = {"kIICore", "kRSUnicode"};
		for (final String hanProp : hanProps) {
			final String pname = hanProp;
			if (!nameMatch.reset(pname).matches()) {
				continue;
			}
			testHanProp(iterations, total, pname, "Han");
		}

		System.out.println();
		System.out.println("Done");
	}


	static void check() throws IOException, ClassNotFoundException {
		final UnicodeMap m = new UnicodeMap();
		m.put(1,"abc");
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeBoolean(true);
		oos.writeUTF("abcdefg");
		oos.writeObject(m);
		oos.close();

		final int size = out.size();
		final byte[] buffer = out.toByteArray();
		System.out.println(showBuffer(buffer, size));

		final InputStream in = new ByteArrayInputStream(buffer, 0, size);
		final ObjectInputStream ois = new ObjectInputStream(in);
		System.out.println(ois.readBoolean());
		System.out.println(ois.readUTF());
		System.out.println(ois.readObject());
		ois.close();
	}

	/**
	 * 
	 */
	private static boolean equals(int i, String value) {
		final int len = value.length();
		if (len < 0 || len > 2) {
			return false;
		}
		if (len == 1) {
			return i == value.charAt(0);
		}
		if (i <= 0xFFFF) {
			return false;
		}
		return i == UTF16.charAt(value,0);
	}

	/**
	 * 
	 */
	private static void testHanProp(int iterations, int total, String pname, String type) throws IOException, ClassNotFoundException {
		System.out.println();
		final UnicodeMap umap = Default.ucd().getHanValue(pname);
		System.out.println(umap);
		umap.setMissing("na");
		System.out.print("Name:\t" + pname + "\tType:\t" + type);
		total = testUnicodeMapSerialization(iterations, total, pname, umap);
	}

	static String outdircore = UCD_Types.GEN_DIR + "UCD_Data/";
	static String outdir = outdircore + "4.1.0/";
	/**
	 * @param pname
	 * 
	 */
	private static int testUnicodeMapSerialization(int iterations, int total, String pname, UnicodeMap umap) throws IOException, ClassNotFoundException {
		System.out.print("\tValue Count:\t" + umap.getAvailableValues().size());

		final String filename = outdir + pname + ".bin";
		OutputStream out;
		ByteArrayOutputStream baout = null;
		if (USE_FILE) {
			out = new FileOutputStream(filename);
		} else {
			out = baout = new ByteArrayOutputStream();
		}
		out = new GZIPOutputStream(out);
		final ObjectOutputStream oos = new ObjectOutputStream(out);
		//Random rand = new Random();

		/*		if (false) {
			oos.writeObject(umap);
			oos.close();
			buffer = baout.toByteArray();
			in = new ByteArrayInputStream(buffer, 0, baout.size());
			ois = new ObjectInputStream(in);
			reverseMap = (UnicodeMap) ois.readObject();
		}
		 */
		//      UnicodeMap.StreamCompressor sc = new UnicodeMap.StreamCompressor();
		//      int test = (int)Math.abs(rand.nextGaussian()*100000);
		//      System.out.print(Integer.toString(test, 16).toUpperCase());
		//      sc.writeInt(out, test);
		//      out.close();
		//oos.writeBoolean(true);
		//oos.writeUTF("abcdefg");
		oos.writeObject(umap);
		oos.close();


		long size;
		byte[] buffer;
		if (USE_FILE) {
			size = new File(filename).length();
		} else {
			size = baout.size();
			buffer = baout.toByteArray();
			if (DEBUG) {
				System.out.println(showBuffer(buffer, size));
			}
		}
		System.out.print("\t"+"Size:\t" + size);


		// only measure read time
		UnicodeMap reverseMap = null;
		final long start = System.currentTimeMillis();
		for (int i = iterations; i > 0; --i) {
			InputStream in;
			if (USE_FILE) {
				in = new FileInputStream(filename);
			} else {
				in = new ByteArrayInputStream(buffer, 0, (int)size);
			}
			in = new GZIPInputStream(in);
			//            int x = sc.readInt(in);
			//            if (x != test) System.out.println("Failure");
			//            System.out.println("\t=> " + Integer.toString(x, 16).toUpperCase());
			final ObjectInputStream ois = new ObjectInputStream(in);
			//System.out.println(ois.readBoolean());
			//System.out.println(ois.readUTF());

			try {
				reverseMap = (UnicodeMap) ois.readObject();
			} catch (final java.io.OptionalDataException e1) {
				System.out.println(e1.eof + "\t" + e1.length);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			ois.close();
		}
		final long end = System.currentTimeMillis();

		if (!reverseMap.equals(umap)) {
			System.out.println("Failed roundtrip");
			for (int i = 0; i <= 0x10FFFF; ++i) {
				final String main = (String) umap.getValue(i);
				final String rev = (String) reverseMap.getValue(i);
				if (UnicodeMap.areEqual(main, rev)) {
					continue;
				}
				System.out.println(Utility.hex(i) + "\t'" + main + "',\t'"
						+ rev + "'");
			}
		}
		//out.toByteArray();
		total += size;
		System.out.print("\tTime:\t" + (end - start) / (iterations * 1.0)
				+ "\tmsecs (raw:\t" + ((end - start) / 1000.0) + "\tsecs)");
		/* with Vanilla Serialization
		 * Size: 24131
		 * Time: 1.9488 msecs (raw: 9.744 secs)
		 * With my serialization
		 * Size: 19353
		 * Time: 0.8652 msecs (raw: 4.326 secs)
		 * With my serialization, and compression of ints
		 * Size: 8602
		 * Time: 2.784 msecs (raw: 1.392 secs)
		 * With delta encoding
		 * Size: 5226
		 * Time: 1.924 msecs (raw: 0.962 secs)
		 * Name:
		 * Size: 776926
		 * Time: 180.3 msecs (raw: 1.803 secs)
		 */
		return total;
	}

	/**
	 * 
	 */
	private static String showBuffer(byte[] buffer, long size) {
		final StringBuffer result = new StringBuffer();
		for (int j = 0; j < size; ++j) {
			if (j != 0) {
				result.append(' ');
			}
			result.append(Utility.hex(buffer[j]&0xFF,2));
		}
		return result.toString();
	}

	/**
	 * 
	 */
	private static void testStreamCompressor() throws IOException {
		final Object[] tests = {
				UTF16.valueOf(0x10FFFF),"\u1234", "abc",
				new Long(-3), new Long(12345),
				new Short(Short.MAX_VALUE), new Short(Short.MIN_VALUE),
				new Integer(Integer.MAX_VALUE), new Integer(Integer.MIN_VALUE),
				new Long(Long.MIN_VALUE), new Long(Long.MAX_VALUE)};

		for (int i = 0; i < tests.length; ++i) {
			final Object source = tests[i];
			final ByteArrayOutputStream out = new ByteArrayOutputStream(100);
			final ObjectOutputStream out2 = new ObjectOutputStream(out);
			ByteArrayInputStream in;
			final ObjectInputStream ois;
			byte[] buffer;
			final DataOutputCompressor sc = new DataOutputCompressor(out2);
			long y = 0;
			if (source instanceof String) {
				sc.writeUTF((String)source);
			} else {
				y = ((Number)source).longValue();
				sc.writeLong(y);
			}
			out2.close();
			buffer = out.toByteArray();
			showBytes(buffer, out.size());
			System.out.println();
			in = new ByteArrayInputStream(buffer, 0, out.size());
			final ObjectInputStream in2 = new ObjectInputStream(in);
			final DataInputCompressor isc = new DataInputCompressor(in2);
			final boolean success = false;
			Object result;
			final boolean isString = source instanceof String;
			long x = 0;
			if (isString) {
				result = isc.readUTF();
				System.out.println(i + "\t" + source
						+ "\t" + result
						+ (source.equals(result) ? "\tSuccess" : "\tBitter Failure"));
			} else {
				x = isc.readLong();
				result = new Long(x);
				System.out.println(i + "\t" + y
						+ x
						+ "\t" + Utility.hex(y)
						+ "\t" + Utility.hex(x)
						+ (x == y ? "\tSuccess" : "\tBitter Failure"));
			}

			in2.close();
		}
	}

	/**
	 * 
	 */
	private static void showBytes(byte[] buffer, int len) {
		for (int i = 0; i < len; ++i) {
			System.out.print(Utility.hex(buffer[i]&0xFF,2) + " ");
		}
	}

	/**
	 * 
	 */
	private static UnicodeMap fixNameMap(BreakIterator bk, UnicodeMap umap) {
		final UnicodeMap temp = new UnicodeMap();
		final Counter<String> counter = new Counter<String>();
		for (int i = 0; i < 0x10FFFF; ++i) {
			String name = (String) umap.getValue(i);
			if (name == null) {
				continue;
			}
			if (name.startsWith("CJK UNIFIED IDEOGRAPH-")) {
				name = "*";
			} else if (name.startsWith("CJK COMPATIBILITY IDEOGRAPH-")) {
				name = "#";
			} else if (name.startsWith("HANGUL SYLLABLE ")) {
				name = "@";
			}
			bk.setText(name);
			int start = 0;
			while (true) {
				final int end = bk.next();
				if (end == BreakIterator.DONE) {
					break;
				}
				final String word = name.substring(start, end);
				counter.add(word, Math.max(0, word.length() - 2));
				start = end;
			}
			temp.put(i, name);
		}
		if (false) {
			final Set m = counter.getKeysetSortedByCount(true);
			int count = 0;
			int running = 0;
			for (final String key : counter) {
				final long c = counter.getCount(key);
				running += c;
				System.out.println(count++ + "\t" + c + "\t" + running
						+ "\t" + key);
			}
			for (final UnicodeMapIterator it2 = new UnicodeMapIterator(
					temp); it2.nextRange();) {
				System.out.println(Utility.hex(it2.codepoint) + "\t"
						+ Utility.hex(it2.codepointEnd) + "\t"
						+ it2.value);
			}
		}
		umap = temp;
		return umap;
	}

	/**
	 * 
	 */
	private static void tryFileUnicodeProperty() {
		final UnicodeProperty.Factory factory = FileUnicodeProperty.Factory.make("4.1.0");
		System.out.println(factory.getAvailableNames());
		UnicodeProperty prop = factory.getProperty("White_Space");
		System.out.println(prop.getUnicodeMap());
		prop = factory.getProperty("kRSUnicode");
		System.out.println();
		System.out.println(prop.getUnicodeMap());
	}

	public static class FileUnicodeProperty extends UnicodeProperty {
		private final File file;
		private final String version;
		private UnicodeMap map;

		private FileUnicodeProperty(File file, String version) {
			this.file = file;
			this.version = version;
			final String base = file.getName();
			setName(base.substring(0, base.length()-4)); // subtract .bin
		}

		public static class Factory extends UnicodeProperty.Factory {
			private Factory() {}
			public static Factory make(String version) {
				final Factory result = new Factory();
				final File f = new File(outdircore + version + "/");
				final File[] files = f.listFiles();
				for (final File file2 : files) {
					result.add(new FileUnicodeProperty(file2, version));
				}
				return result;
			}
		}

		@Override
		protected List _getAvailableValues(List result) {
			if (map == null) {
				make();
			}
			return (List) map.getAvailableValues(result);
		}

		@Override
		protected String _getVersion() {
			return version;
		}

		/* (non-Javadoc)
		 * @see com.ibm.icu.dev.test.util.UnicodeProperty#_getValue(int)
		 */
		@Override
		protected String _getValue(int codepoint) {
			if (map == null) {
				make();
			}
			return (String)map.getValue(codepoint);
		}

		/**
		 * 
		 */
		private void make() {
			try {
				final InputStream in = new FileInputStream(file.getCanonicalPath());
				final ObjectInputStream ois = new ObjectInputStream(in);
				map = (UnicodeMap) ois.readObject();
				ois.close();
			} catch (final Exception e) {
				throw (InternalError)new InternalError("Can't create property").initCause(e);
			}
		}

		@Override
		protected List _getNameAliases(List result) {
			result.add(getName());
			return result;
		}

		@Override
		protected List _getValueAliases(String valueAlias, List result) {
			return result;
		}
	}
}