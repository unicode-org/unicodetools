package org.unicode.propstest;

import org.unicode.cldr.util.Counter;
import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.UcdProperty;
import org.unicode.props.UcdPropertyValues.Age_Values;
import org.unicode.props.UcdPropertyValues.General_Category_Values;
import org.unicode.props.UcdPropertyValues.Script_Values;
import org.unicode.text.utility.Settings;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.text.UnicodeSet;

public class ShowScripts {
    public static void main(String[] args) {
        IndexUnicodeProperties iup = IndexUnicodeProperties.make(Settings.latestVersion);
        UnicodeMap<Script_Values> script = iup.loadEnum(UcdProperty.Script, Script_Values.class);
        UnicodeMap<Age_Values> age = iup.loadEnum(UcdProperty.Age, Age_Values.class);
        UnicodeMap<General_Category_Values> gc = iup.loadEnum(UcdProperty.General_Category, General_Category_Values.class);

        Counter<Script_Values> scriptCounter = new Counter<>();
        Counter<General_Category_Values> gcCounter = new Counter<>();
        Counter<General_Category_Values> gcCounter2 = new Counter<>();
        for (String s : age.getSet(Age_Values.V9_0)) {
            scriptCounter.add(script.get(s), 1);
            gcCounter.add(gc.get(s), 1);
        }
        
        for (String s : new UnicodeSet(0,0x10FFFF)) {
            final General_Category_Values gcv = gc.get(s);
            if (gcv == gcv.Unassigned || gcv == gcv.Private_Use || gcv == gcv.Surrogate) {
                continue;
            }
            gcCounter2.add(gcv, 1);
        }

        
        show(scriptCounter);
        System.out.println();
        show(gcCounter);
        System.out.println();
        show(gcCounter2);
        
        System.out.println("Properties: " + UcdProperty.values().length);
    }

    private static <T> void show(Counter<T> scriptCounter) {
        for (T s : scriptCounter.getKeysetSortedByCount(false)) {
            System.out.println(scriptCounter.get(s) + "\t" +  s);
        }
        System.out.println(scriptCounter.getTotal() + "\tTOTAL");
    }
}
