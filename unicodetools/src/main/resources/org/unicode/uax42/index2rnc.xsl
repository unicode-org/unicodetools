<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:ucdxml="http://unicode.org/ns/2001/ucdxml"
  xmlns="http://www.w3.org/TR/REC-html40"
  version="2.0">

  <xsl:output
          method="text"
          encoding="UTF-8"/>

  <xsl:template match='/'>
    <xsl:apply-templates select='/descendant::ucdxml:schema[@file]'/>
  </xsl:template>

  <xsl:template match='ucdxml:schema[@file]'>
    # Copyright &#x00A9; <xsl:value-of select='/article/articleinfo/copyright/year'/> Unicode, Inc.

    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='ucdxml:block[@revisionflag="deleted"]'/>

  <xsl:key name='block-key' match='ucdxml:block' use='@id'/>

  <xsl:template match='ucdxml:include'>
    <xsl:apply-templates select='key("block-key", @linkend)'/>
  </xsl:template>

  <xsl:template match='phrase[@revisionflag="deleted"]'/>

  <xsl:template match='phrase[@revisionflag="added"]'>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='@* | node()'>
    <xsl:copy>
      <xsl:apply-templates select='@* | node()'/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="JRW"/>

</xsl:stylesheet>
