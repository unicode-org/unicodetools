/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package org.unicode.temp;

import java.util.List;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.dev.util.UnicodeProperty;
import com.ibm.icu.text.UnicodeSet;

public abstract class UnicodePropertyX<T> extends UnicodeProperty {

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getTypeName()
     */
    @Override
    public String getTypeName() {
        // TODO Auto-generated method stub
        return super.getTypeName();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getVersion()
     */
    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return super.getVersion();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getValue(int)
     */
    @Override
    public String getValue(int codepoint) {
        // TODO Auto-generated method stub
        return super.getValue(codepoint);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getNameAliases(java.util.List)
     */
    @Override
    public List<String> getNameAliases(List<String> result) {
        // TODO Auto-generated method stub
        return super.getNameAliases(result);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getValueAliases(java.lang.String, java.util.List)
     */
    @Override
    public List<String> getValueAliases(String valueAlias, List<String> result) {
        // TODO Auto-generated method stub
        return super.getValueAliases(valueAlias, result);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getAvailableValues(java.util.List)
     */
    @Override
    public List<String> getAvailableValues(List<String> result) {
        // TODO Auto-generated method stub
        return super.getAvailableValues(result);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getVersion()
     */
    @Override
    protected String _getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getValue(int)
     */
    @Override
    protected String _getValue(int codepoint) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getNameAliases(java.util.List)
     */
    @Override
    protected List<String> _getNameAliases(List<String> result) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getValueAliases(java.lang.String, java.util.List)
     */
    @Override
    protected List<String> _getValueAliases(String valueAlias, List<String> result) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getAvailableValues(java.util.List)
     */
    @Override
    protected List<String> _getAvailableValues(List<String> result) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getMaxWidth(boolean)
     */
    @Override
    public int getMaxWidth(boolean getShortest) {
        // TODO Auto-generated method stub
        return super.getMaxWidth(getShortest);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getSet(com.ibm.icu.dev.util.UnicodeProperty.PatternMatcher, com.ibm.icu.text.UnicodeSet)
     */
    @Override
    public UnicodeSet getSet(PatternMatcher matcher, UnicodeSet result) {
        // TODO Auto-generated method stub
        return super.getSet(matcher, result);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getUnicodeMap()
     */
    @Override
    public UnicodeMap getUnicodeMap() {
        // TODO Auto-generated method stub
        return super.getUnicodeMap();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getUnicodeMap(boolean)
     */
    @Override
    public UnicodeMap getUnicodeMap(boolean getShortest) {
        // TODO Auto-generated method stub
        return super.getUnicodeMap(getShortest);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getUnicodeMap_internal()
     */
    @Override
    public UnicodeMap getUnicodeMap_internal() {
        // TODO Auto-generated method stub
        return super.getUnicodeMap_internal();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#_getUnicodeMap()
     */
    @Override
    protected UnicodeMap _getUnicodeMap() {
        // TODO Auto-generated method stub
        return super._getUnicodeMap();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#isValidValue(java.lang.String)
     */
    @Override
    public boolean isValidValue(String propertyValue) {
        // TODO Auto-generated method stub
        return super.isValidValue(propertyValue);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#getValueAliases()
     */
    @Override
    public List<String> getValueAliases() {
        // TODO Auto-generated method stub
        return super.getValueAliases();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#isDefault(int)
     */
    @Override
    public boolean isDefault(int cp) {
        // TODO Auto-generated method stub
        return super.isDefault(cp);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#hasUniformUnassigned()
     */
    @Override
    public boolean hasUniformUnassigned() {
        // TODO Auto-generated method stub
        return super.hasUniformUnassigned();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeProperty#setUniformUnassigned(boolean)
     */
    @Override
    protected UnicodeProperty setUniformUnassigned(boolean hasUniformUnassigned) {
        // TODO Auto-generated method stub
        return super.setUniformUnassigned(hasUniformUnassigned);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeLabel#transform(java.lang.Integer)
     */
    @Override
    public String transform(Integer codepoint) {
        // TODO Auto-generated method stub
        return super.transform(codepoint);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.util.UnicodeLabel#getValue(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public String getValue(String s, String separator, boolean withCodePoint) {
        // TODO Auto-generated method stub
        return super.getValue(s, separator, withCodePoint);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return super.equals(obj);
    }
}

