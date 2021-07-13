package org.unicode.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;
import com.ibm.icu.impl.locale.KeyTypeData;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ICUException;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;


public class LocaleCanonicalizer {
    private static final boolean DEBUG = false;

    public enum ErrorChoice {
	ignoreErrors, 
	throwErrors}

    public static final Splitter SEP_SPLITTER = Splitter.on(CharMatcher.anyOf("-_"));
    public static final UnicodeSet SEP = new UnicodeSet("[-_]");
    public static final Joiner SEP_JOIN = Joiner.on('-');
    public static final Joiner SPACE_JOINER = Joiner.on(' ');
    public static final Splitter SPACE_SPLITTER = Splitter.on(' ').trimResults().omitEmptyStrings();
    public static final Splitter EQUAL_SPLITTER = Splitter.on('=').trimResults().omitEmptyStrings();
    public static final Joiner SEMI_JOINER = Joiner.on("; ");

    static final Pattern TAG2 = Pattern.compile("[a-zA-Z][0-9]");
    static final Pattern KEY_SPLITTER = Pattern.compile("(?:^|[-])(?=[a-z][0-9](?:$|[-]))");

    final AliasesFull LANGUAGE_ALIASES;
    final AliasesFull SCRIPT_ALIASES;
    final AliasesFull REGION_ALIASES;
    final AliasesFull VARIANT_ALIASES;
    private ErrorChoice throwChoice;

    public LocaleCanonicalizer(AliasDataSource aliasDataSource, ErrorChoice throwChoice) {
	// later, allocate these only once
	LANGUAGE_ALIASES = new AliasesFull(Datatype.language, aliasDataSource);
	SCRIPT_ALIASES = new AliasesFull(Datatype.script, aliasDataSource);
	REGION_ALIASES = new AliasesFull(Datatype.region, aliasDataSource);
	VARIANT_ALIASES = new AliasesFull(Datatype.variant, aliasDataSource);
	this.throwChoice = throwChoice;
    }

    public ULocale canonicalize(ULocale locale) {
	Builder builder = null; // use lazy build: it is most common to have no change
	Output<Collection<ExceptionInfo>> exceptionOutput = new Output<>();
	LocaleCanonicalizer.LSRV source = LocaleCanonicalizer.LSRV.from(locale);
	LocaleCanonicalizer.LSRV result = LocaleCanonicalizer.LSRV.from(source);
	String replacement;

	replacement = LANGUAGE_ALIASES.getCanonical(source.language, exceptionOutput);
	if (exceptionOutput.value != null) {
	    for (ExceptionInfo ex : exceptionOutput.value) {
		if (ex.resetTargetIfMatches(Datatype.language, source, result)) {
		    break;
		}
	    }
	} else if (replacement != null) {
	    result.language = replacement;
	}

	replacement = SCRIPT_ALIASES.getCanonical(source.script, exceptionOutput);
	if (exceptionOutput.value != null) {
	    for (ExceptionInfo ex : exceptionOutput.value) {
		if (ex.resetTargetIfMatches(Datatype.script, source, result)) {
		    break;
		}
	    }
	} else if (replacement != null) {
	    result.script = replacement;
	}

	replacement = REGION_ALIASES.getCanonical(source.region, exceptionOutput);
	if (exceptionOutput.value != null) {
	    for (ExceptionInfo ex : exceptionOutput.value) {
		if (ex.resetTargetIfMatches(Datatype.region, source, result)) {
		    break;
		}
	    }
	} else if (replacement != null) {
	    result.region = replacement;
	}

	// key is that we never introduce a variant that the source doesn't have
	// key is that we only replace or delete variants

	if (!source.variants.isEmpty()) {
	    result.variants.clear();
	    for (String variant : source.variants) {
		// variants have wrong case in ICU
		variant = variant.toLowerCase(Locale.ROOT);
		replacement = VARIANT_ALIASES.getCanonical(variant, exceptionOutput);
		if (exceptionOutput.value != null) {
		    for (ExceptionInfo ex : exceptionOutput.value) {
			if (ex.resetTargetIfMatches(Datatype.variant, source, result)) {
			    break;
			}
		    }
		} else if (replacement != null) {
		    if (!replacement.isEmpty()) {
			result.variants.add(replacement);
		    }
		} else {
		    result.variants.add(variant);
		}
	    }
	}

	// TODO change API so that can use regular for (kKey : locale.getKeywords())

	LocaleExtensions localeExtension = LocaleExtensions.create(locale, throwChoice);
	LocaleExtensions localeExtension2 = null;
	if (localeExtension != null) {
	    localeExtension2 = canonicalizeKeyValues(localeExtension.tKeyValues);
	}

	if (result.equals(source) && Objects.equals(localeExtension, localeExtension2)
		) {
	    return locale; // original, with extensions, etc.
	} else {
	    builder = new ULocale.Builder()
		    .setLocale(locale) // picks up extensions, etc
		    .setLanguage(result.language)
		    .setScript(result.script)
		    .setRegion(result.region)
		    .setVariant(result.variants == null ? "" : SEP_JOIN.join(result.variants));
	    if (localeExtension2 != null) {
		localeExtension2.set(builder);
	    }
	    return builder.build();
	}
    }

    public LocaleExtensions canonicalizeKeyValues(Map<String, String> tKeyValues) {
	Map<String, String> result = new LinkedHashMap<>(tKeyValues);
	// note: could avoid some allocations, but do it the simple way.
	for (Entry<String, String> entry : tKeyValues.entrySet()) {
	    String newKey = KeyTypeData.toBcpKey(entry.getKey());
	    // do each one separately
	    List<String> newValues = new ArrayList<>();
	    Output<Boolean> isSpecialType = new Output<>();
	    Output<Boolean> isKnownKey = new Output<>();
	    String entryValue = entry.getValue();
	    String newValue = KeyTypeData.toBcpType(entry.getKey(), entryValue, isKnownKey, isSpecialType);
	    String replacementValue = null;
	    if (newValue != null) {
		if ("true".equals(newValue)) {
		    newValue = "";
		}
		replacementValue = newValue;
	    } else if (SEP.containsSome(entryValue)){
		for (String tValue : SEP_SPLITTER.splitToList(entryValue)) {
		    // TODO fix toBcpType to canonicalize even if isSpecialType is null
		    newValue = KeyTypeData.toBcpType(entry.getKey(), tValue, isKnownKey, isSpecialType);
		    newValue = newValue == null ? tValue : newValue;
		    if ("true".equals(newValue)) {
			newValue = "";
		    }
		    newValues.add(newValue);
		}
		replacementValue = SEP_JOIN.join(newValues);
	    }
	    result.put(newKey == null ? entry.getKey() : newKey, replacementValue);
	}
	return LocaleExtensions.create(result);
    }

    // can optimize by using same logic as canonicalize, 
    // but testing and returning false immediately instead of making change
    public boolean isCanonicalized(ULocale locale) {
	return locale.equals(canonicalize(locale));
    }

    // can optimize by using same logic as canonicalize, operating in parallel
    // but testing and returning false immediately instead of making change
    public boolean areCanonicalizedEqual(ULocale locale1, ULocale locale2) {
	return canonicalize(locale1).equals(canonicalize(locale2));
    }

    public static class LSRV {
	String language;
	String script;
	String region;
	Set<String> variants;

	public static LSRV from(ULocale locale) {
	    LSRV result = new LSRV();
	    result.language = locale.getLanguage();
	    result.script = locale.getScript();
	    result.region = locale.getCountry();
	    String variantString = locale.getVariant();
	    result.variants = variantString.isEmpty() ? new TreeSet<>() 
		    : new TreeSet<>(TestLocaleConstruction.SEP_SPLITTER.splitToList(variantString));
	    return result;
	}
	public static LSRV from(LSRV locale) {
	    LSRV result = new LSRV();
	    result.language = locale.language;
	    result.script = locale.script;
	    result.region = locale.region;
	    result.variants = new TreeSet<>(locale.variants);
	    return result;
	}
	@Override
	public boolean equals(Object obj) {
	    LSRV other = (LSRV) obj; // private class, don't worry about exception
	    return language.equals(other.language)
		    && script.equals(other.script)
		    && region.equals(other.region)
		    && variants.equals(other.variants);
	}
	@Override
	public int hashCode() {
	    return Objects.hash(language, script, region, variants);
	}
	@Override
	public String toString() {
	    return Arrays.asList(language, script, region, variants).toString();
	}
    }

    public static class AliasData {
	final String aliasFrom;
	final String reason;
	final String aliasTo;

	public AliasData(String aliasFrom, String reason, String aliasTo) {
	    this.aliasFrom = aliasFrom.replace('_', '-');
	    this.reason = reason;
	    this.aliasTo = aliasTo.replace('_', '-');
	}
    }

    public static final class ExceptionInfo {
	final String cLang;
	final String cRegion;
	final String cVariant;
	final String tLang;
	final String tRegion;
	final String tVariant;
	final String reason;
	public ExceptionInfo(Datatype datatype, ULocale context, ULocale aliasTo, String reason) {
	    this.reason = reason;
	    if (context == null) {
		cLang = cRegion = cVariant = null;
	    } else {
		String _cLang = context.getLanguage();
		String _cRegion = context.getCountry();
		String _cVariant = context.getVariant();

		cLang = datatype == Datatype.language || _cLang.isEmpty() || _cLang.equals("und") ? null 
			: _cLang;
		cRegion = datatype == Datatype.region || _cRegion.isEmpty() ? null 
			: _cRegion;
		cVariant = datatype == Datatype.variant || _cVariant.isEmpty() ? null 
			: _cVariant;
	    }
	    String _tLang = aliasTo.getLanguage();
	    String _tRegion = aliasTo.getCountry();
	    String _tVariant = aliasTo.getVariant();

	    tLang = datatype == Datatype.language ? _tLang 
		    : _tLang.isEmpty() || _tLang.equals("und") ? null 
			    : _tLang;
	    tRegion = datatype == Datatype.region ? _tRegion 
		    : _tRegion.isEmpty() ? null 
			    : _tRegion;
	    tVariant = datatype == Datatype.variant ? _tVariant 
		    : _tVariant.isEmpty() ? null 
			    : _tVariant;
	}
	/* Set the other fields of the result, and return the new datatype value */
	public void setTarget(Datatype datatype, LocaleCanonicalizer.LSRV result) {
	    if (tLang != null && (result.language.isEmpty() || datatype == Datatype.language)) {
		result.language = tLang;
	    }
	    if (tRegion != null && (result.region.isEmpty() || datatype == Datatype.region)) {
		result.region = tRegion;
	    }
	    if (tVariant != null && (result.variants.isEmpty() || datatype == Datatype.variant)) {
		result.variants.add(tVariant);
	    }
	}
	public boolean matches(Datatype datatype, LocaleCanonicalizer.LSRV source) {
	    return 
		    (datatype == Datatype.language || cLang == null || cLang.equals(source.language))
		    && (datatype == Datatype.region || cRegion == null || cRegion.equals(source.region))
		    && (datatype == Datatype.variant || cVariant == null || source.variants.contains(cVariant))
		    ;
	}

	public boolean resetTargetIfMatches(Datatype datatype, LocaleCanonicalizer.LSRV source, LocaleCanonicalizer.LSRV result) {
	    if (matches(datatype, source)) {
		setTarget(datatype, result);
		return true;
	    } else {
		return false;
	    }
	}


	@Override
	public String toString() {
	    return 
		    "["
		    + (cLang == null ? "" : "cLang=" + cLang)
		    + (cRegion == null ? "" : " cRegion=" + cRegion)
		    + (cVariant == null ? "" : " cVariant=" + cVariant)
		    + (tLang == null ? "" : " tLang=" + tLang)
		    + (tRegion == null ? "" : " tRegion=" + tRegion)
		    + (tVariant == null ? "" : " tVariant=" + tVariant)
		    + " ]";
	}
	public String toXml(Datatype datatype) {
	    StringBuilder result = new StringBuilder();
	    if (cLang != null) {
		result.append(" cLang=\"").append(cLang).append('"');
	    }
	    if (cRegion != null) {
		result.append(" cRegion=\"").append(cRegion).append('"');
	    }
	    if (cVariant != null) {
		result.append(" cVariant=\"").append(cVariant).append('"');
	    }
	    if (tLang != null) {
		result.append(datatype==Datatype.language ? " replacement=\"" : " rLang=\"").append(tLang).append('"');
	    }
	    if (tRegion != null) {
		result.append(datatype==Datatype.region ? " replacement=\"" : " rRegion=\"").append(tRegion).append('"');
	    }
	    if (tVariant != null) {
		result.append(datatype==Datatype.variant ? " replacement=\"" : " rVariant=\"").append(tVariant).append('"');
	    }
	    return result.toString();
	}
    }
    public static final class AliasesFull {
	public final Map<String, String> fullMap;
	public final Map<String, String> reasons;
	public final Multimap<String, ExceptionInfo> exceptions;
	private final Datatype datatype; // null is extensions, for now


	// TODO change to read file once
	/**
	 * Build a set of alias data. It is processed to make runtime computation easier for complex results:
	 * where the field(s) affected are not just the source field, such as when
	 * a variant changes the language.
	 * @param aliasDataSource 
	 */

	public AliasesFull(Datatype datatype, AliasDataSource aliasDataSource) {
	    this.datatype = datatype;
	    Map<String, String> _fullMap = new TreeMap<>();
	    Map<String, String> _reasons = new TreeMap<>();
	    Multimap<String, ExceptionInfo> _exceptions = LinkedHashMultimap.create();
	    Multimap<String, Row.R3<ULocale,ULocale,String>> exceptionsStored = LinkedHashMultimap.create();

	    List<AliasData> rawData = aliasDataSource.getData(datatype);

	    for (AliasData datum : rawData) {
		processData(datum, _fullMap, _reasons, _exceptions, exceptionsStored);
	    }

	    for (Entry<String, Collection<Row.R3<ULocale,ULocale,String>>> entry : exceptionsStored.asMap().entrySet()) {
		String code = entry.getKey();
		String oldTo = _fullMap.get(code);
		String oldReason = _reasons.get(code);
		Collection<Row.R3<ULocale,ULocale,String>> infos = (Set<Row.R3<ULocale,ULocale,String>>) entry.getValue();
		for (Row.R3<ULocale,ULocale,String> info : infos) {
		    ULocale context = info.get0();
		    ULocale aliasTo = info.get1();
		    String reason = info.get2();
		    // TODO put longer maps first
		    _exceptions.put(code, new ExceptionInfo(datatype, context, aliasTo, reason));
		}
		if (oldTo != null) {
		    String prefix = datatype == Datatype.language ? "" : "und-";
		    _exceptions.put(code, new ExceptionInfo(datatype, null, new ULocale(prefix + oldTo), oldReason)); 
		}
		_fullMap.put(code, "");
	    }
	    fullMap = ImmutableMap.copyOf(_fullMap);
	    reasons = ImmutableMap.copyOf(_reasons);
	    exceptions = ImmutableMultimap.copyOf(_exceptions);
	    if (DEBUG) {
		System.out.println("\n" + datatype);
		System.out.println(this);
		System.out.println(this.toXML());
	    }
	}



	void processData(AliasData datum, Map<String, String> _fullMap, 
		Map<String, String> _reasons, 
		Multimap<String, ExceptionInfo> _exceptions, 
		Multimap<String, 
		R3<ULocale, ULocale, String>> exceptionsStored) {

	    String aliasFrom = datum.aliasFrom;
	    String reason = datum.reason;
	    if (reason.equals("legacy")) {
		return; // skip these
	    }

	    String aliasTo = datum.aliasTo;
	    if (DEBUG && (aliasFrom.contains("-") || aliasTo.contains("-"))) {
		System.out.println(
			"aliasFrom: " + aliasFrom
			+ " aliasTo: " + aliasTo
			+ " reason: " + reason
			);
	    }

	    switch(datatype) {
	    case language:
		// FIX
		String sLang = SEP_SPLITTER.split(aliasFrom).iterator().next();
		String tLang = SEP_SPLITTER.split(aliasTo).iterator().next();
		if (sLang.equals(tLang)) {
		    ULocale source = new ULocale(aliasFrom);
		    ULocale target = new ULocale(aliasTo);
		    if (ULocale.addLikelySubtags(source).equals(ULocale.addLikelySubtags(target))) {
			//"* SKIP equivalent mappings " + source + " ==> " + target + " (" + reason + ")"
			if (DEBUG) System.out.println(showKeyValueReason(new StringBuilder("<!-- "), 
				source.toString(), 
				target.toString(), reason).append(" — skip equivalent -->").toString());
			return;
		    }
		}
		break;
	    case region: 
		// skip overlong region 3-letter?
		if ("overlong".equals(reason) && aliasFrom.length() == 3
		&& aliasFrom.charAt(0) >= 'A') {
		    if (DEBUG) System.out.println(showKeyValueReason(new StringBuilder("<!-- "), 
			    aliasFrom, 
			    aliasTo, reason+"-notUbli").toString());
		    return;
		}
		break;
	    case variant:
		aliasFrom = aliasFrom.toLowerCase(Locale.ROOT);
		aliasTo = aliasTo.toLowerCase(Locale.ROOT);
		break;
	    }

	    ULocale context = null;
	    boolean fromLocale = aliasFrom.contains("-");
	    boolean toLocale = aliasTo.contains("-");
	    boolean multipleTargets = aliasTo.contains(" ");
	    String prefix = datatype == Datatype.language ? "" : "und-";

	    if (multipleTargets && datatype != Datatype.region) {
		throw new ICUException();
	    }
	    if (fromLocale || toLocale || multipleTargets || datatype == Datatype.variant && aliasTo.length() < 5) {
		if (fromLocale) {
		    context = new ULocale(aliasFrom);
		    aliasFrom = context.getLanguage();
		}
		if (multipleTargets) {
		    Set<ULocale> contextsSoFar = new HashSet<>();
		    String firstLang = null;
		    String firstRegion = null;
		    List<String> targets = SPACE_SPLITTER.splitToList(aliasTo);

		    for (String target : targets) {
			ULocale targetLocale = ULocale.forLanguageTag(prefix + target);
			ULocale max = ULocale.addLikelySubtags(targetLocale);
			String maxLang = max.getLanguage();
			if (!maxLang.equals("und")) {
			    if (firstLang == null) {
				if (DEBUG) System.out.println("* STORE DEFAULT region context: " + aliasFrom + ", " + maxLang + " ==> " + target);
				firstLang = maxLang;
				firstRegion = target;
				continue;
			    } else if (firstLang.equals(maxLang)) {
				if (DEBUG) System.out.println("* SKIP region context: " + aliasFrom + ", " + maxLang + " ==> " + targetLocale);
				continue;
			    }
			    context = new ULocale(maxLang);
			    //targetLocale = new ULocale.Builder().setLocale(targetLocale).setLanguage(maxLang).build();
			    if (DEBUG) System.out.println("* SET region context: " + aliasFrom + ", " + maxLang + " ==> " + targetLocale);
			}
			if (contextsSoFar.contains(context)) {
			    if (DEBUG) System.out.println("* SKIP2 region context: " + aliasFrom + ", " + maxLang + " ==> " + targetLocale);
			    continue;
			}
			contextsSoFar.add(context);
			exceptionsStored.put(aliasFrom, Row.of(context, targetLocale, reason));
		    }
		    if (firstLang != null) {
			if (DEBUG) System.out.println("* SET DEFAULT region context: " + aliasFrom + " (" + firstLang + ") ==> " + firstRegion);
			_fullMap.put(aliasFrom, firstRegion);
			_reasons.put(aliasFrom, reason);
		    }
		} else {
		    ULocale targetLocale = new ULocale(prefix + aliasTo);
		    exceptionsStored.put(aliasFrom, Row.of(context, targetLocale, reason));
		}
	    } else {
		_fullMap.put(aliasFrom, aliasTo);
		_reasons.put(aliasFrom, reason);
	    }
	}

	@Override
	public String toString() {
	    return toString(true, true);
	}
	public String toString(boolean simple, boolean complex) {
	    StringBuilder result = new StringBuilder();
	    for (Entry<String, String> entry : fullMap.entrySet()) {
		String key = entry.getKey();
		String value = entry.getValue();
		if (!value.isEmpty()) {
		    if (simple) {
			result.append(key)
			.append("\tresult: ").append(value)
			.append('\n');
		    }
		} else if (complex) {
		    Collection<ExceptionInfo> exceptionList = exceptions.get(key);
		    for (ExceptionInfo exception : exceptionList) {
			result.append(key)
			.append("\t").append(exception)
			.append('\n');
		    } 
		}
	    }
	    return result.toString();
	}
	public String toXML() {
	    //             <languageAlias type="art_lojban" replacement="jbo" reason="deprecated"/> <!-- Lojban -->

	    StringBuilder result = new StringBuilder();
	    for (Entry<String, ExceptionInfo> entry : exceptions.entries()) {
		String key = entry.getKey();
		ExceptionInfo exception = entry.getValue();
		String reason = exception.reason;
		result.append("<" + datatype + "Alias")
		.append(" type=\"").append(key).append('"')
		.append(exception.toXml(datatype))
		.append(" reason=\"").append(reason).append('"')
		.append(">\n");
	    }
	    result.append('\n');
	    for (Entry<String, String> entry : fullMap.entrySet()) {
		String key = entry.getKey();
		String value = entry.getValue();
		String reason = reasons.get(key);
		if (value.isEmpty()) {
		    continue;
		}
		showKeyValueReason(result, key, value, reason).append("\n");
	    }
	    return result.toString();
	}

	private StringBuilder showKeyValueReason(StringBuilder result, String key, String value, String reason) {
	    result.append("<" + datatype + "Alias")
	    .append(" type=\"").append(key).append('"')
	    .append(" replacement=\"").append(value).append('"')
	    .append(" reason=\"").append(reason).append("\"/>")
	    ;
	    return result;
	}

	/**
	 * Returns null if there is no replacement, or if there is an exception;
	 * @param code
	 * @param exception
	 * @return
	 */
	public String getCanonical(String code, Output<Collection<ExceptionInfo>> exception) {
	    String result = fullMap.get(code);
	    if (result == null) {
		exception.value = null;
	    } else if (!result.isEmpty()){
		exception.value = null;
	    } else {
		exception.value = exceptions.get(code);
		result = null;
	    }
	    return result;
	}
    }
}