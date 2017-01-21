package org.unicode.propstest;

import java.util.Set;

import org.unicode.props.IndexUnicodeProperties;
import org.unicode.props.PropertyType;
import org.unicode.props.UcdProperty;
import org.unicode.props.ValueCardinality;

public class ListProps {
public static void main(String[] args) {
    IndexUnicodeProperties latest = IndexUnicodeProperties.make();
    PropertyType lastType = null;
    for (UcdProperty item : UcdProperty.values()) {
    	PropertyType type = item.getType();
    	if (type != lastType) {
    		System.out.println("\n" + type + "\n");
    		lastType = type;
    	}
		System.out.println(item + (item.getCardinality() == ValueCardinality.Singleton ? "" : "\t\t**** " + item.getCardinality()));
    	Set<Enum> enums = item.getEnums();
    	if (enums != null) {
    		System.out.println("\t" + enums);
    	}
    }
}
}
