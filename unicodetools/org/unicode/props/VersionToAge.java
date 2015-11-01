package org.unicode.props;

import java.util.Date;
import java.util.Map;

import org.unicode.props.UcdPropertyValues.Age_Values;

import com.google.common.collect.ImmutableMap;

public class VersionToAge {
    private static Map<Age_Values, Long> versionToDate 
    = ImmutableMap.<Age_Values, Long>builder()
    .put(Age_Values.V8_0, getDate(2015, 6))
    .put(Age_Values.V7_0, getDate(2014, 6))
    .put(Age_Values.V6_3, getDate(2013, 9))
    .put(Age_Values.V6_2, getDate(2012, 9))
    .put(Age_Values.V6_1, getDate(2012, 1))
    .put(Age_Values.V6_0, getDate(2010, 10))
    .put(Age_Values.V5_2, getDate(2009, 10))
    .put(Age_Values.V5_1, getDate(2008, 4))
    .put(Age_Values.V5_0, getDate(2006, 7))
    .put(Age_Values.V4_1, getDate(2005, 3))
    .put(Age_Values.V4_0, getDate(2003, 4))
    .put(Age_Values.V3_2, getDate(2002, 3))
    .put(Age_Values.V3_1, getDate(2001, 8))
    .put(Age_Values.V3_0, getDate(2000, 8))
    .put(Age_Values.V2_1, getDate(1999, 4))
    .put(Age_Values.V2_0, getDate(1996, 7))
    .put(Age_Values.V1_1, getDate(1995, 7))
    .put(Age_Values.Unassigned, Long.MAX_VALUE)
    .build();
    private static Long getDate(int year, int month) {
        return new Date(year-1900, month-1, 1).getTime();
    }
    public static Date getDate(Age_Values version) {
        return new Date(versionToDate.get(version));
    }
    public static int getYear(Age_Values versionInfo) {
        return getDate(versionInfo).getYear()+1900;
    }
    public static void main(String[] args) {
        for (Age_Values x : Age_Values.values()) {
            System.out.println(x + "\t" + getYear(x));
        }
    }
}

