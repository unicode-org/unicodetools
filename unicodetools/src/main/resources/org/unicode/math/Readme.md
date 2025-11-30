Generate files:

- MathClassEx.html: java -cp {path to Saxon jar files} net.sf.saxon.Transform -xi -dtd -o:./MathClassEx.html -s:./MathClassEx.xml -xsl:./xml2Html.xsl ucdxmlflatfile={path to ucd.all.flat.xml} unicodeentitiesfile={path to unicode.xml from xml-entities}
    - Example: java -cp "/jars/*" net.sf.saxon.Transform -xi -dtd -o:./MathClassEx.html -s:./MathClassEx.xml -xsl:./xml2Html.xsl ucdxmlflatfile=/ucdxml/17.0.0/ucd.all.flat.xml unicodeentitiesfile=/xml-entities/unicode.xml
- MathClassEx.txt: java -cp {path to Saxon jar files} net.sf.saxon.Transform -xi -dtd -o:./MathClassEx.txt -s:./MathClassEx.xml -xsl:./xml2ExTxt.xsl ucdxmlflatfile={path to ucd.all.flat.xml} unicodeentitiesfile={path to unicode.xml from xml-entities}
    - Example: java -cp "/jars/*" net.sf.saxon.Transform -xi -dtd -o:./MathClassEx.txt -s:./MathClassEx.xml -xsl:./xml2ExTxt.xsl ucdxmlflatfile=/ucdxml/17.0.0/ucd.all.flat.xml unicodeentitiesfile=/xml-entities/unicode.xml
- MathClass.txt: java -cp {path to Saxon jar files} net.sf.saxon.Transform -xi -dtd -o:./MathClass.txt -s:./MathClassEx.xml -xsl:./xml2Txt.xsl
    - Example: java -cp "/jars/*" net.sf.saxon.Transform -xi -dtd -o:./MathClass.txt -s:./MathClassEx.xml -xsl:./xml2Txt.xsl
    
