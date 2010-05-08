package org.unicode.draft;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.tool.GenerateLikelySubtagTests;
import org.unicode.cldr.tool.ConvertLanguageData.InverseComparator;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.impl.Row;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;


public class LanguageDetectionVsTags {
  private static final EnumSet<LineFormat> tag_range = EnumSet.range(LineFormat.http, LineFormat.xmllang);

  //  http  meta  html-lang xml-lang  detected  count navboost  pagerang  lang  encod
  enum LineFormat {L1, http, meta, lang, xmllang, detected, occurrences, documents, navboost, pagerank, lang2, enc, url};
  
  public static void main(String[] args) throws IOException {
    BufferedReader in = BagFormatter.openUTF8Reader("/Users/markdavis/Documents/Data/", "lang78.txt");
    Map<String,Counter<String>> detectedToCountAndTag = new TreeMap<String,Counter<String>>();
    Counter<String> detectedToCount = new Counter<String>();
    Counter<String> taggedToCount = new Counter<String>();
    Set<String> tagSet = new HashSet<String>();
    Counter<Integer> tagCount2 = new Counter<Integer>();
    while (true) {
      String line = in.readLine();
      if (line == null) break;
      String[] parts = line.split("\t");
      // L1 html lang detected occurrences documents navboost pagerank lang enc url
      // L1 html  en  en  110977145 110867809 48729828  60774 0 22  http://www.facebook.com/  27961428  60188 0 0 http://www.mapquest.com/  10651203  58229 0 22  http://free.grisoft.com/
      try {
        String googleID = fixID(parts[LineFormat.detected.ordinal()]);
        long count = Long.parseLong(parts[LineFormat.occurrences.ordinal()]);
        detectedToCount.add(googleID, count);
        tagSet.clear();
        for (LineFormat item : tag_range) {
          String tag = fixID(parts[item.ordinal()]);
          if (!tag.equals("---")) {
            tagSet.add(tag);
          }
        }
        int tagSetSize = tagSet.size();
        if (tagSetSize == 1) {
          taggedToCount.add(tagSet.iterator().next(), count);
        }
        if (!tagSet.contains(googleID)) {
          tagSetSize = -tagSetSize;
        }
        tagCount2.add(tagSetSize, count);
        if (true) {
          Counter<String> counter = detectedToCountAndTag.get(googleID);
          if (counter == null) {
            detectedToCountAndTag.put(googleID, counter = new Counter<String>());
          }
          final String tags = parts[LineFormat.lang.ordinal()];
          String tag = tags.split("\\s*,\\s*")[0];
          tag = tag.replace("_", "-");
          counter.add(tag, Long.parseLong(parts[LineFormat.documents.ordinal()]));
        }
      } catch (NumberFormatException e) {
        System.err.println("Failure at: " + line);
        break;
      }
    }
    in.close();
    for (Integer i : tagCount2) {
      System.out.println(i + "\t" + tagCount2.getCount(i));
    }
    for (String detected : detectedToCount.getKeysetSortedByCount(false)) {
      System.out.println(getLanguageName(detected) 
              + "\t" + detectedToCount.getCount(detected) 
              + "\t" + taggedToCount.getCount(detected));
    }
    if (true) return;

    Set<Row.R3<Long, String, Counter<Type>>> countLangTypes = new TreeSet<Row.R3<Long, String, Counter<Type>>>(new InverseComparator());
    for (String lang : detectedToCountAndTag.keySet()) {
        Counter<String> counter = detectedToCountAndTag.get(lang);
        final long total = counter.getTotal();
        Counter<Type> typeCount = new Counter<Type>();
        for (String x : counter.keySet()) {
          typeCount.add(getType(lang,x), counter.getCount(x));
        }
        countLangTypes.add(new Row.R3<Long, String, Counter<Type>>(total, lang, typeCount));
    }
    
    NumberFormat nf = NumberFormat.getIntegerInstance();
    
    System.out.print("Language");
    for (Type type : Type.values()) {
      System.out.print("\t" + type);
    }
    System.out.println();
    for (Row.R3<Long, String, Counter<Type>> countAndLang : countLangTypes) {
      String lang = countAndLang.get1();
      Counter<Type> typeCount = countAndLang.get2();
      System.out.print(getLanguageName(lang));
      for (Type type : Type.values()) {
        System.out.print("\t" + ((double)typeCount.getCount(type)/typeCount.getTotal() - 0.0001));
      }
      System.out.println();
    }
    
    for (Row.R3<Long, String, Counter<Type>> countAndLang : countLangTypes) {
      String lang = countAndLang.get1();
      long total = countAndLang.get0();
      Counter<String> counter = detectedToCountAndTag.get(lang);
      System.out.println(lang + "\t" + getLanguageName(lang) + "\t" + nf.format(total));
      int count = 0;
      long remaining = total;
      List<String> tags = new ArrayList<String>();
      List<Long> counts = new ArrayList<Long>();
      for (String tagged : counter.getKeysetSortedByCount(false)) {
        long tagCount = counter.getCount(tagged);
        tags.add(tagged);
        counts.add(tagCount);
        remaining -= tagCount;
        if (count++ > 3 && remaining*1000 < total) break;
      }
      if (remaining > 0) {
        tags.add("other");
        counts.add(remaining);
      }
      if (false) {
        for (String tag : tags) {
          System.out.print("\t" + getName(tag));
        }
        System.out.println();
        for (Long tagCount : counts) {
          System.out.print("\t" + nf.format(tagCount));
        }
        System.out.println();
      } else {
        for (int i = 0; i < tags.size(); ++i) {
          final String tag = tags.get(i);
          String name = ULocale.getDisplayName(tag, ULocale.ENGLISH);
          if (name.equals(tag)) {
            name = "??";
          }
          System.out.println("\t" + getName(tag) + "\t" + name + "\t" + counts.get(i)
                  + "\t" + getType(lang, tag));
        }
      }
    }
  }
  
  static LanguageTagParser langTagParser = new LanguageTagParser();
  static SupplementalDataInfo supplementalData = SupplementalDataInfo.getInstance(CldrUtility.SUPPLEMENTAL_DIRECTORY);
  static Map<String, String> likelySubtags = supplementalData.getLikelySubtags();
  
  private static String fixID(String string) {
    string = string.toLowerCase();
    int x;
    try {
      langTagParser.set(string);
      string = langTagParser.toString();
      String minimized = GenerateLikelySubtagTests.minimize(string, likelySubtags, false);
      if (minimized != null) {
        if (minimized.equals(string)) {
          x = 1;
        } else {
          x = 0;
        }
        return minimized;
      }
      return string;
    } catch (Exception e) {
      return string;
    }
  }

  enum Type {missing, mismatch, match};
  
  static Type getType(String detected, String tagged) {
    if (tagged.equals("---")) {
      return Type.missing;
    }
    if (detected.equals(tagged)) {
      return Type.match;
    }
    String[] tags = tagged.split("-");
    if (tags.length == 0) {
      return Type.mismatch;
    }
    final String firstTag = tags[0];
    if (firstTag.equals(detected)) {
      return Type.match;
    }
    if (firstTag.equals("he") && detected.equals("iw")) {
      return Type.match;
    }
    if (firstTag.equals("zh")) {
      if (detected.equals("zh-CN") && tags.length == 1) {
        return Type.match;
      }
    }
    return Type.mismatch;
  }
  
  private static String getName(String tag) {
    if (tag.equals("---")) {
      return "none";
    }
    if (tag.equals("other")) {
      return "other";
    }
    if (tag.length() > 12) {
      tag = tag.substring(0,12) + "…";
    }
    return '"' + tag + '"';
  }
  
  static Map<String,String> remapping = new HashMap();
  static {
    remapping.put("zh-CN", "Chinese (S)");
    remapping.put("zh-TW", "Chinese (T)");
  }

  private static String getLanguageName(String lang) {
    String remap = remapping.get(lang);
    if (remap != null) {
      return remap;
    }
    return ULocale.getDisplayName(lang, ULocale.ENGLISH);
  }
}
