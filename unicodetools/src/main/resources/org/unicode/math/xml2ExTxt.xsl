<?xml version="1.0" encoding="UTF-8"?>
<!--java -cp "c:\tmp\jars\*" net.sf.saxon.Transform -xi -o:.\MathClassEx.txt -s:.\unicode.xml -xsl:.\unicode2txt.xsl-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:f="http://url"
  version="2.0">

  <xsl:output
          method="text"
          omit-xml-declaration="no"
          indent="no"
          encoding="UTF-8"/>
  <xsl:strip-space elements="*"/>

  <!--  Parameters  -->
  <xsl:param name="ucdxmlflatfile"/>
  <xsl:param name="unicodeentitiesfile"/>

  <xsl:variable name="repertoire"
                select="document($ucdxmlflatfile)"
                as="document-node()"/>
  <xsl:variable name="entities"
                select="document($unicodeentitiesfile)"
                as="document-node()"/>

  <!--  Keys  -->
  <xsl:key name="mathmlgroup" match="*:group[@name='mathml']/set" use="@name"/>
  <xsl:key name="html5group" match="*:group[@name='html5']/set" use="@name"/>
  <xsl:key name="allgroups" match="*:group[@name!='iso8879']/set" use="@name"/>
  <xsl:key name="cp_key" match="*:ucd/*:repertoire/*:char" use="@cp"/>
  <xsl:key name="character_key" match="*:unicode/*:charlist/*:character" use="replace(@id, '^U0?', '')"/>

  <!--  Templates  -->
  <xsl:template match="mathchars">
    <xsl:call-template name="introduction">
      <xsl:with-param name="unicode" select="@unicode"/>
      <xsl:with-param name="tr25" select="@tr25"/>
    </xsl:call-template>
    <xsl:apply-templates/>
    <xsl:call-template name="footer"/>
  </xsl:template>

  <xsl:template match="char">
    <xsl:variable name="entity"><xsl:for-each
            select="distinct-values($entities/key('character_key', current()/@cp)/entity[
            (starts-with(@set, '9573-2003')
            and not(ends-with(@set, 'isogrk1'))
            and not(ends-with(@set, 'isogrk2'))
            and not(ends-with(@set, 'isogrk4')))
            or starts-with(@set, 'mml')
            or starts-with(@set, 'xhtml')
            or starts-with(@set, 'html5')
            ]/@id)"><xsl:value-of
            select="."/><xsl:text> </xsl:text></xsl:for-each></xsl:variable>
    <xsl:variable name="isMathMLset"><xsl:for-each
            select="distinct-values($entities/key('character_key', current()/@cp)/entity[
            (starts-with(@set, '9573-2003')
            and not(ends-with(@set, 'isogrk1'))
            and not(ends-with(@set, 'isogrk2'))
            and not(ends-with(@set, 'isogrk4')))
            or starts-with(@set, 'mml')
            or starts-with(@set, 'xhtml')
            or starts-with(@set, 'html5')
            ]/@set)"><xsl:if test="($entities/key('mathmlgroup',
            current())/@fpi) != ''">true</xsl:if></xsl:for-each></xsl:variable>
    <xsl:variable name="isHTML5set"><xsl:for-each
            select="distinct-values($entities/key('character_key', current()/@cp)/entity[
            (starts-with(@set, '9573-2003')
            and not(ends-with(@set, 'isogrk1'))
            and not(ends-with(@set, 'isogrk2'))
            and not(ends-with(@set, 'isogrk4')))
            or starts-with(@set, 'mml')
            or starts-with(@set, 'xhtml')
            or starts-with(@set, 'html5')
            ]/@set)"><xsl:if test="($entities/key('html5group',
            current())/@fpi) != ''">true</xsl:if></xsl:for-each></xsl:variable>
    <xsl:variable name="isOtherset"><xsl:for-each
            select="distinct-values($entities/key('character_key', current()/@cp)/entity[
            (starts-with(@set, '9573-2003')
            and not(ends-with(@set, 'isogrk1'))
            and not(ends-with(@set, 'isogrk2'))
            and not(ends-with(@set, 'isogrk4')))
            or starts-with(@set, 'mml')
            or starts-with(@set, 'xhtml')
            or starts-with(@set, 'html5')
            ]/@set)"><xsl:if test="($entities/key('allgroups',
            current())/@fpi) != ''">true</xsl:if></xsl:for-each></xsl:variable>
    <xsl:variable name="set">
      <xsl:choose>
        <xsl:when test="$isHTML5set != '' or $isMathMLset != ''">HTML-MathML</xsl:when>
        <xsl:when test="$isOtherset != ''">W3C-Full</xsl:when>
        <xsl:otherwise/>
      </xsl:choose>
    </xsl:variable>
    <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">#</xsl:if><xsl:value-of
          select="@cp"/><xsl:if test="@unassigned = 'true' and @equivalent">=<xsl:value-of
          select="@equivalent"/></xsl:if>;<xsl:value-of
          select="@mathclass"/>;<xsl:value-of
          select="normalize-space($entity)"/>;<xsl:value-of
          select="normalize-space($set)"/>;<xsl:value-of
          select="@note"/><xsl:text> # </xsl:text><xsl:if
          test="(not(@unassigned) and not(@deprecated)) or @unassigned != 'true' or @deprecated != 'true'"><xsl:value-of
          select="$repertoire/key('cp_key', current()/@cp)/@gc"/><xsl:text> (</xsl:text><xsl:value-of
          select="codepoints-to-string(f:hexToDec(@cp))"/><xsl:text>) </xsl:text><xsl:value-of
          select="$repertoire/key('cp_key', current()/@cp)/@na"/></xsl:if><xsl:text>&#x000D;</xsl:text>
  </xsl:template>

  <xsl:template name="introduction">
    <xsl:param name="unicode"/>
    <xsl:param name="tr25"/>
    <xsl:text># File: MathClassEx.txt
# Revision: </xsl:text><xsl:value-of select="$unicode"/><xsl:text>
# Date: </xsl:text><xsl:value-of select="format-date(current-date(), '[Y0001]-[M01]-[D01]')"/><xsl:text>
#
# © </xsl:text><xsl:value-of select="format-date(current-date(), '[Y0001]')"/><xsl:text> Unicode®, Inc.
# Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.
# For terms of use, see http://www.unicode.org/terms_of_use.html
# For documentation, see http://www.unicode.org/reports/tr25/
#
# ------------------------------------------------
# This file is a classification of characters based on their usage in
# mathematical notation and providing a mapping to standard entity
# sets commonly used for SGML and MathML documents.
#
# While the contents of this file represent the best information
# available to the authors and the Unicode Technical Committee as
# of the date referenced above, it is likely that the information
# in this file will change from time to time.
#
# This file is *NOT* formally part of the Unicode Character Database
# at this time.
#
# The character encoding of this plain-text file is UTF-8.
#
# The data consists of 8 fields. The number and type of fields may change
# in future versions of this file.
#
# 1: code point or range
#
# 2: class, one of:
#
#	N - Normal - includes all digits and symbols requiring only one form
#	A - Alphabetic
#	B - Binary
#	C - Closing - usually paired with opening delimiter
#	D - Diacritic
#	F - Fence - unpaired delimiter (often used as opening or closing)
#	G - Glyph_Part - piece of large operator
#	L - Large - n-ary or large operator, often takes limits
#	O - Opening - usually paired with closing delimiter
#	P - Punctuation
#	R - Relation - includes arrows
#	S - Space
#	U - Unary - operators that are only unary
#	V - Vary - operators that can be unary or binary depending on context
#	X - Special - characters not covered by other classes
#
# The C, O, and F operators are stretchy. In addition, some binary operators such
# as U+002F are stretchy as noted in the descriptive comments. The classes are
# also useful in determining extra spacing around the operators as discussed
# in UTR #25.
#
# 3: entity name
#
# 4: entity set
#
# 5: descriptive comments (of various types)
# The descriptive comments provide more information about a character,
# or its specific appearance. Some descriptions contain common macro
# names (with slash) but in the majority of cases, the description is
# simply the description of the entity in the published entity set, if
# different from the formal Unicode character name. Minor differences
# in word order, punctuation and verb forms have been ignored, but not
# systematic differences in terminology, such as filled vs. black.
# In principle this allows location of entities by their description.
#
# 6: Comment with Unicode General Category, character (UTf-8)
#    and Unicode character name or names
#    Character names are provided for ease of reference only.
#
# Fields are delimited by ';'.
# Spaces adjacent to the delimiter or the '#' are not significant
# Future versions of this file may use different amounts of whitespace.
#
# Some character positions in the Mathematical Alphanumeric Symbols block are
# reserved and have been mapped to the Letterlike Symbols block in Unicode.
# This is indicated in 24 special purpose comments.
#
# The character repertoire of this revision is the repertoire of Unicode
# Version </xsl:text><xsl:value-of
          select="$unicode"/><xsl:text>. For more information see Revision </xsl:text><xsl:value-of
          select="$tr25"/><xsl:text> or later of UTR #25.
# ------------------------------------------------

#code point;class;entity;set;note/description # comment/Unicode name
</xsl:text>
  </xsl:template>

  <xsl:template name="footer">
    <xsl:text>

# EOF</xsl:text>
  </xsl:template>

  <!--  Functions  -->
  <xsl:function name="f:hexToDec">
    <xsl:param name="hex"/>
    <xsl:variable name="dec"
                  select="string-length(substring-before('0123456789ABCDEF', substring($hex,1,1)))"/>
    <xsl:choose>
      <xsl:when test="matches($hex, '([0-9]*|[A-F]*)')">
        <xsl:value-of
                select="if ($hex = '') then 0
        else $dec * f:power(16, string-length($hex) - 1) + f:hexToDec(substring($hex,2))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message>Provided value is not hexadecimal...</xsl:message>
        <xsl:value-of select="$hex"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="f:power">
    <xsl:param name="base"/>
    <xsl:param name="exp"/>
    <xsl:sequence
            select="if ($exp lt 0) then f:power(1.0 div $base, -$exp)
                else if ($exp eq 0)
                then 1e0
                else $base * f:power($base, $exp - 1)"/>
  </xsl:function>

</xsl:stylesheet>
