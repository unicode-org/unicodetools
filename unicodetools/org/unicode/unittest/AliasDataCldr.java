package org.unicode.unittest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.unittest.LocaleCanonicalizer.AliasData;

import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;

public class AliasDataCldr extends AliasDataSource {
    static final SupplementalDataInfo SDI = CLDRConfig.getInstance().getSupplementalDataInfo();

    public List<AliasData> getData(Datatype datatype) {
	Map<String, Map<String, R2<List<String>, String>>> ALIASES = SDI.getLocaleAliasInfo();
	Map<String, R2<List<String>, String>> codeToInfo = ALIASES.get(
		datatype == Datatype.region ? "territory" : datatype.toString());
	List<AliasData> results = new ArrayList<>();
	for (Entry<String, R2<List<String>, String>> info : codeToInfo.entrySet()) {
	    String aliasFrom = info.getKey();
	    String reason = info.getValue().get1();
	    String aliasTo = LocaleCanonicalizer.SPACE_JOINER.join(info.getValue().get0());
	    // patch bad case
	    if (datatype == Datatype.region && aliasFrom.length() == 3 && aliasFrom.charAt(0) > '9') {
		continue;
	    }
	    results.add(new AliasData(aliasFrom, reason, aliasTo));
	}
	return results;
    }
}
