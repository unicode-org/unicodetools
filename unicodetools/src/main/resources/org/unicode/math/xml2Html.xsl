<?xml version="1.0" encoding="UTF-8"?>
<!--java -cp "c:\tmp\jars\*" net.sf.saxon.Transform -xi -o:.\MathClassEx.txt -s:.\unicode.xml -xsl:.\unicode2txt.xsl-->

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.w3.org/1999/xhtml"
  xmlns:f="http://url"
  version="2.0">

  <xsl:output
          method="xml"
          omit-xml-declaration="yes"
          indent="yes"
          doctype-public="-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN"
          doctype-system="http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd"
          encoding="UTF-8"/>

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
    <html xml:lang="en">
      <head>
        <meta http-equiv="content-type" content="application/xhtml+xml; charset=UTF-8" />
        <link rel="stylesheet" type="text/css" href="https://www.unicode.org/reports/reports-v2.css" />
        <title>MathClassEx</title>
        <style type="text/css">
          @import url("https://fonts.googleapis.com/css?family=Noto+Sans"); /* Sans-serif */
          @import url("https://fonts.googleapis.com/css?family=Noto+Sans+Mono"); /* Monospace */
          @import url("https://fonts.googleapis.com/css?family=Noto+Sans+Math"); /* Math - Arabic Mathematical Alphabetic Symbols (U+1EE00 - U+1EEFF)> */

          /* https://github.com/stipub/stixfonts */
          @font-face {
          font-family: STIX Two Math;
          src: local("STIX Two Math"),
          local("STIXTwoMath-Regular"),
          url("https://texlive.net/fonts/stix/STIXTwoMath-Regular.woff2");
          }
          html {
          font-family: "Noto Sans", Arial, Helvetica, sans-serif;
          }
          #MathClass tbody td:nth-child(2) {white-space:nowrap;font-family: "STIX Two Math", math, "Noto Sans", "Noto Sans Math", serif;font-size:125%;}
          #MathClass tbody td:nth-child(5) {width:10em;}
        </style>
      </head>
      <body style="background-color:#ffffff">
        <table class="header" cellpadding="0" cellspacing="0" width="100%">
          <tbody>
            <tr>
              <td class="icon">
                <a href="https://www.unicode.org/">
                  <img style="vertical-align:middle;border:0" alt="[Unicode]"
                       src="https://www.unicode.org/webscripts/logo60s2.gif" height="33" width="34" />
                </a>&#x00A0;<a class="bar" href="https://www.unicode.org/reports/">Technical Reports</a>
              </td>
            </tr>
            <tr>
              <td class="gray">&#x00A0;</td>
            </tr>
          </tbody>
        </table>
        <div class="body">
          <h1 style="text-align:center">Data file supporting Unicode® Technical Report #25</h1>
          <h2 style="text-align:center">MathClassEx</h2>
          <!-- Here"s where we add all sections of the document -->
          <xsl:call-template name="generate-body"/>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="generate-body">
    <xsl:call-template name="introduction">
      <xsl:with-param name="unicode" select="@unicode"/>
      <xsl:with-param name="tr25" select="@tr25"/>
    </xsl:call-template>
    <table id="MathClass">
      <thead>
        <tr style="background-color: #CCFFCC">
          <th>Code Point</th>
          <th>Character</th>
          <th>Math<br/>Class</th>
          <th>General<br/>Category</th>
          <th>Entity Name</th>
          <th>Entity Set</th>
          <th>Note/Description/Count</th>
          <th>Unicode Character Name</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="char"/>
      </tbody>
    </table>
    <xsl:call-template name="copyright"/>
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
    <xsl:element name="tr">
      <xsl:element name="td">
        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
          <xsl:attribute name="style">color:#A0A0A0</xsl:attribute>#</xsl:if><xsl:value-of
              select="@cp"/><xsl:if test="@unassigned = 'true' and @equivalent">=<xsl:value-of
              select="@equivalent"/></xsl:if></xsl:element>
      <xsl:element name="td">
        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
          <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:if test="not(@unassigned) or @unassigned != 'true'">
          <xsl:if test="starts-with($repertoire/key('cp_key', current()/@cp)/@na,
          'COMBINING')">&#x25CC;</xsl:if><xsl:value-of
              select="codepoints-to-string(f:hexToDec(@cp))"/></xsl:if></xsl:element>
      <xsl:element name="td">
        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
          <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="@mathclass"/></xsl:element>
      <xsl:element name="td">        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
        <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="$repertoire/key('cp_key', current()/@cp)/@gc"/></xsl:element>
      <xsl:element name="td">        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
        <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="normalize-space($entity)"/></xsl:element>
      <xsl:element name="td">        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
        <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="normalize-space($set)"/></xsl:element>
      <xsl:element name="td">        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
        <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="@note"/></xsl:element>
      <xsl:element name="td">        <xsl:if test="@deprecated = 'true' or @unassigned = 'true'">
        <xsl:attribute name="style">color:#A0A0A0</xsl:attribute></xsl:if>
        <xsl:value-of select="$repertoire/key('cp_key', current()/@cp)/@na"/></xsl:element>
    </xsl:element>
  </xsl:template>

  <xsl:template name="introduction">
    <xsl:param name="unicode"/>
    <xsl:param name="tr25"/>
    <p>File: <b>MathClassEx</b></p>
    <p>Revision: <b><xsl:value-of select="$unicode"/></b></p>
    <p>Date: <xsl:value-of select="format-date(current-date(), '[Y0001]-[M01]-[D01]')"/></p>
    <p>This file provides a more readable version of <a href="MathClassEx.txt">MathClassEx.txt</a>.</p>
    <p>This file is a classification of characters based on their usage in mathematical notation and providing a
      mapping to standard entity sets commonly used for SGML and MathML documents.</p>
    <p>While the contents of this file represent the best information available to the authors and the Unicode
      Technical Committee as of the date referenced above, it is likely that the information in this file will change
      from time to time.</p>
    <p>This file is <b>not</b> formally part of the Unicode Character Database at this time.</p>
    <p>The data consists of 8 fields. The number and type of fields may change in future versions of this file.</p>
    <ol>
      <li>code point or range</li>
      <li>Unicode character (UTF-8)</li>
      <li>class, one of:
        <ul>
          <li>N &#x2013; Normal &#x2013; includes all digits and symbols requiring only one form</li>
          <li>A &#x2013; Alphabetic</li>
          <li>B &#x2013; Binary</li>
          <li>C &#x2013; Closing &#x2013; usually paired with opening delimiter</li>
          <li>D &#x2013; Diacritic</li>
          <li>F &#x2013; Fence &#x2013; unpaired delimiter (often used as opening or closing)</li>
          <li>G &#x2013; Glyph_Part &#x2013; piece of large operator</li>
          <li>L &#x2013; Large &#x2013; n-ary or large operator, often takes limits</li>
          <li>O &#x2013; Opening &#x2013; usually paired with closing delimiter</li>
          <li>P &#x2013; Punctuation</li>
          <li>R &#x2013; Relation &#x2013; includes arrows</li>
          <li>S &#x2013; Space</li>
          <li>U &#x2013; Unary &#x2013; operators that are only unary</li>
          <li>V &#x2013; Vary &#x2013; operators that can be unary or binary depending on context</li>
          <li>X &#x2013; Special &#x2013; characters not covered by other classes</li>
        </ul>
        <p>The C, O, and F operators are stretchy. In addition, some binary operators such as U+002F are stretchy as
          noted in the descriptive comments. The classes are also useful in determining extra spacing around the
          operators as discussed in UTR #25.</p>
      </li>
      <li>Unicode General Category</li>
      <li>entity name</li>
      <li>entity set</li>
      <li>descriptive comments (of various types)
        <p>The descriptive comments provide more information about a character, or its specific appearance. Some
          descriptions contain common macro names (with slash) but in the majority of cases, the description is
          simply the description of the entity in the published entity set, if different from the formal Unicode
          character name. Minor differences in word order, punctuation and verb forms have been ignored, but not
          systematic differences in terminology, such as filled vs. black.</p>
        <p>In principle this allows location of entities by their description.</p>
      </li>
      <li>Unicode character name or names
        <p>Character names are provided for ease of reference only.</p>
      </li>
    </ol>
    <p>Some character positions in the Mathematical Alphanumeric Symbols block are reserved and have been mapped to
      the Letterlike Symbols block in Unicode.</p>
    <p>This is indicated in 24 special purpose comments.</p>
    <p>The character repertoire of this revision is the repertoire of Unicode Version <xsl:value-of
            select="$unicode"/>. For more information see Revision <xsl:value-of
            select="$tr25"/> or later of UTR #25.</p>
  </xsl:template>


  <xsl:template name="copyright">
    <p class="copyright">&#xa9; 2008–<xsl:value-of select="format-date(current-date(), '[Y0001]')"/> Unicode, Inc. This
      publication is protected by copyright, and permission must be obtained from Unicode, Inc. prior to any
      reproduction, modification, or other use not permitted by the
      <a href="https://www.unicode.org/copyright.html">Terms of Use</a>. Specifically, you may make copies of this
      publication and may annotate and translate it solely for personal or internal business purposes and not for
      public distribution, provided that any such permitted copies and modifications fully reproduce all copyright and
      other legal notices contained in the original. You may not make copies of or modifications to this publication
      for public distribution, or incorporate it in whole or in part into any product or publication without the
      express written permission of Unicode.</p>

    <p class="copyright">Use of all Unicode Products, including this publication, is governed by the Unicode
      <a href="https://www.unicode.org/copyright.html">Terms of Use</a>. The authors, contributors, and publishers have
      taken care in the preparation of this publication, but make no express or implied representation or warranty of
      any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental
      damages that may arise therefrom. This publication is provided “AS-IS” without charge as a convenience to
      users.</p>

    <p class="copyright">Unicode and the Unicode Logo are registered trademarks of Unicode, Inc., in the United States
      and other countries.</p>
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
