<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:unicode="http://unicode.org/ns/2001"
  xmlns:ucdxml="http://unicode.org/ns/2001/ucdxml"
  xmlns="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="unicode"
  version="2.0">

  <xsl:output
          method="xml"
          omit-xml-declaration="yes"
          indent="yes"
          doctype-public="-//W3C//DTD XHTML 1.0 Transitional//EN"
          doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
          encoding="UTF-8"/>

<!-- Start of the article, headings, and version table -->

  <xsl:template match="article">
<!--    For debugging-->
<!--    <xsl:comment>-->
<!--      XSLT Version = <xsl:copy-of select="system-property('xsl:version')"/>-->
<!--      XSLT Vendor = <xsl:copy-of select="system-property('xsl:vendor')"/>-->
<!--      XSLT Vendor URL = <xsl:copy-of select="system-property('xsl:vendor-url')"/>-->
<!--    </xsl:comment>-->
    <html>
      <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
        <link rel="stylesheet" type="text/css" href="https://www.unicode.org/reports/reports-v2.css" />
        <title>
          <xsl:choose>
            <xsl:when test="articleinfo/unicode:tr/@class='uax'">
              <xsl:text>UAX</xsl:text>
            </xsl:when>
            <xsl:when test="articleinfo/unicode:tr/@class='uts'">
              <xsl:text>UTS</xsl:text>
            </xsl:when>
            <xsl:when test="articleinfo/unicode:tr/@class='utr'">
              <xsl:text>UTR</xsl:text>
            </xsl:when>
          </xsl:choose>
          <xsl:text> #</xsl:text>
          <xsl:value-of select="articleinfo/unicode:tr/@number"/>
          <xsl:text>: </xsl:text>
          <xsl:value-of select="title"/>
        </title>
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
          <h2 style="text-align:center">
            <xsl:call-template name="display-stage"/>
            <xsl:choose>
              <xsl:when test="articleinfo/unicode:tr/@class='uax'">
                <xsl:text>Unicode® Standard Annex</xsl:text>
              </xsl:when>
              <xsl:when test="articleinfo/unicode:tr/@class='uts'">
                <xsl:text>Unicode® Technical Standard</xsl:text>
              </xsl:when>
              <xsl:when test="articleinfo/unicode:tr/@class='utr'">
                <xsl:text>Unicode® Technical Report</xsl:text>
              </xsl:when>
            </xsl:choose>
            <xsl:text> #</xsl:text>
            <xsl:value-of select="articleinfo/unicode:tr/@number"/>
          </h2>
          <h1 style="text-align:center"><xsl:value-of select="title"/></h1>
          <!-- Here"s where we add all sections of the document -->
          <xsl:call-template name="generate-body"/>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="generate-body">
    <xsl:call-template name="version-info"/>
    <xsl:apply-templates select="abstract"/>
    <xsl:call-template name="status"/>
    <xsl:call-template name="toc"/>
    <hr/>
    <xsl:apply-templates select="section|acknowledgments"/>
    <h2><a name="Modifications">Modifications</a></h2>
    <p>This section indicates the changes introduced by each revision.</p>
    <xsl:apply-templates select="articleinfo/revhistory"/>
    <hr/>
    <xsl:call-template name="copyright"/>
  </xsl:template>

  <xsl:template name="display-stage">
    <xsl:choose>
      <xsl:when test="articleinfo/unicode:tr/@stage='working-draft'">
        <span><xsl:attribute name="class">changed</xsl:attribute><xsl:text>Working draft </xsl:text></span>
      </xsl:when>
      <xsl:when test="articleinfo/unicode:tr/@stage='proposed-update'">
        <span><xsl:attribute name="class">changed</xsl:attribute><xsl:text>Proposed Update </xsl:text></span>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="version-info">

    <xsl:param name="thisnumber">
      <xsl:value-of select="articleinfo/unicode:tr/@number"/>
    </xsl:param>

    <xsl:param name="thisrev">
      <xsl:value-of select="articleinfo/revhistory/revision[1]/@revnumber"/>
    </xsl:param>

    <xsl:param name="thisurl">
      <xsl:text>https://www.unicode.org/reports/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>-</xsl:text>
      <xsl:value-of select="$thisrev"/>
      <xsl:text>.html</xsl:text>
    </xsl:param>

    <xsl:param name="prevrev">
      <xsl:value-of select="articleinfo/unicode:tr/@prevrev"/>
    </xsl:param>

    <xsl:param name="prevurl">
      <xsl:text>https://www.unicode.org/reports/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>-</xsl:text>
      <xsl:value-of select="$prevrev"/>
      <xsl:text>.html</xsl:text>
    </xsl:param>

    <xsl:param name="latesturl">
      <xsl:text>https://www.unicode.org/reports/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>/</xsl:text>
    </xsl:param>

    <xsl:param name="thisschema">
      <xsl:text>https://www.unicode.org/reports/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>/tr</xsl:text>
      <xsl:value-of select="$thisnumber"/>
      <xsl:text>-</xsl:text>
      <xsl:value-of select="$thisrev"/>
      <xsl:text>.rnc</xsl:text>
    </xsl:param>

    <table class="simple" width="90%">
      <tbody>
        <tr>
          <td valign="top" width="20%">Version</td>
          <td valign="top">
            <xsl:call-template name="apply-draft-highlighting"/>
            <xsl:if test="//article/articleinfo/unicode:tr/@class='uax'">Unicode </xsl:if>
            <xsl:value-of select="articleinfo/unicode:tr/@version"/>
          </td>
        </tr>
        <tr>
          <td valign="top">
            <xsl:choose>
              <xsl:when test="count(articleinfo/authors/author)=1">
                Editor
              </xsl:when>
              <xsl:otherwise>
                Editors
              </xsl:otherwise>
            </xsl:choose>
          </td>
          <td valign="top">
            <xsl:apply-templates select="articleinfo/authors"/>
          </td>
        </tr>
        <tr>
          <td valign="top">Date</td>
          <td valign="top">
            <xsl:call-template name="apply-draft-highlighting"/>
            <xsl:value-of select="articleinfo/revhistory/revision[1]/@date"/>
          </td>
        </tr>
        <tr>
          <td valign="top">This Version</td>
          <td valign="top">
            <xsl:call-template name="apply-draft-highlighting"/>
            <a href="{$thisurl}"><xsl:value-of select="$thisurl"/></a>
          </td>
        </tr>
        <tr>
          <td valign="top">Previous Version</td>
          <td valign="top">
            <xsl:choose>
              <xsl:when test="$prevrev = ''">
                <xsl:text>n/a</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:call-template name="apply-draft-highlighting"/>
                <a href="{$prevurl}"><xsl:value-of select="$prevurl"/></a>
              </xsl:otherwise>
            </xsl:choose>
          </td>
        </tr>
        <tr>
          <td valign="top">Latest Version</td>
          <td valign="top">
            <a href="{$latesturl}"><xsl:value-of select="$latesturl"/></a>
          </td>
        </tr>
        <tr>
          <td valign="top">Latest Proposed Update</td>
          <td valign="top">
            <a href="{$latesturl}proposed.html"><xsl:value-of select="$latesturl"/>proposed.html</a>
          </td>
        </tr>
        <xsl:if test="articleinfo/unicode:tr/@schema">
          <tr>
            <td valign="top">Schema</td>
            <td valign="top">
              <xsl:call-template name="apply-draft-highlighting"/>
              <a href="{$thisschema}"><xsl:value-of select="$thisschema"/></a>
            </td>
          </tr>
        </xsl:if>
        <tr>
          <td valign="top">Revision</td>
          <td valign="top">
            <xsl:call-template name="apply-draft-highlighting"/>
            <a href="#Modifications">
              <xsl:value-of select="$thisrev"/>
            </a>
          </td>
        </tr>
      </tbody>
    </table>
  </xsl:template>

  <xsl:template match="authors">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="author">
    <xsl:value-of select="firstname"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="surname"/>
    <xsl:apply-templates select="email"/><br/>
  </xsl:template>

  <xsl:template match="email">
    <xsl:text> (</xsl:text>
    <a><xsl:attribute name="href">mailto:<xsl:value-of select="."/></xsl:attribute><xsl:value-of select="."/></a>
    <xsl:text>)</xsl:text>
  </xsl:template>

  <!-- Summary/Abstract -->

  <xsl:template match="abstract">
    <h4 style="margin-top: 1em;">Summary</h4>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="abstract/para">
    <p><i><xsl:apply-templates/></i></p>
  </xsl:template>

  <!-- Status -->

  <xsl:template name="status">
    <h4><i>Status</i></h4>
    <xsl:choose>
      <xsl:when test="articleinfo/unicode:tr/@stage='approved'">
        <p><i>This document has been reviewed by Unicode members and other interested parties, and has been
          approved for publication by the Unicode Consortium. This is a stable document and may be used as reference
          material or cited as a normative reference by other specifications.</i></p>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <xsl:call-template name="apply-draft-highlighting"/>
          <i>This is a <b><span style="color:#ff0000">draft</span></b> document which may be updated, replaced, or
            superseded by other documents at any time. Publication does not imply endorsement by the Unicode
            Consortium.  This is not a stable document; it is inappropriate to cite this document as other than a
            work in progress.</i></p>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:choose>
      <xsl:when test="articleinfo/unicode:tr/@class='uax'">
        <blockquote>
          <p><i><b>A Unicode Standard Annex (UAX)</b> forms an integral part of the Unicode Standard, but is
            published online as a separate document. The Unicode Standard may require conformance to normative
            content in a Unicode Standard Annex, if so specified in the Conformance chapter of that version of the
            Unicode Standard. The version number of a UAX document corresponds to the version of the Unicode Standard
            of which it forms a part.</i></p>
        </blockquote>
        <p><i>Please submit corrigenda and other comments with the online reporting form [<a
                href="https://www.unicode.org/reporting.html">Feedback</a>]. Related information that is useful in
          understanding this annex is found in Unicode Standard Annex #41, &#x201C;<a
                  href="https://www.unicode.org/reports/tr41/tr41-32.html">Common References for Unicode Standard
            Annexes.</a>&#x201D; For the latest version of the Unicode Standard, see [<a
                  href="https://www.unicode.org/versions/latest/">Unicode</a>]. For a list of current Unicode
          Technical Reports, see [<a href="https://www.unicode.org/reports/">Reports</a>]. For more information about
          versions of the Unicode Standard, see [<a href="https://www.unicode.org/versions/">Versions</a>]. For any
          errata which may apply to this annex, see [<a href="https://www.unicode.org/errata/">Errata</a>].</i></p>
      </xsl:when>
      <xsl:when test="articleinfo/unicode:tr/@class='uts'">
        <blockquote>
          <p><i><b>A Unicode Technical Standard (UTS)</b> is an independent specification. Conformance to the Unicode
            Standard does not imply conformance to any UTS.</i></p>
        </blockquote>
        <p><i>Please submit corrigenda and other comments with the online reporting form [<a href="#biblio_feedback">
          Feedback</a>]. Related information that is useful in understanding this document is found in <a
                href="#references">References</a>.  For the latest version of the Unicode Standard see [<a
                href="#biblio_unicode">Unicode</a>]. For a list of current Unicode Technical Reports see [<a
                href="#biblio_reports">Reports</a>]. For more information about versions of the Unicode Standard, see
          [<a href="#biblio_versions">Versions</a>].</i></p>
      </xsl:when>
      <xsl:when test="articleinfo/unicode:tr/@class='utr'">
        <blockquote>
          <p><i><b>A Unicode Technical Report (UTR)</b> contains informative material. Conformance to the Unicode
            Standard does not imply conformance to any UTR. Other specifications, however, are free to make normative
            references to a UTR.</i></p>
        </blockquote>
        <p><i>Please submit corrigenda and other comments with the online reporting form [<a href="#biblio_feedback">
          Feedback</a>]. Related information that is useful in understanding this document is found in <a
                href="#references">References</a>.  For the latest version of the Unicode Standard see [<a
                href="#biblio_unicode">Unicode</a>]. For a list of current Unicode Technical Reports see [<a
                href="#biblio_reports">Reports</a>]. For more information about versions of the Unicode Standard, see
          [<a href="#biblio_versions">Versions</a>].</i></p>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- Table of Contents -->

  <xsl:template name="toc">
    <h4>Contents</h4>
    <ul class="toc">
      <xsl:apply-templates mode="toc"/>
      <li>
        <a href="#Modifications">Modifications</a>
      </li>
    </ul>
  </xsl:template>

  <xsl:template match="section|acknowledgments" mode="toc">
    <li>
      <xsl:apply-templates select="title" mode="toc"/>
      <xsl:if test="section">
        <ul class="toc">
          <xsl:apply-templates select="section|acknowledgements" mode="toc"/>
        </ul>
      </xsl:if>
    </li>
  </xsl:template>

  <xsl:template match="section/title" mode="toc">
    <xsl:param name="id">
      <xsl:call-template name="parentid"/>
    </xsl:param>
    <xsl:number level="multiple" count="section" format="1.1"/>
    <xsl:text>&#x00A0;&#x00A0;&#x00A0;&#x00A0;</xsl:text>
    <a href="#{$id}">
      <xsl:apply-templates/>
    </a>
  </xsl:template>

  <xsl:template match="acknowledgments/title" mode="toc">
    <xsl:param name="id">
      <xsl:call-template name="parentid"/>
    </xsl:param>
    <a href="#{$id}">
      <xsl:apply-templates/>
    </a>
  </xsl:template>

  <xsl:template match="*|text()" mode="toc"/>

  <!-- Sections -->
  <xsl:template match="section/title">
    <xsl:param name="id">
      <xsl:call-template name="parentid"/>
    </xsl:param>
    <xsl:element name="h{count(ancestor::section)+1}">
      <a name="{$id}">
        <xsl:number level="multiple" count="section" format="1.1"/>
        <xsl:text> </xsl:text>
        <xsl:apply-templates/>
      </a>
    </xsl:element>
  </xsl:template>

  <xsl:template match="acknowledgments/title">
    <xsl:param name="id">
      <xsl:call-template name="parentid"/>
    </xsl:param>
    <h2>
      <a name="{$id}">
        <xsl:apply-templates/>
      </a>
    </h2>
  </xsl:template>

  <xsl:template match="para">
    <p><xsl:apply-templates/></p>
  </xsl:template>

  <xsl:template match="codeblock">
    <pre><xsl:apply-templates/></pre>
  </xsl:template>

  <xsl:template match="codephrase">
    <code><xsl:apply-templates/></code>
  </xsl:template>

  <xsl:template match="ulink">
    <a href="{@url}">
      <xsl:if test="@type='newwindow'">
        <xsl:attribute name="target">_blank</xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="text()">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@url"/>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:preserve-space elements="ucdxml:block"/>

  <xsl:key name="ucdxml-key" match="ucdxml:block" use="@id"/>

  <xsl:template match="ucdxml:include">
    <i>
      <xsl:text>[</xsl:text>
        <xsl:choose>
          <xsl:when test="@title">
            <xsl:value-of select="@title"/>
          </xsl:when>
          <xsl:when test="count (key ('ucdxml-key', @linkend)) = 1">
            <xsl:value-of select="key ('ucdxml-key', @linkend)/@title"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@linkend"/>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:text>: </xsl:text>
        <xsl:for-each select="key('ucdxml-key',@linkend)">
          <xsl:variable name="link"><xsl:value-of
                  select="replace(lower-case(@title), ' ', '_')"/><xsl:text>_</xsl:text><xsl:number
                  count="ucdxml:block" level="any"/></xsl:variable>
          <a href="#ucdxml:{$link}"><xsl:number count="ucdxml:block" level="any"/></a>
          <xsl:if test="position() != last ()">, </xsl:if></xsl:for-each><xsl:text>]</xsl:text>
    </i>
  </xsl:template>

  <xsl:template match="ucdxml:block">
    <xsl:variable name="link"><xsl:value-of
            select="replace(lower-case(@title), ' ', '_')"/><xsl:text>_</xsl:text><xsl:number
            count="ucdxml:block" level="any"/></xsl:variable>
    <p>
      <xsl:apply-templates select="@edit"/>
      <i><a name="ucdxml:{$link}">[<xsl:value-of select="@title"/>,
        <xsl:number count="ucdxml:block" level="any"/>]
      </a>
        =</i>
      <tt style="white-space: pre;">
        <xsl:apply-templates/>
      </tt>
    </p>
  </xsl:template>

  <xsl:template match="ucdxml:schema">
    <xsl:variable name="link"><xsl:value-of
            select="replace(lower-case(@title), ' ', '_')"/></xsl:variable>
    <p>
      <i><a name="ucdxml:{$link}">[<xsl:value-of select="@title"/>]
      </a>
        =</i>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="ucdxml:schema/text()">
    <tt style="white-space: pre;"><xsl:value-of select="."/></tt>
  </xsl:template>

  <!-- Revision history aka. Modifications -->

  <xsl:template match="revision">
    <xsl:choose>
      <xsl:when test="@edit">
        <div>
          <xsl:apply-templates select="@edit"/>
          <p>
            <b>Revision <xsl:value-of select="@revnumber"/></b>
          </p>
          <xsl:apply-templates/>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <p>
          <b>Revision <xsl:value-of select="@revnumber"/></b>
        </p>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="proposed_update">
    <p>
      <xsl:apply-templates/>
    </p>
  </xsl:template>

  <xsl:template match="changes">
    <ul>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="change">
    <li>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <xsl:template match="reissued">
    <b><xsl:apply-templates/></b>
  </xsl:template>

  <!-- Copyright -->

  <xsl:template name="copyright">
    <p class="copyright">&#xa9; 2008–<xsl:apply-templates select="articleinfo/copyright/year"/> Unicode, Inc. This
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

  <!-- Common templates -->

  <xsl:template name="apply-draft-highlighting">
    <xsl:if test="//article/articleinfo/unicode:tr/@stage='proposed-update'">
      <xsl:attribute name="class">changed</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template name="parentid">
    <xsl:choose>
      <xsl:when test="../@id">
        <xsl:value-of select="../@id"/>
      </xsl:when>
      <xsl:when test="@id">
        <xsl:value-of select="@id"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="currentText" select="descendant::text()"/>
        <xsl:value-of
                select="replace(lower-case(descendant::text()), ' ', '_')"/><xsl:text>_</xsl:text><xsl:value-of
              select="count(preceding::title[descendant::text() = $currentText])"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="prop">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="elem">
    <tt><xsl:apply-templates/></tt>
  </xsl:template>

  <xsl:template match="attr">
    <tt><xsl:apply-templates/></tt>
  </xsl:template>

  <xsl:template match="val">
    <tt><xsl:apply-templates/></tt>
  </xsl:template>

  <xsl:template match="@edit">
    <xsl:attribute name="class"><xsl:value-of select="."/></xsl:attribute>
  </xsl:template>

  <xsl:template match="edit">
    <span><xsl:attribute name="class"><xsl:value-of select="@flag"/></xsl:attribute><xsl:apply-templates/></span>
  </xsl:template>


</xsl:stylesheet>
