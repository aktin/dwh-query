<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" version="1.0">
	<xsl:template match="/cda-report">
		<!-- hier XSL-FO dokument ausgeben -->
    <HTML>
      <HEAD>
        <TITLE></TITLE>
      </HEAD>
      <BODY>
        <H1>
          <xsl:value-of select="head/period/@start"/>
        </H1>
        <xsl:apply-templates select="body/*"/>
      </BODY>
    </HTML>

	</xsl:template>

	<xsl:template match="xhtml:table">
		<!-- hier ausgabe einer xsl-fo tabelle! -->
		<html5>
			<xsl:copy-of select="."/> 
		</html5>
	</xsl:template>

	<xsl:template match="table">
		<xsl:apply-templates select="document(./@href)"/> 
	</xsl:template>

	<xsl:template match="plot">
		<!-- hier einbinden einer SVG grafik -->
	</xsl:template>
</xsl:stylesheet>
