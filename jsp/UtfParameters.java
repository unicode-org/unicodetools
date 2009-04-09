/**
 * 
 */
package jsp;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
  public Iterator<String> iterator() {
    return map.keySet().iterator();
  }
}