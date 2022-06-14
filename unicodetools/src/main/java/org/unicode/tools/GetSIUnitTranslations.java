package org.unicode.tools;

import java.util.TreeMap;
import org.unicode.cldr.tool.LanguageCodeConverter;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M3;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XPathParts;

public class GetSIUnitTranslations {
    public static void main(String[] args) {
        Factory factory = CLDRConfig.getInstance().getCldrFactory();
        for (String locale :
                "af sq am ar hy as az eu be bn bs bg my ca zh-HK zh-CN zh-TW hr cs da nl en-GB et fa fil fi fr fr-CA gl ka de el gu iw hi hu is id it ja kn kk km ko ky lo lv lt mk ms ml mr mn ne no or pl pt-BR pt-PT pa ro ru sr si sk sl es es-419 sw sv ta te th tr uk ur uz vi zu rm"
                        .split(" ")) {
            String cldrLocale =
                    LanguageCodeConverter.GOOGLE_CLDR.getOrDefault(
                            locale, locale.replace('-', '_'));
            // System.out.println("# " + cldrLocale + "\t" + locale);
            CLDRFile cldrFile = factory.make(cldrLocale, true);
            M3<String, String, String> cm =
                    ChainedMap.of(new TreeMap(), new TreeMap(), String.class);
            for (String path : cldrFile) {
                // ldml/units/unitLength[@type="short"]/unit[@type="digital-petabyte"]/unitPattern[@count="one"]
                if (!path.startsWith("//ldml/units/unitLength[@type=\"short\"]")) {
                    continue;
                }
                XPathParts parts = XPathParts.getFrozenInstance(path);
                if (!"unitPattern".equals(parts.getElement(-1))) {
                    continue;
                }
                String type = parts.getAttributeValue(-2, "type");
                if (!type.contains("byte")) {
                    continue;
                }
                String count = parts.getAttributeValue(-1, "count");
                if (!count.equals("other")) {
                    continue;
                }
                String value = cldrFile.getStringValue(path);
                value = value.replace("{0}", "").trim();
                String typeName = type.substring("digital-".length());
                System.out.println(locale + "\t" + typeName + "\t" + value);
                // cm.put(locale, type, value);
            }
        }
    }
}
