<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    version="2.0">
    <xsl:output method="text"/>       
        
    <xsl:template match="/ClaML">
            
        <xsl:for-each select="/ClaML/Class[@kind='category']">
            <xsl:if test="string-length(./@code)=3"><xsl:value-of select="./@code"/><xsl:text>&#9;</xsl:text><xsl:value-of select="./Rubric[@kind='preferred']/Label"/><xsl:text>&#10;</xsl:text></xsl:if>
        </xsl:for-each>
        
    </xsl:template>
    
    <xsl:template match="text()"/>   <!-- Match plain text nodes and do nothing, i.e. mask default output for text nodes     -->

</xsl:stylesheet>