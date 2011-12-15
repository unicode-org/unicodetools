package org.unicode.props;

public class TestProperties {
    public static void main(String[] args) {
        
        for (UcdProperty prop : UcdProperty.values()) {
            System.out.println(prop + "\t" + prop.getNames() + "\t" + prop.getEnums());
//            Collection<Enum> values = PropertyValues.valuesOf(prop);
//            System.out.println("values: " + values);
        }
        for (UcdPropertyValues.General_Category_Values prop : UcdPropertyValues.General_Category_Values.values()) {
            System.out.println(prop + "\t" + prop.getNames());
//            Collection<Enum> values = PropertyValues.valuesOf(prop);
//            System.out.println("values: " + values);
        }

        UcdPropertyValues.General_Category_Values q = UcdPropertyValues.General_Category_Values.Unassigned;
        System.out.println(q.getNames());

//        Enum x = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
//        //Bidi_Mirrored_Values y = Properties.Bidi_Mirrored_Values.No;
//        Enum z = PropertyValues.forValueName(UcdProperty.Bidi_Mirrored, "N");
//        Enum w = PropertyValues.forValueName(UcdProperty.General_Category, "Cc");
//        System.out.println(x + " " + z + " " + w);
    }

    
}
