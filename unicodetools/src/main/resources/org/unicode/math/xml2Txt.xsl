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
    <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">#</xsl:if><xsl:value-of
          select="@cp"/><xsl:if test="@unassigned = 'true' and @equivalent">=<xsl:value-of
          select="@equivalent"/></xsl:if>;<xsl:value-of
          select="@mathclass"/><xsl:text>&#x000D;</xsl:text>
  </xsl:template>

  <xsl:template name="introduction">
    <xsl:param name="unicode"/>
    <xsl:param name="tr25"/>
    <xsl:text># File: MathClass.txt
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
# mathematical notation.
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
# The data consists of 2 fields.
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

#code point;class
</xsl:text>
  </xsl:template>

  <xsl:template name="footer">
    <xsl:text>

# EOF</xsl:text>
  </xsl:template>

</xsl:stylesheet>
