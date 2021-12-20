package org.unicode.jsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import com.ibm.icu.text.UnicodeSet;

import org.junit.jupiter.api.Test;
import org.unicode.props.UcdPropertyValues.Age_Values;

public class TestUBAVersion {
    @Test
    void UBAVersionTest() {
        final Age_Values current = UBAVersion.getCurrent();
        assertNotNull(current);

        // toString: 14.0
        String currentString = current.getShortName();
        assertNotNull(currentString);
        assertTrue(UnicodeSet.fromAll(currentString).contains('.'));
        assertTrue(UnicodeSet.fromAll(currentString).containsSome("0123456789"));

        // toString: 140
        String selectString = UBAVersion.toSelect(current);
        assertNotNull(selectString);
        assertFalse(UnicodeSet.fromAll(selectString).contains('.'));
        assertTrue(UnicodeSet.fromAll(selectString).containsSome("0123456789"));

        final EnumSet<Age_Values> versions = UBAVersion.getVersions();
        assertNotNull(versions);

        // Current is the last item
        assertTrue(current.equals(versions.toArray()[versions.size()-1]));

        // First is 6.2
        final Age_Values first = versions.iterator().next();
        assertEquals("6.2", first.getShortName());
        assertEquals("62", UBAVersion.toSelect(first));
    }
}
