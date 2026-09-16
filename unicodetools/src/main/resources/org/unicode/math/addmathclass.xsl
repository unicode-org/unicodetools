
<!--
    Update MathClasEx.xml from ucd.nounihan.gruped.xml
    saxon MathClassEx.xml addmathclass.xsl  > MathClassEx2.xml
    
    New entries will have mathclass="?" which need to be decided and adjusted
    then commit as a new MathClassEx.xml
-->

<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:variable name="ucd" select="doc('ucd.nounihan.grouped.xml')"/>
  <xsl:variable name="ex" select="/"/>
  <xsl:output doctype-system="MathClassEx.dtd" indent="no"/>
  <xsl:key name="c" match="char" use="@cp"/>
    
  <xsl:template match="/">
    <mathchars unicode="17.0" tr25="16">
      <xsl:message select="'###', count($ucd//*:char[@Math='Y']|$ucd//*:group[@Math='Y']/*:char[not(@Math='N')])"/>
      <xsl:for-each select="$ucd//*:char[@Math='Y']|
			    $ucd//*:group[@Math='Y']/*:char[not(@Math='N')]|
			    $ucd//*:char[key('c',@cp,$ex)]|
			    $ucd//*:reserved[key('c',@cp,$ex)|key('c',@first-cp,$ex)]
			    ">
	<xsl:text>&#10;   </xsl:text>
	<char cp="{@cp|@first-cp}" mathclass="?">
	  <xsl:copy-of select="key('c',@cp|@first-cp,$ex)/@mathclass"/>
	  <xsl:copy-of select="key('c',@cp|@first-cp,$ex)/@unassigned"/>
	  <xsl:copy-of select="key('c',@cp|@first-cp,$ex)/@equivalent"/>
	  <xsl:copy-of select="key('c',@cp,$ex)/@note"/>
	  <xsl:copy-of select="key('c',@cp,$ex)/@deprecated"/>
	</char>
	<xsl:if test="@first-cp">
	  <xsl:text>&#10;   </xsl:text>
	  <char>
	    <xsl:copy-of select="key('c',@first-cp,$ex)/following-sibling::*[1]/@*"/>
	  </char>
	  <xsl:if test="key('c',@first-cp,$ex)/following-sibling::*[2]/@unassigned">
	    <xsl:text>&#10;   </xsl:text>
	    <char>
	      <xsl:copy-of select="key('c',@first-cp,$ex)/following-sibling::*[2]/@*"/>
	    </char>
	  </xsl:if>
	</xsl:if>
      </xsl:for-each>
      <xsl:text>&#10;</xsl:text>
    </mathchars>
    <xsl:text>&#10;</xsl:text>
  </xsl:template>
  
</xsl:stylesheet>
