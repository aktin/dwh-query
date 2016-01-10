<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xml="http://www.w3.org/XML/1998/namespace" version="1.0">
	<xsl:template match="/template">
	<xsl:variable name="colpos" select="count(preceding-sibling::xhtml:col)"/>
	<xsl:variable name="datapos" select="count(preceding-sibling::xhtml:td)"/>
		<fo:root>
		  <fo:layout-master-set>
			<fo:simple-page-master master-name="page-layout" page-height="11in" page-width="8in">
			  <fo:region-body margin="1in" region-name="body"/>
			</fo:simple-page-master>
		  </fo:layout-master-set>
		  <fo:declarations>
			<x:xmpmeta xmlns:x="adobe:ns:meta/">
			<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
      <rdf:Description rdf:about="" xmlns:pdf="http://ns.adobe.com/pdf/1.3/" xmlns:xmp="http://ns.adobe.com/xap/1.0/" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <dc:title>fo-report</dc:title>
        <dc:creator><xsl:value-of select="document('report-data.xml')/report-data/text[@id='leitung']/text()"/></dc:creator>
        <dc:description>Notaufnahmebericht</dc:description>
        <pdf:Keywords>Stichworte</pdf:Keywords>
       <xmp:CreatorTool>erstellt mit Apache FOP</xmp:CreatorTool>
         <xmp:CreationDate>2015-12-05T08:15:30-05:00</xmp:CreationDate> <!-- macht wohl nichts, Datum wird automatisch auf die aktuelle Zeit gesetzt -->
          <xmp:ModifyDate>2015-12-08T08:15:30-05:00</xmp:ModifyDate>
       </rdf:Description>
    </rdf:RDF>
  </x:xmpmeta>
</fo:declarations>
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
			<xsl:attribute name="border-bottom-style">solid</xsl:attribute>
			<xsl:attribute name="border-collapse">collapse</xsl:attribute>
			<xsl:attribute name="space-before">10pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="tablehead">
			<xsl:attribute name="font-weight">bold</xsl:attribute>
			<xsl:attribute name="border-bottom-style">solid</xsl:attribute>
			<xsl:attribute name="border-top-style">solid</xsl:attribute>
			<xsl:attribute name="border-collapse">collapse</xsl:attribute>
			<xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="cellborder">
			<!--<xsl:attribute name="border-style">solid</xsl:attribute>-->
  </xsl:attribute-set>
   <xsl:attribute-set name="celltext">
			<xsl:attribute name="hyphenate">true</xsl:attribute>
			<xsl:attribute name="hyphenation-remain-character-count">4</xsl:attribute>
			<xsl:attribute name="xml:lang">de</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="headerformat">
			<xsl:attribute name="font-size">20pt</xsl:attribute>
			<xsl:attribute name="font-weight">bold</xsl:attribute>
			<xsl:attribute name="font-family">Courier</xsl:attribute>
			<xsl:attribute name="text-align">center</xsl:attribute>
			<xsl:attribute name="space-before">30pt</xsl:attribute>
			<xsl:attribute name="space-after">30pt</xsl:attribute>
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
    <fo:block keep-with-previous="always" space-before="10pt"><xsl:value-of select="./text()"/></fo:block>
 </xsl:template>
 
 <xsl:template match="xhtml:table">
	<fo:table xsl:use-attribute-sets="table">
		<xsl:apply-templates select="xhtml:col"/>
		<fo:table-header>
			<xsl:apply-templates select="xhtml:thead/xhtml:tr"/>
		</fo:table-header>
		<fo:table-body>
			<xsl:apply-templates select="xhtml:tbody/xhtml:tr"/>
		</fo:table-body>
	</fo:table>
 </xsl:template>
 
 <xsl:template match="xhtml:col">
	<fo:table-column>
			<xsl:attribute name="column-width"><xsl:value-of select="@width"/></xsl:attribute>
			<xsl:attribute name="text-align"><xsl:value-of select="@align"/></xsl:attribute>
		</fo:table-column>
</xsl:template>
	
 <xsl:template match="xhtml:thead/xhtml:tr">
	<fo:table-row xsl:use-attribute-sets="cellborder">
		<xsl:apply-templates select="xhtml:th"/>
	</fo:table-row>
 </xsl:template>
 
 <xsl:template match="xhtml:th">
 <fo:table-cell xsl:use-attribute-sets="tablehead">
		<fo:block><xsl:value-of select="./text()"/></fo:block>
	</fo:table-cell>
 </xsl:template>
 
 <xsl:template match="xhtml:td">
 <xsl:variable name="p" select="position()"/>
 <fo:table-cell xsl:use-attribute-sets="cellborder">
 <xsl:attribute name="text-align"><xsl:value-of select="../../../xhtml:col[$p]/@align"/></xsl:attribute>
		<fo:block xsl:use-attribute-sets="celltext">
		<xsl:value-of select="./text()"/></fo:block>
	</fo:table-cell>
 </xsl:template>
 
 <xsl:template match="xhtml:tr">
	<fo:table-row>
		<xsl:apply-templates select="xhtml:td"/>
	</fo:table-row>
 </xsl:template>

</xsl:stylesheet>
