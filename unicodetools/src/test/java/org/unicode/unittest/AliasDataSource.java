package org.unicode.unittest;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.unittest.LocaleCanonicalizer.AliasData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;
import com.ibm.icu.impl.locale.KeyTypeData;
import com.ibm.icu.impl.locale.KeyTypeData.ValueType;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.ULocale;

public abstract class AliasDataSource {
    public abstract List<AliasData> getData(Datatype datatype);
    public Map<String,String> generateTests() {
	String[][] tests = {
		// debug
		{"sv-aaland", "sv-AX"},

		// context independent
		{"iw", "he"},
		{"und-Qaai", "und-Zinh"},
		{"und-BU", "und-MM"},
		{"und-bbbbb-heploc-polytoni", "und-alalc97-bbbbb-polyton"},
		{"iw-Qaai-IL-polytoni", "he-Zinh-IL-polyton"},
		{"en-fonipa-heploc", "en-alalc97-fonipa"},

		{"en-840", "en-US"},
		{"cmn-SG", "zh-SG"},

		// context dependent
		// language
		{"mo", "ro"},
		{"mo-US", "ro-US"},
		{"cnr", "sr-ME"},
		{"cnr-US", "sr-US"},

		// region
		{"en-SU", "en-RU"},
		{"uz-SU", "uz-UZ"},
		{"en-172", "en-RU"},
		{"uz-172", "uz-UZ"},

		{"en-AN", "en-SX"},
		{"und-AN", "und-CW"},

		{"uz-062", "uz-143"}, 
		// TODO need to add languages of KZ, KG, TJ, TM, UZ  (Kazakhstan, Kyrgyzstan, Tajikistan, Turkmenistan, Uzbekistan)
		{"hi-062", "hi-034"},

		// variant
		{"sv-aaland", "sv-AX"},
		
		// u extensions - simple
		{"und-u-ca-islamicc", "und-u-ca-islamic-civil"},
		{"und-u-tz-aqams", "und-u-tz-nzakl"},
		{"und-u-tz-cnckg", "und-u-tz-cnsha"},
		{"und-u-tz-cnhrb", "und-u-tz-cnsha"},
		{"und-u-tz-cnkhg", "und-u-tz-cnurc"},
		{"und-u-tz-usnavajo", "und-u-tz-usden"},
		
		
	};
	Map<String,String> result = new LinkedHashMap<>();
	for (String[] test : tests) {
	    result.put(test[0],  test[1]);
	}

	Matcher lsrvMatcher = Pattern.compile("([A-Za-z]{2,3}|[A-Za-z]{5,8})"
		+ "([-_][A-Za-z]{4})?"
		+ "([-_]([A-Za-z]{2}|[0-9]{3}))?"
		+ "([-_]([0-9A-Za-z]{5,8}|[0-9][0-9A-Za-z]{3}))*").matcher("");
	Matcher scriptMatcher = Pattern.compile("[A-Za-z]{4}").matcher("");
	Matcher regionMatcher = Pattern.compile("[A-Za-z]{2}|[0-9]{3}").matcher("");
	Matcher variantMatcher = Pattern.compile("[A-Za-z0-9]{5,8}|[0-9][A-Za-z0-9]{3}").matcher("");

	Multimap<Datatype, String> failures = LinkedHashMultimap.create();

	for (Datatype dt : Arrays.asList(Datatype.language, Datatype.script, Datatype.region, 
		Datatype.variant
		// , Datatype.subdivision // do later
		)) {
	    List<AliasData> data = getData(dt);
	    for (AliasData datum : data) {
		String code = datum.aliasFrom.replace('_', '-');
		String reason = datum.reason;
		if (reason.equals("legacy")) {
		    continue;
		}
		String rawReplacement = datum.aliasTo.replace('_', '-');
		String replacement = rawReplacement.contains(" ") ? replacement = LocaleCanonicalizer.SPACE_SPLITTER.split(rawReplacement).iterator().next() : rawReplacement;
		switch(dt) {
		case language:
		    if (handleItem(dt, lsrvMatcher, datum, failures)) {
			result.put(code, replacement); 
		    }
		    break;
		case script:
		    if (handleItem(dt, scriptMatcher, datum, failures)) {
			result.put("und-" + code, "und-" + replacement);
		    }
		    break;
		case region:
		    if (handleItem(dt, regionMatcher, datum, failures)) {
			result.put("und-" + code, "und-" + replacement);
		    }
		    break;
		case variant:
		    if (handleItem(dt, variantMatcher, datum, failures)) {
			if (replacement.length() < 4) {
			    result.put("und-" + code, replacement);
			} else {
			    result.put("und-" + code, "und-" + replacement);
			}
		    }
		    break;
		case subdivision:
		    result.put("und-u-sd-" + code, "und-u-sd-" + replacement);
		    break;
		default:
		    throw new ICUException("Unexpected datatype: " + dt);
		}
	    }
	    for (String key : KeyTypeData.getBcp47Keys()) {
		// TODO get all variants of keys. Need to expand KeyTypeData for that
		ValueType valueType = KeyTypeData.getValueType(key);
		char extension = LocaleCanonicalizer.TAG2.matcher(key).matches() ? 't' : 'u'; 
		// TODO add to KeyTypeData test for whether extension is 't' or 'u' for given key
		// TODO add to KeyTypeData the ability to get all aliases, whether special
		String prefix = "und-" + extension + "-" + key;
		for (String value : KeyTypeData.getBcp47KeyTypes(key)) {
		    if (value.contentEquals("true")) {
			    result.put(prefix + "-true", prefix);
			    System.out.println(prefix + "-true" + " => " + prefix);
		    }
		    // TODO get all variants of values 
		    String altValue = KeyTypeData.toLegacyType(key, value, null, null);
		}
	    }
	}
	System.out.println("\nIllformed");
	for (Entry<Datatype, String> entry : failures.entries()) {
	    System.out.println("<" 
		    + (entry.getKey() == Datatype.region ? "territory" : entry.getKey().toString())+ "Alias"
		    + entry.getValue()
		    );
	}
	// sift out illegal input
	return ImmutableMap.copyOf(result);
    }
    
    private boolean handleItem(Datatype datatype, Matcher lsrvMatcher, AliasData datum,
	    Multimap<Datatype, String> failures) {
	if (datum.aliasFrom.contains("-"))
	if (!lsrvMatcher.reset(datum.aliasFrom).matches()) {
	    //<languageAlias type="i_ami" replacement="ami" reason="deprecated"/>

	    failures.put(datatype,
		    " type=\"" + datum.aliasFrom + "\""
		    + " replacement=\"" + datum.aliasTo + "\""
		    + " reason=\"ill_formed\""
		    + "/>"
		    );
	    return false;
	}
	return true;
    }
}