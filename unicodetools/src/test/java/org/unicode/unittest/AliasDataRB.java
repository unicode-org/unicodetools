package org.unicode.unittest;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ValidIdentifiers.Datatype;
import com.ibm.icu.util.UResourceBundle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.unicode.unittest.LocaleCanonicalizer.AliasData;

public class AliasDataRB extends AliasDataSource {

    public List<AliasData> getData(Datatype datatype) {
        UResourceBundle metadata =
                UResourceBundle.getBundleInstance(
                        ICUData.ICU_BASE_NAME, "metadata", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle metadataAlias = metadata.get("alias");
        UResourceBundle aliases =
                metadataAlias.get(datatype == Datatype.region ? "territory" : datatype.toString());
        List<AliasData> results = new ArrayList<>();
        for (int i = 0; i < aliases.getSize(); i++) {
            UResourceBundle res = aliases.get(i);
            String aliasFrom = res.getKey();
            String reason = res.get("reason").getString();
            UResourceBundle aliasToRB = res.get("replacement");
            String aliasTo = aliasToRB.getString();
            // patch bad case
            if (datatype == Datatype.region
                    && aliasFrom.length() == 3
                    && aliasFrom.charAt(0) > '9') {
                continue;
            }
            results.add(new AliasData(aliasFrom, reason, aliasTo));
        }
        return results;
    }

    public Map<String, String> generateTests() {
        return super.generateTests();
    }
}
