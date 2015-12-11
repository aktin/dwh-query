<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml" version="1.0">
	<xsl:template match="/template">
		<HTML>
			<HEAD>
				<TITLE></TITLE>
			</HEAD>
			<BODY>
				<H1><xsl:value-of select="document('report-data.xml')/report-data/text[@id='kh']/text()"/></H1>
				<xsl:value-of select="document('report-data.xml')/report-data/text[@id='bereich']/text()"/><br/> 
				<xsl:value-of select="document('report-data.xml')/report-data/text[@id='leitung']/text()"/><br/>
				<xsl:value-of select="document('report-data.xml')/report-data/text[@id='monat']/text()"/><br/> 
				<xsl:value-of select="document('report-data.xml')/report-data/text[@id='stand']/text()"/><br/> 
				<xsl:call-template name="genTOC"/>
				<xsl:apply-templates select="./section"/>
			</BODY>
		</HTML>
	</xsl:template>
	
	<xsl:attribute-set name="table">
			<xsl:attribute name="border">1</xsl:attribute>
			<xsl:attribute name="style">margin-top:10</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="textheaderformat">
			<xsl:attribute name="style">margin-top:30;font-size:15pt;font-weight:bold</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="subheaderformat">
			<xsl:attribute name="font-size">14pt</xsl:attribute>
			<xsl:attribute name="space-before">30pt</xsl:attribute>
  </xsl:attribute-set>
  <xsl:attribute-set name="plotformat">
			<xsl:attribute name="content-width">12cm</xsl:attribute>
			<xsl:attribute name="content-height">7cm</xsl:attribute>
  </xsl:attribute-set>
	
	<xsl:template name="genTOC">
	<h2>TABLE OF CONTENTS</h2>
		<xsl:for-each select="//header">
		<a href="#{generate-id(.)}"><xsl:value-of select="./text()"/></a><br/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="section">
		<xsl:apply-templates select="./*"/>
 </xsl:template>

 <xsl:template match="header">
			<div xsl:use-attribute-sets="textheaderformat" id="{generate-id(.)}">
				<xsl:value-of select="./text()"/><br/>
			</div>
 </xsl:template>
 
 <xsl:template match="paragraph">
	<xsl:apply-templates select="./text() | var"/><br/>
 </xsl:template>
 
 <xsl:template match="var">
	<xsl:variable name="id" select="@ref"/>
	<xsl:value-of select="document('report-data.xml')/report-data/text[@id=$id]/text()"/>
 </xsl:template>

 <xsl:template match="plot">
	<xsl:variable name="id" select="@ref"/>
	<xsl:variable name="href" select="document('report-data.xml')/report-data/plot[@id=$id]/@href"/>
	<figure>
		<img style="margin-top:10">
			<xsl:attribute name="src"><xsl:value-of select="$href"/></xsl:attribute>
			<xsl:attribute name="alt"></xsl:attribute>
			<xsl:attribute name="height">400</xsl:attribute>
			<xsl:attribute name="width">400</xsl:attribute>
		</img>
		<figcaption>
			<xsl:value-of select="./text()"/>
		</figcaption>
	</figure>
 </xsl:template>
 
 <xsl:template match="table">
	<xsl:variable name="id" select="@ref"/>
    <xsl:variable name="href" select="document('report-data.xml')/report-data/table[@id=$id]/@href"/>
    <xsl:apply-templates select="document($href)"/>
    <xsl:value-of select="./text()"/>
 </xsl:template>


	<xsl:template match="xhtml:table">
		<table xsl:use-attribute-sets="table">
			<thead>
				<xsl:apply-templates select="xhtml:thead/xhtml:tr"/>
			</thead>
			<tbody>
				<xsl:apply-templates select="xhtml:tbody/xhtml:tr"/>
			</tbody>
		</table>
	</xsl:template>
   
	<xsl:template match="xhtml:thead/xhtml:tr">
		<tr>
			<xsl:apply-templates select="xhtml:th"/>
		</tr>
	</xsl:template>
   
	<xsl:template match="xhtml:th">
		<th>
			<xsl:value-of select="./text()"/>
		</th>
	</xsl:template>
  
	<xsl:template match="xhtml:td">
		<td>
			<xsl:value-of select="./text()" />
		</td>
	</xsl:template>
	
	<xsl:template match="xhtml:tr">
		<tr>
			<xsl:apply-templates select="xhtml:td"/>
		</tr>
	</xsl:template>
</xsl:stylesheet>
