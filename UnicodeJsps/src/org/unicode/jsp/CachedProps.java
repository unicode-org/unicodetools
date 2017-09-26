package org.unicode.jsp;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.jsp.UnicodeDataInput.ItemReader;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.util.ICUUncheckedIOException;
import com.ibm.icu.util.VersionInfo;

public class CachedProps {
    private static final boolean IS_BETA = false;

    public static final Splitter HASH_SPLITTER = Splitter.on('#').trimResults();
    public static final Splitter SEMI_SPLITTER = Splitter.on(';').trimResults();

    static ConcurrentHashMap<VersionInfo, CachedProps> versionToCachedProps = new ConcurrentHashMap();

    final VersionInfo version;
    final Set<String> propNames;
    final ConcurrentHashMap<String, UnicodeProperty> propertyCache = new ConcurrentHashMap<String, UnicodeProperty>();
    final BiMultimap<String,String> nameToAliases = new BiMultimap<String,String>(null,null);
    final Map<String,BiMultimap<String,String>> nameToValueToAliases = new LinkedHashMap();

    static CachedProps CACHED_PROPS = getInstance(VersionInfo.getInstance(10));

    static UnicodeProperty NAMES = CachedProps.CACHED_PROPS.getProperty("Name");

    private CachedProps(VersionInfo version2) {
        version = version2;
        File dir = new File(getRelativeFileName(CachedProps.class, "props"));
        LinkedHashSet<String> temp = new LinkedHashSet<String>();
        for (String filename : dir.list()) {
            if (filename.endsWith(".bin")) {
                temp.add(filename.substring(0, filename.length()-4));
            }
        }

        // scf                      ; Simple_Case_Folding         ; sfc
        for (String fileName : Arrays.asList("PropertyAliases.txt", "ExtraPropertyAliases.txt")) {
            for (String line : FileUtilities.in(CachedProps.class, "data/" + fileName)) {
                List<String> splitLine = breakLine(line);
                if (splitLine == null) {
                    continue;
                }
                String name = splitLine.get(1);
                List<String> nameAliases = new ArrayList(splitLine);
                nameAliases.remove(1);
                nameToAliases.putAll(name, nameAliases);
            }
        }
        // AHex; Y                               ; Yes                              ; T                                ; True
        // ccc;   0; NR                         ; Not_Reordered
        for (String fileName : Arrays.asList("PropertyValueAliases.txt", "ExtraPropertyValueAliases.txt")) {
            for (String line : FileUtilities.in(CachedProps.class, "data/" + fileName)) {
                List<String> splitLine = breakLine(line);
                if (splitLine == null) {
                    continue;
                }
                String pname = splitLine.get(0);
                Collection<String> names = nameToAliases.getKeys(pname);
                String longName = names.iterator().next();
                BiMultimap<String, String> valueToAliases = nameToValueToAliases.get(longName);
                if (valueToAliases == null) {
                    nameToValueToAliases.put(longName, valueToAliases = new BiMultimap<String, String>(null,null));
                }
                List<String> aliases = splitLine.subList(1, splitLine.size());
                for (String item : aliases) {
                    valueToAliases.putAll(item, aliases);
                }
            }
        }
        propNames = Collections.unmodifiableSet(temp);
    }

    private List<String> breakLine(String line) {
        Iterable<String> items = HASH_SPLITTER.split(line);
        String first = items.iterator().next();
        if (first.isEmpty()) {
            return null;
        }
        List<String> splitLine = SEMI_SPLITTER.splitToList(first);
        if (splitLine.isEmpty()) {
            return null;
        }
        return splitLine;
    }


    public static CachedProps getInstance(VersionInfo version) {
        CachedProps result = versionToCachedProps.get(version);
        if (result == null) {
            versionToCachedProps.put(version, result = new CachedProps(version));
        }
        return result;
    }

    public Set<String> getAvailable() {
        return propNames;
    }

    public UnicodeProperty getProperty(String propName) {
        UnicodeProperty result = propertyCache.get(propName);
        if (result == null) {
            if (!propNames.contains(propName)) {
                result = null;
            } else {
                try {
                    return new DelayedUnicodeProperty(version, propName, nameToAliases.getValues(propName), nameToValueToAliases.get(propName));
                } catch (Exception e) {
                    throw new IllegalArgumentException(propName, e);
                }
            }
        }
        return result;
    }

    class DelayedUnicodeProperty extends UnicodeProperty {

        private final VersionInfo version;
        private UnicodeMap<String> map;
        private List<String> nameAliases;
        private Multimap<String,String> valueToAliases;

        public DelayedUnicodeProperty(VersionInfo version, String propName, 
                Collection<String> nameAliases, 
                BiMultimap<String, String> biMultimap) {
            this.version = version;
            Collection<String> temp;
            if (IS_BETA) {
                propName = propName + "β";
                temp = new LinkedHashSet<String>();
                for (String nameAlias : nameAliases) {
                    temp.add(nameAlias + "β");
                }
            } else {
                temp = nameAliases;
            }
            this.nameAliases = ImmutableList.copyOf(temp);
            this.valueToAliases = biMultimap == null ? null : ImmutableMultimap.copyOf(biMultimap.getKeyToValues());
            setName(propName);
        }

        @Override
        protected String _getVersion() {
            return version.getVersionString(2, 2);
        }

        @Override
        protected String _getValue(int codepoint) {
            return getMap().get(codepoint);
        }

        @Override
        protected List _getNameAliases(List result) {
            result.clear();
            result.addAll(nameAliases);
            return result;
        }

        @Override
        protected List _getValueAliases(String valueAlias, List result) {
            result.clear();
            if (valueToAliases != null) {
                result.addAll(valueToAliases.get(valueAlias));
            }
            result.add(valueAlias);
            return result;
        }

        @Override
        protected List _getAvailableValues(List result) {
            result.addAll(getMap().values());
            return result;
        }

        @Override
        protected UnicodeMap _getUnicodeMap() {
            return getMap();
        }

        private UnicodeMap<String> getMap() {
            if (map == null) {
                InputStream fis = null;
                InputStream gs = null;
                DataInputStream in = null;
                map = new UnicodeMap<String>().freeze();
                try {
                    String baseName = getName();
                    if (baseName.endsWith("β")) {
                        baseName = baseName.substring(0, baseName.length()-1);
                    }
                    fis = CachedProps.class.getResourceAsStream("props/" + baseName + ".bin");
                    gs = new GZIPInputStream(fis);
                    in = new DataInputStream(gs);
                    final ItemReader<String> stringReader = new UnicodeDataInput.StringReader();
                    UnicodeMap<String> newItem;
                    final UnicodeDataInput unicodeDataInput = new UnicodeDataInput();
                    newItem = unicodeDataInput.set(in, true).readUnicodeMap(stringReader);
                    map = newItem.freeze();
                } catch (Exception e) { }
                try {
                    if (fis != null) {
                        fis.close();
                        if (gs != null) {
                            gs.close();
                            if (in != null) {
                                in.close();
                            }
                        }
                    }
                } catch (IOException e) {}
            }
            return map;
        }
    }

    public static String getRelativeFileName(Class<?> class1, String filename) {
        URL resource = class1.getResource(filename);
        String resourceString = resource.toString();
        if (resourceString.startsWith("file:")) {
            return resourceString.substring(5);
        } else if (resourceString.startsWith("jar:file:")) {
            return resourceString.substring(9);
        } else {
            throw new ICUUncheckedIOException("File not found: " + resourceString);
        }
    }

    public static void main(String[] args) {
        CachedProps cp = CachedProps.getInstance(VersionInfo.getInstance(10));
        Set<String> available = cp.getAvailable();
        System.out.println(available);
        for (String name : available) {
            UnicodeProperty p = cp.getProperty(name);
            System.out.println(p.getName() + "\t" + p.getNameAliases() + "\t" + clip(p.getAvailableValues()));
            String value = p.getValue('a');
            System.out.println("value('a'): " + value + "\t" + p.getValueAliases(value));
        }
    }


    private static String clip(Collection availableValues) {
        return availableValues.size() > 24 ? new ArrayList(availableValues).subList(0, 23) + ", …" : availableValues.toString();
    }


    public Set<String> getPropertyNames() {
        return nameToAliases.keySet();
    }
}
