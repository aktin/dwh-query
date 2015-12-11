<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
	<xsl:template match="/template">
		<fo:root>
		  <fo:layout-master-set>
			<fo:simple-page-master master-name="page-layout" page-height="11in" page-width="8in">
			  <fo:region-body margin="1in" region-name="body"/>
			</fo:simple-page-master>
		  </fo:layout-master-set>
		  <fo:page-sequence master-reference="page-layout">
			<fo:flow flow-name="body">
				<fo:block xsl:use-attribute-sets="headerformat"><xsl:value-of select="document('report-data.xml')/report-data/text[@id='kh']/text()"/></fo:block>
				<fo:block><xsl:value-of select="document('report-data.xml')/report-data/text[@id='bereich']/text()"/></fo:block> 
				<fo:block><xsl:value-of select="document('report-data.xml')/report-data/text[@id='leitung']/text()"/></fo:block> 
				<fo:block><xsl:value-of select="document('report-data.xml')/report-data/text[@id='monat']/text()"/></fo:block> 
				<fo:block><xsl:value-of select="document('report-data.xml')/report-data/text[@id='stand']/text()"/></fo:block>  
				<xsl:call-template name="genTOC"/>
				<xsl:apply-templates select="./section"/>
			</fo:flow>
		  </fo:page-sequence>
    </fo:root>
  </xsl:template>
  
  <xsl:attribute-set name="table">
			<xsl:attribute name="border-style">solid</xsl:attribute>
			<xsl:attribute name="space-before">10pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="tablehead">
			<xsl:attribute name="font-weight">bold</xsl:attribute>
			<xsl:attribute name="border-style">solid</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="cellborder">
			<xsl:attribute name="border-style">solid</xsl:attribute>
  </xsl:attribute-set>
   <xsl:attribute-set name="celltext">
			<xsl:attribute name="hyphenate">true</xsl:attribute>
			<xsl:attribute name="hyphenation-remain-character-count">4</xsl:attribute>
			<xsl:attribute name="xml:lang">de</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="headerformat">
			<xsl:attribute name="font-size">20pt</xsl:attribute>
			<xsl:attribute name="font-weight">bold</xsl:attribute>
			<xsl:attribute name="space-before">30pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="textheaderformat">
			<xsl:attribute name="font-size">15pt</xsl:attribute>
			<xsl:attribute name="font-weight">bold</xsl:attribute>
			<xsl:attribute name="space-before">30pt</xsl:attribute>
			<xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="subheaderformat">
			<xsl:attribute name="font-size">14pt</xsl:attribute>
			<xsl:attribute name="space-before">30pt</xsl:attribute>
			<xsl:attribute name="keep-with-next">always</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="plotformat">
			<xsl:attribute name="content-width">7in</xsl:attribute>
			<xsl:attribute name="content-height">6in</xsl:attribute>
  </xsl:attribute-set>
  
  <xsl:template name="genTOC">
	<fo:block break-before='page'  break-after='page' >
		<fo:block xsl:use-attribute-sets="headerformat">TABLE OF CONTENTS</fo:block>
		<xsl:for-each select="//section">
			<fo:block text-align-last="justify">
				<xsl:variable name="anzahl" select="count(ancestor::*)"/>
				<xsl:choose>
					<xsl:when test="$anzahl=1">
						<fo:block font-weight="bold">
							<fo:basic-link internal-destination="{generate-id(.)}">
								<xsl:text> </xsl:text>
								<xsl:value-of select="./header/text()" />
								<fo:leader leader-pattern="dots" />
								<fo:page-number-citation ref-id="{generate-id(.)}" />
							</fo:basic-link>
						</fo:block>
					</xsl:when>
					<xsl:otherwise>
						<fo:block text-indent="10mm">
							<fo:basic-link internal-destination="{generate-id(.)}">
								<xsl:text></xsl:text>
								<xsl:value-of select="./header/text()"/>
								<fo:leader leader-pattern="dots" />
								<fo:page-number-citation ref-id="{generate-id(.)}"/>
							</fo:basic-link>
						</fo:block>
					</xsl:otherwise>
				</xsl:choose>
			</fo:block>
		</xsl:for-each>
	</fo:block>
  </xsl:template>
  
  <xsl:template match="section">
		<fo:block id="{generate-id(.)}"/>
		<xsl:apply-templates select="./*"/>
 </xsl:template>

 <xsl:template match="header">
	<xsl:variable name="stufe" select="count(ancestor::*)"/>
	<xsl:choose>
		<xsl:when test="$stufe=2">
			<fo:block xsl:use-attribute-sets="textheaderformat"><xsl:value-of select="./text()"/></fo:block>
		</xsl:when>
		<xsl:otherwise>
			<fo:block xsl:use-attribute-sets="subheaderformat"><xsl:value-of select="./text()"/></fo:block>
		</xsl:otherwise>
	</xsl:choose>
 </xsl:template>
 
 <xsl:template match="paragraph">
	<fo:block><xsl:apply-templates select="text() | var"/></fo:block>
 </xsl:template>

<xsl:template match="var">
<xsl:variable name="id" select="@ref"/>
	<xsl:value-of select="document('report-data.xml')/report-data/text[@id=$id]/text()"/>
 </xsl:template>

 <xsl:template match="plot">
	<xsl:variable name="id" select="@ref"/>
	<xsl:variable name="href" select="document('report-data.xml')/report-data/plot[@id=$id]/@href"/>
	<fo:block space-before="10pt">
		<fo:external-graphic xsl:use-attribute-sets="plotformat">
			<xsl:attribute name="src"><xsl:value-of select="$href"/></xsl:attribute>
		</fo:external-graphic>
		<fo:block keep-with-previous="always"><xsl:value-of select="./text()"/></fo:block>
	</fo:block>
 </xsl:template>
 
 <xsl:template match="table">
	<xsl:variable name="id" select="@ref"/>
    <xsl:variable name="href" select="document('report-data.xml')/report-data/table[@id=$id]/@href"/>
    <xsl:apply-templates select="document($href)"/>
    <fo:block keep-with-previous="always"><xsl:value-of select="./text()"/></fo:block>
 </xsl:template>
 
 <xsl:template match="xhtml:table">
	<fo:table xsl:use-attribute-sets="table">
		<fo:table-header>
			<xsl:apply-templates select="xhtml:thead/xhtml:tr"/>
		</fo:table-header>
		<fo:table-body>
			<xsl:apply-templates select="xhtml:tbody/xhtml:tr"/>
		</fo:table-body>
	</fo:table>
 </xsl:template>

 <xsl:template match="xhtml:thead/xhtml:tr">
	<fo:table-row>
		<xsl:apply-templates select="xhtml:th"/>
	</fo:table-row>
 </xsl:template>
 
 <xsl:template match="xhtml:th">
	<fo:table-cell xsl:use-attribute-sets="tablehead">
		<fo:block><xsl:value-of select="./text()"/></fo:block>
	</fo:table-cell>
 </xsl:template>
 
 <xsl:template match="xhtml:td">
	<fo:table-cell xsl:use-attribute-sets="cellborder">
		<fo:block xsl:use-attribute-sets="celltext"><xsl:value-of select="./text()"/></fo:block>
	</fo:table-cell>
 </xsl:template>
 
 <xsl:template match="xhtml:tr">
	<fo:table-row>
		<xsl:apply-templates select="xhtml:td"/>
	</fo:table-row>
 </xsl:template>

</xsl:stylesheet>
