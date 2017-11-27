package org.unicode.props;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.props.UcdPropertyValues.Age_Values;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.VersionInfo;

public enum VersionToAge {
    //    ucd(ImmutableMap.<Age_Values, Long>builder()
    //            .put(Age_Values.V10_0, getDate(2017, 6))
    //            .put(Age_Values.V9_0, getDate(2016, 6))
    //            .put(Age_Values.V8_0, getDate(2015, 6))
    //            .put(Age_Values.V7_0, getDate(2014, 6))
    //            .put(Age_Values.V6_3, getDate(2013, 9))
    //            .put(Age_Values.V6_2, getDate(2012, 9))
    //            .put(Age_Values.V6_1, getDate(2012, 1))
    //            .put(Age_Values.V6_0, getDate(2010, 10))
    //            .put(Age_Values.V5_2, getDate(2009, 10))
    //            .put(Age_Values.V5_1, getDate(2008, 4))
    //            .put(Age_Values.V5_0, getDate(2006, 7))
    //            .put(Age_Values.V4_1, getDate(2005, 3))
    //            .put(Age_Values.V4_0, getDate(2003, 4))
    //            .put(Age_Values.V3_2, getDate(2002, 3))
    //            .put(Age_Values.V3_1, getDate(2001, 8))
    //            .put(Age_Values.V3_0, getDate(2000, 8))
    //            .put(Age_Values.V2_1, getDate(1999, 4))
    //            .put(Age_Values.V2_0, getDate(1996, 7))
    //            .put(Age_Values.V1_1, getDate(1995, 7))
    //            .put(Age_Values.Unassigned, Long.MAX_VALUE)
    //            .build()), 
    //
    //    emoji(ImmutableMap.<Age_Values, Long>builder()
    //            .put(Age_Values.V5_0, getDate(2017, 5))
    //            .put(Age_Values.V4_0, getDate(2016, 4))
    //            .put(Age_Values.V2_0, getDate(2015, 11))
    //            .put(Age_Values.V1_1, getDate(2015, 6))
    //            .put(Age_Values.Unassigned, Long.MAX_VALUE)
    //            .build()),

    ucd(ImmutableMap.<VersionInfo, Long>builder()
            .put(VersionInfo.getInstance(11, 0), getDate(2018, 6)) // emoji 6.0
            .put(VersionInfo.getInstance(10, 0), getDate(2017, 6)) // emoji 5.0
            .put(VersionInfo.getInstance(9, 0), getDate(2016, 6)) // emoji 4.0
            .put(VersionInfo.getInstance(8, 0), getDate(2015, 6)) // emoji 3.0
            .put(VersionInfo.getInstance(7, 0), getDate(2014, 6))
            .put(VersionInfo.getInstance(6, 3), getDate(2013, 9))
            .put(VersionInfo.getInstance(6, 2), getDate(2012, 9))
            .put(VersionInfo.getInstance(6, 1), getDate(2012, 1))
            .put(VersionInfo.getInstance(6, 0), getDate(2010, 10))
            .put(VersionInfo.getInstance(5, 2), getDate(2009, 10))
            .put(VersionInfo.getInstance(5, 1), getDate(2008, 4))
            .put(VersionInfo.getInstance(5, 0), getDate(2006, 7))
            .put(VersionInfo.getInstance(4, 1), getDate(2005, 3))
            .put(VersionInfo.getInstance(4, 0), getDate(2003, 4))
            .put(VersionInfo.getInstance(3, 2), getDate(2002, 3))
            .put(VersionInfo.getInstance(3, 1), getDate(2001, 8))
            .put(VersionInfo.getInstance(3, 0), getDate(2000, 8))
            .put(VersionInfo.getInstance(2, 1), getDate(1999, 4))
            .put(VersionInfo.getInstance(2, 0), getDate(1996, 7))
            .put(VersionInfo.getInstance(1, 1), getDate(1995, 7))
            .build()), 

    emoji(ImmutableMap.<VersionInfo, Long>builder()
            .put(VersionInfo.getInstance(11, 0), getDate(2018, 6))
            .put(VersionInfo.getInstance(5, 0), getDate(2017, 5))
            .put(VersionInfo.getInstance(4, 0), getDate(2016, 4))
            .put(VersionInfo.getInstance(3, 0), getDate(2016, 3))
            .put(VersionInfo.getInstance(2, 0), getDate(2015, 11))
            .put(VersionInfo.getInstance(1, 1), getDate(2015, 6))
            .build());

    final ImmutableMap<VersionInfo, Long> versionToDate;

    private VersionToAge (ImmutableMap<VersionInfo, Long> data) {
        versionToDate = data;
    }

    public static final VersionInfo UNASSIGNED = VersionInfo.getInstance(255);
    public static final VersionInfo OLDEST = VersionInfo.getInstance(0);

    public Set<VersionInfo> getVersions() {
        return versionToDate.keySet();
    }

    public static Long getDate(int year, int month) {
        return new Date(year-1900, month-1, 1).getTime();
    }

    public static VersionInfo getVersionInfo(Age_Values ageValues) {
        return ageValues == Age_Values.Unassigned ? UNASSIGNED 
                : VersionInfo.getInstance(ageValues.getShortName());
    }
    public static Age_Values getAgeValue(VersionInfo versionInfo) {
        return UNASSIGNED.equals(versionInfo) ? Age_Values.Unassigned 
                : Age_Values.forName(versionInfo.getVersionString(2, 2));
    }


    public long getLongDate(VersionInfo version) {
        Long date = versionToDate.get(version);
        return date == null ? Long.MAX_VALUE : date;
    }

    @Deprecated
    public long getLongDate(Age_Values version) {
        return getLongDate(getVersionInfo(version));
    }

    @Deprecated
    public Date getDate(Age_Values version) {
        return new Date(getLongDate(version));
    }

    @Deprecated
    public int getYear(Age_Values versionInfo) {
        return getDate(versionInfo).getYear()+1900;
    }

    public int getYear(VersionInfo versionInfo) {
        return new Date(getLongDate(versionInfo)).getYear()+1900;
    }

    public VersionInfo getVersionInfo(long requestedDate) {
        //System.out.println("req-date=" + new Date(requestedDate));
        VersionInfo best = OLDEST;
        // Find the latest version that is at or before requestedDate
        for (Entry<VersionInfo, Long> entry : versionToDate.entrySet()) {
            //System.out.println("entry-date=" + new Date(entry.getValue()));
            if (entry.getValue() <= requestedDate
                    && entry.getKey().compareTo(best) > 0) {
                best = entry.getKey();
            }
        }
        return best;
    }

    public VersionInfo getVersionInfoForYear(int year) {
        return getVersionInfo(getDate(year+1, 1)-1);
    }

    @Deprecated
    public Age_Values getAge(long requestedDate) {
        return getAgeValue(getVersionInfo(requestedDate));
    }

    public static void main(String[] args) {
        System.out.println(ucd.getYear(Age_Values.Unassigned));

        Age_Values age = ucd.getAge(new Date(2015-1900,11,31,23,59,59).getTime());
        System.out.println(age);
        for (Age_Values x : Age_Values.values()) {
            System.out.println(x + "\t" + ucd.getYear(x));
        }

        show(emoji);
        show(ucd);
    }

    private static void show(VersionToAge v2a) {
        for (VersionInfo version : v2a.getVersions()) {
            int year = v2a.getYear(version);
            System.out.println(v2a + "\t" + version + "\t" + year + "\t" + v2a.getVersionInfoForYear(year));
        }
    }

}

