package org.unicode.props;

import java.io.DataOutputStream;

import com.ibm.icu.text.UnicodeSet;

//import org.unicode.props.Properties.UcdProperty;

public class TestProperties {
    public static void main(String[] args) {
        //General_Category_Values q = Properties.General_Category_Values.Unassigned;
//        Enum x = UcdProperty.General_Category.forValueName("Cc");
//        //Bidi_Mirrored_Values y = Properties.Bidi_Mirrored_Values.No;
//        Enum z = UcdProperty.Bidi_Mirrored.forValueName("N");
//        Enum w = UcdProperty.General_Category.forValueName("Cc");
//        System.out.println(x + " " + z + " " + w);
        UnicodeSet test = new UnicodeSet("[:ll:]");
    }
    
}
