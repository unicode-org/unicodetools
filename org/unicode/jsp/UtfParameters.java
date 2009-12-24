/**
 * 
 */
package org.unicode.jsp;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ibm.icu.text.UnicodeSet;

public class UtfParameters implements Iterable<String> {

  private Map<String,String> map = new LinkedHashMap<String,String>();

  public UtfParameters(String query) {
    if (query != null) {
      String[] queries = query.split("&");
      for (String s : queries) {
        int pos = s.indexOf('=');
        String key = pos == -1 ? s : s.substring(0,pos);
        try {
          key = URLDecoder.decode(key, "UTF-8");
        } catch (Exception e) {}
        String value = pos == -1 ? "" : s.substring(pos+1);
        try {
          value = URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {}
        map.put(key, value);
      }
    }
    map = Collections.unmodifiableMap(map);
  }
  public String getParameter(String key) {
    return map.get(key);
  }
  public String getParameter(String key, String nullReplacement) {
    String result = map.get(key);
    if (result == null) {
      return nullReplacement;
    }
    return result;
  }
  public String getParameter(String key, String nullReplacement, String emptyReplacement) {
    String result = map.get(key);
    if (result == null) {
      return nullReplacement;
    }
    if (result.length() == 0) {
      return emptyReplacement;
    }
    return result;
  }
  public Iterator<String> iterator() {
    return map.keySet().iterator();
  }

  private static UnicodeSet okByte = new UnicodeSet("[A-Za-z0-9]");

  public static String fixQuery(String input) {
    try {
      StringBuilder result = new StringBuilder();
      byte[] bytes = input.getBytes("utf-8");
      for (int i = 0; i < bytes.length; ++i) {
        int ch = bytes[i] & 0xFF;
        if (okByte.contains(ch)) {
          result.append((char)ch);
        } else {
          result.append('%');
          String hex = Integer.toHexString(ch);
          if (hex.length() == 1) {
            result.append('0');
          }
          result.append(hex);
        }
      }
      return result.toString();
    } catch (UnsupportedEncodingException e) {
      return null;
    }
  }
}