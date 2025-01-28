<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:fo="http://www.w3.org/1999/XSL/Format" 
	version="1.0">
	<xsl:template match="/template">
		<xsl:variable name="colpos" select="count(preceding-sibling::xhtml:col)"/>
		<xsl:variable name="datapos" select="count(preceding-sibling::xhtml:td)"/>
		<fo:root font-family="Helvetica">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="page-layout" page-height="297mm"
					page-width="210mm"
					margin-top="10mm">
					<fo:region-body region-name="body"
                    margin-top="10mm" margin-bottom="20mm"
                    margin-left="25mm" margin-right="25mm"/>
					<fo:region-before region-name="header-normal" extent="15mm"/>
					<fo:region-after region-name="footer-normal" extent="10mm"/>
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:declarations>
				<x:xmpmeta xmlns:x="adobe:ns:meta/">
					<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
						<rdf:Description rdf:about="" xmlns:pdf="http://ns.adobe.com/pdf/1.3/"
							xmlns:xmp="http://ns.adobe.com/xap/1.0/"
							xmlns:dc="http://purl.org/dc/elements/1.1/">
							<dc:title>fo-report</dc:title>
							<dc:creator>generated by AKTIN</dc:creator>
							<dc:description>Notaufnahmebericht</dc:description>
							<pdf:Keywords>AKTIN</pdf:Keywords>
							<xmp:CreatorTool>erstellt mit Apache FOP</xmp:CreatorTool>
							<xmp:CreationDate>2015-12-05T08:15:30-02:00</xmp:CreationDate> <!-- macht wohl nichts, Datum wird automatisch auf die aktuelle Zeit gesetzt -->
							<xmp:ModifyDate>2017-07-28T18:25:30-02:00</xmp:ModifyDate>
						</rdf:Description>
					</rdf:RDF>
				</x:xmpmeta>
			</fo:declarations>
			<fo:page-sequence master-reference="page-layout" language="de">
                <fo:static-content flow-name="header-normal">
                	<fo:block xsl:use-attribute-sets="headerfooter">AKTIN Monatsbericht V01.5 - <xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'local.o']/text()"/> - <xsl:call-template name="zeitraum"><xsl:with-param name="start" select="document('prefs.xml')/properties/entry[@key = 'report.data.start']/text()"/><xsl:with-param name="end" select="document('prefs.xml')/properties/entry[@key = 'report.data.end']/text()"/></xsl:call-template></fo:block>
				</fo:static-content>
				<fo:static-content flow-name="footer-normal">
					<fo:block xsl:use-attribute-sets="headerfooter">Seite <fo:page-number/> von
							<fo:page-number-citation-last ref-id="end"/></fo:block>
				</fo:static-content>
				<fo:flow flow-name="body" id="mybody">
					<fo:block xsl:use-attribute-sets="headerformat">
						<xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'local.o']/text()"/>
					</fo:block>
					<fo:block>
						<xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'local.ou']/text()"/>
					</fo:block>
					<fo:block>
						Monatsbericht: <xsl:call-template name="zeitraum"><xsl:with-param name="start" select="document('prefs.xml')/properties/entry[@key = 'report.data.start']/text()"/><xsl:with-param name="end" select="document('prefs.xml')/properties/entry[@key = 'report.data.end']/text()"/></xsl:call-template>
					</fo:block>
					<fo:block>
						Datenstand: <xsl:call-template name="datenstand"><xsl:with-param name="timestamp" select="document('prefs.xml')/properties/entry[@key = 'report.data.timestamp']/text()"/></xsl:call-template>
					</fo:block>
					<fo:block space-before="75mm">Das AKTIN-Notaufnahmeregister – Daten 
					für die Qualitätssicherung,Public-Health Surveillance und Versorgungsforschung 
					in der Akutmedizin</fo:block>
					<fo:block space-before="10mm">
						<fo:external-graphic>
							<xsl:attribute name="src">Notaufnahmeregister_Logo_2021.svg</xsl:attribute>
							<xsl:attribute name="content-height">30mm</xsl:attribute>
						</fo:external-graphic>
					</fo:block>
					<fo:block space-before="25mm"></fo:block>
					    <fo:block space-before="5mm">
							<!-- Create a table to align the images side by side -->
							<fo:table table-layout="fixed" width="100%">
									<fo:table-column column-width="auto"/>
									<fo:table-column column-width="proportional-column-width(1)"/>
									<fo:table-body>
											<fo:table-row>
													<!-- BMBF Logo -->
													<fo:table-cell text-align="left">
															<fo:block>
																	<fo:external-graphic>
																			<xsl:attribute name="src">BMBF.svg</xsl:attribute>
																			<xsl:attribute name="content-height">30mm</xsl:attribute>
																			<xsl:attribute name="content-width">scale-to-fit</xsl:attribute>
																	</fo:external-graphic>
															</fo:block>
													</fo:table-cell>
													<!-- NUM Logo -->
													<fo:table-cell text-align="left">
															<fo:block>
																	<fo:external-graphic>
																			<xsl:attribute name="src">NUM.svg</xsl:attribute>
																			<xsl:attribute name="content-height">30mm</xsl:attribute>
																			<xsl:attribute name="content-width">scale-to-fit</xsl:attribute>
																	</fo:external-graphic>
															</fo:block>
													</fo:table-cell>
											</fo:table-row>
											<!-- Second row: Sentence below the images -->
											<fo:table-row>
													<fo:table-cell number-columns-spanned="2">
															<fo:block text-align="left" space-before="5mm" font-size="10pt">
																	Das AKTIN-Notaufnahmeregister wird gefördert durch das Bundesministerium für Bildung und Forschung (BMBF) im Rahmen des Netzwerks Universitätsmedizin 2.0: "NUM 2.0", Nr. 01KX2121, Projekt: AKTIN@NUM.
															</fo:block>
													</fo:table-cell>
											</fo:table-row>
									</fo:table-body>
							</fo:table>
					</fo:block>
					<xsl:apply-templates select="./intro"/>
					<xsl:call-template name="genTOC"/>
					<xsl:apply-templates select="./section"/>
					<fo:block id="end"/>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>

	<xsl:attribute-set name="table">
		<xsl:attribute name="border-bottom-style">solid</xsl:attribute>
		<xsl:attribute name="border-collapse">collapse</xsl:attribute>
		<xsl:attribute name="space-before">10pt</xsl:attribute>
		<xsl:attribute name="keep-together">always</xsl:attribute>
		<xsl:attribute name="table-layout">fixed</xsl:attribute>
		<xsl:attribute name="width">100%</xsl:attribute>
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
		<xsl:attribute name="hyphenate">false</xsl:attribute>
		<!-- <xsl:attribute name="hyphenation-remain-character-count">4</xsl:attribute> 
		<xsl:attribute name="xml:lang">de</xsl:attribute> -->
	</xsl:attribute-set>
	<xsl:attribute-set name="headerformat">
		<xsl:attribute name="font-size">20pt</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<!--<xsl:attribute name="font-family">Courier</xsl:attribute>-->
		<xsl:attribute name="text-align">center</xsl:attribute>
		<xsl:attribute name="space-before">30pt</xsl:attribute>
		<xsl:attribute name="space-after">30pt</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="textheaderformat"> <!-- Überschriften im Fließtext -->
		<xsl:attribute name="font-size">15pt</xsl:attribute>
		<xsl:attribute name="font-weight">bold</xsl:attribute>
		<xsl:attribute name="page-break-before">always</xsl:attribute>
		<xsl:attribute name="space-after">10pt</xsl:attribute>
		<xsl:attribute name="keep-with-next">always</xsl:attribute>		
	</xsl:attribute-set>
	<xsl:attribute-set name="subheaderformat">
		<xsl:attribute name="font-size">14pt</xsl:attribute>
		<xsl:attribute name="space-before">20pt</xsl:attribute>
		<xsl:attribute name="keep-with-next">always</xsl:attribute>
	</xsl:attribute-set>
	<xsl:attribute-set name="plotformat">
		<xsl:attribute name="inline-progression-dimension">100%</xsl:attribute>
		<xsl:attribute name="content-height">scale-to-fit</xsl:attribute>
		<xsl:attribute name="content-width">scale-to-fit</xsl:attribute>
		<xsl:attribute name="space-before">0pt</xsl:attribute>
		<xsl:attribute name="space-before.precedence">force</xsl:attribute> <!-- Plot haben sonst zu viel Abstand -->
	</xsl:attribute-set>
    <xsl:attribute-set name="headerfooter">
		<xsl:attribute name="text-align">center</xsl:attribute>
		<xsl:attribute name="font-size">9pt</xsl:attribute>
		<xsl:attribute name="font-style">italic</xsl:attribute>
	</xsl:attribute-set>

	<xsl:template name="genTOC">
		<fo:block break-before="page" break-after="page">
			<fo:block xsl:use-attribute-sets="headerformat">Inhaltsverzeichnis</fo:block>
			<xsl:for-each select="//section">
				<fo:block text-align-last="justify">
					<xsl:variable name="anzahl" select="count(ancestor::*)"/>
					<xsl:choose>
						<xsl:when test="$anzahl = 1">
							<xsl:variable name="chapNum">
								<xsl:number from="CHAPTER" count="section" format="1 "
									level="multiple"/>
							</xsl:variable>
							<fo:block font-weight="bold">
								<fo:basic-link internal-destination="{generate-id(.)}">
									<xsl:value-of select="$chapNum"/>
									<xsl:value-of select="./header/text()"/>
									<fo:leader leader-pattern="dots"/>
									<fo:page-number-citation ref-id="{generate-id(.)}"/>
								</fo:basic-link>
							</fo:block>
						</xsl:when>
						<xsl:otherwise>
							<xsl:variable name="chapNum">
								<xsl:number from="CHAPTER" count="section" format="1 "
									level="multiple"/>
							</xsl:variable>
							<fo:block text-indent="10mm">
								<fo:basic-link internal-destination="{generate-id(.)}">
									<xsl:value-of select="$chapNum"/>
									<xsl:value-of select="./header/text()"/>
									<fo:leader leader-pattern="dots"/>
									<fo:page-number-citation ref-id="{generate-id(.)}"/>
								</fo:basic-link>
							</fo:block>
						</xsl:otherwise>
					</xsl:choose>
				</fo:block>
			</xsl:for-each>
		</fo:block>
	</xsl:template>
	
    <xsl:template match="intro">
		<fo:block padding-top="40mm" linefeed-treatment="preserve" break-before="page" break-after="page">
			<xsl:apply-templates select="text()"/>
		</fo:block>
	</xsl:template>

	<xsl:template match="section">
		<fo:block id="{generate-id(.)}">
			<xsl:apply-templates select="./*"/>
		</fo:block>
	</xsl:template>

	<xsl:template match="header">
		<xsl:variable name="stufe" select="count(ancestor::*)"/>
		<xsl:choose>
			<xsl:when test="$stufe = 2">
				<xsl:variable name="chapNum">
					<xsl:number from="CHAPTER" count="section" format="1 " level="multiple"/>
				</xsl:variable>
				<fo:block xsl:use-attribute-sets="textheaderformat">
					<xsl:value-of select="$chapNum"/>
					<xsl:value-of select="./text()"/>
				</fo:block>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="chapNum">
					<xsl:number from="CHAPTER" count="section" format="1 " level="multiple"/>
				</xsl:variable>
				<fo:block xsl:use-attribute-sets="subheaderformat">
					<xsl:if test="@break-before='manual'">
						<xsl:attribute name="page-break-before">always</xsl:attribute>
					</xsl:if>
					<xsl:value-of select="$chapNum"/>
					<xsl:value-of select="./text()"/>
				</fo:block>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="paragraph">
		<fo:block>
			<xsl:apply-templates select="text() | var | pref"/>
		</fo:block>
	</xsl:template>

	<xsl:template match="var">
		<xsl:variable name="id" select="@ref"/>
		<xsl:value-of select="document('report-data.xml')/report-data/text[@id = $id]/text()"/>
	</xsl:template>

	<xsl:template match="plot">
        <xsl:variable name="id" select="@ref"/>
		<xsl:variable name="href" select="document('report-data.xml')/report-data/plot[@id = $id]/@href"/>
        <!--  should work to avoid errors with missing files, but check on $href always returns false => path problem?
        <xsl:when test="fs:exists(fs:new($href))" xmlns:fs="java.io.File">
        </xsl:when>
        <xsl:otherwise>
         do something here... default image
        </xsl:otherwise>-->
		<fo:block font-size="0">
			<fo:external-graphic xsl:use-attribute-sets="plotformat">
				<xsl:attribute name="src">
					<xsl:value-of select="$href"/>
				</xsl:attribute>
			</fo:external-graphic>
			<fo:block font-size="9pt" keep-with-previous="always" space-after="10mm">
				<xsl:value-of select="./text()"/>
			</fo:block>
		</fo:block>
	</xsl:template>

	<xsl:template match="table">
		<xsl:variable name="id" select="@ref"/>
		<xsl:variable name="href"
			select="document('report-data.xml')/report-data/table[@id = $id]/@href"/>
		<xsl:apply-templates select="document($href)"/>
		<fo:block font-size="9pt" keep-with-previous="always" space-before="3mm" space-after="3mm">
			<xsl:value-of select="./text()"/>
		</fo:block>
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
			<xsl:attribute name="column-width">
				<xsl:value-of select="@width"/>
			</xsl:attribute>
			<xsl:attribute name="text-align">
				<xsl:value-of select="@align"/>
			</xsl:attribute>
		</fo:table-column>
	</xsl:template>

	<xsl:template match="xhtml:thead/xhtml:tr">
		<fo:table-row xsl:use-attribute-sets="cellborder">
			<xsl:apply-templates select="xhtml:th"/>
		</fo:table-row>
	</xsl:template>

	<xsl:template match="xhtml:th">
		<fo:table-cell xsl:use-attribute-sets="tablehead">
			<fo:block>
				<xsl:value-of select="./text()"/>
			</fo:block>
		</fo:table-cell>
	</xsl:template>

	<xsl:template match="xhtml:td">
		<xsl:variable name="p" select="position()"/>
		<fo:table-cell xsl:use-attribute-sets="cellborder">
			<xsl:attribute name="text-align">
				<xsl:value-of select="../../../xhtml:col[$p]/@align"/>
			</xsl:attribute>
			<fo:block-container overflow="hidden">
                <fo:block xsl:use-attribute-sets="celltext">
                    <xsl:value-of select="./text()"/>
                </fo:block>
            </fo:block-container>
		</fo:table-cell>
	</xsl:template>

	<xsl:template match="xhtml:tr">
		<fo:table-row>
			<xsl:apply-templates select="xhtml:td"/>
		</fo:table-row>
	</xsl:template>
	
	<xsl:template name="zeitraum">  <!-- todo: not done this might be interesting: <xsl:value-of select="functx:first-day-of-month($start)"/> -->
        <xsl:param name="start"></xsl:param>
        <xsl:param name="end"></xsl:param>
		<xsl:variable name="start_year" select="substring-before($start, '-')" />
		<xsl:variable name="start_month" select="substring-before(substring-after($start, '-'), '-')" />
		<xsl:variable name="start_day" select="substring-before(substring-after(substring-after($start, '-'), '-'),'T')" />
		<xsl:variable name="end_year" select="substring-before($end, '-')" />
		<xsl:variable name="end_month" select="substring-before(substring-after($end, '-'), '-')" />
		<xsl:variable name="end_day" select="substring-before(substring-after(substring-after($end, '-'), '-'),'T')" />
		<xsl:choose>
			<xsl:when test="(number($start_day)=1)and(number($end_day)=1)		and		((number($start_month)=number($end_month)-1) and (number($start_year)=number($end_year))		or		(number($start_month)=12) and (number($end_month=1)) and (number($start_year)=number($end_year)-1))">
				<xsl:choose>
					<xsl:when test="$start_month = '1' or $start_month= '01'"><xsl:value-of select="concat('Januar', ' ', $start_year)"/></xsl:when>
					<xsl:when test="$start_month = '2' or $start_month= '02'"><xsl:value-of select="concat('Februar', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '3' or $start_month= '03'"><xsl:value-of select="concat('März', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '4' or $start_month= '04'"><xsl:value-of select="concat('April', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '5' or $start_month= '05'"><xsl:value-of select="concat('Mai', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '6' or $start_month= '06'"><xsl:value-of select="concat('Juni', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '7' or $start_month= '07'"><xsl:value-of select="concat('Juli', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '8' or $start_month= '08'"><xsl:value-of select="concat('August', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '9' or $start_month= '09'"><xsl:value-of select="concat('September', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '10'"><xsl:value-of select="concat('Oktober', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '11'"><xsl:value-of select="concat('November', ' ', $start_year)"/> </xsl:when>
					<xsl:when test="$start_month= '12'"><xsl:value-of select="concat('Dezember', ' ', $start_year)"/> </xsl:when>
				</xsl:choose>
			</xsl:when>			
			<!--<xsl:when test="">
			</xsl:when>	-->
			<xsl:otherwise>
				<xsl:value-of select="concat($start_day, '.', $start_month, '.', $start_year,' - ',$end_day, '.', $end_month, '.', $end_year)"/> <!-- default output -->
			</xsl:otherwise>
		</xsl:choose>
    </xsl:template>
	
	<xsl:template name="datenstand"> 
		<xsl:param name="timestamp"></xsl:param>
		<xsl:variable name="year" select="substring-before($timestamp, '-')" />
		<xsl:variable name="month" select="substring-before(substring-after($timestamp, '-'), '-')" />
		<xsl:variable name="day" select="substring-before(substring-after(substring-after($timestamp, '-'), '-'),'T')" />
		<xsl:value-of select="concat($day, '.', $month, '.', $year)"/>	
	</xsl:template>
	
	<xsl:template match="pref[@ref='zeitraum']">
		<xsl:call-template name="zeitraum"><xsl:with-param name="start" select="document('prefs.xml')/properties/entry[@key = 'report.data.start']/text()"/><xsl:with-param name="end" select="document('prefs.xml')/properties/entry[@key = 'report.data.end']/text()"/></xsl:call-template>
	</xsl:template>
	
	<xsl:template match="pref[@ref='patients']">
		<xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'report.data.patients']/text()"/>
	</xsl:template>
	
    <xsl:template match="pref[@ref='encounters']">
		<xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'report.data.encounters']/text()"/>
	</xsl:template>

</xsl:stylesheet>
