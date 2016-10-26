<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xhtml="http://www.w3.org/1999/xhtml" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	version="1.0">
	<xsl:template match="/">
		<xsl:variable name="colpos" select="count(preceding-sibling::xhtml:col)" />
		<xsl:variable name="datapos" select="count(preceding-sibling::xhtml:td)" />
		<fo:root>
			<fo:layout-master-set>
				<fo:simple-page-master master-name="page-layout"
					page-height="297mm" page-width="210mm">
					<fo:region-body margin="1in" region-name="body" />
					<fo:region-after region-name="footer-normal"
						extent="20mm" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:declarations>
				<x:xmpmeta xmlns:x="adobe:ns:meta/">
					<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
						<rdf:Description rdf:about=""
							xmlns:pdf="http://ns.adobe.com/pdf/1.3/" xmlns:xmp="http://ns.adobe.com/xap/1.0/"
							xmlns:dc="http://purl.org/dc/elements/1.1/">
							<dc:title>fo-report</dc:title>
							<dc:creator>Creator</dc:creator>
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
				<fo:flow flow-name="body" id="mybody">
					<fo:block>
						Krankenhaus <xsl:value-of select="document('prefs.xml')/properties/entry[@key = 'local.ou']/text()"/>
					</fo:block>
					<fo:block>lalala</fo:block>
					<fo:block space-before="200pt">
						Verbesserung der Versorgungsforschung in der Akutmedizin in Deutschland durch
						den Aufbau eines nationalen Notaufnahmeregisters
					</fo:block>
					<fo:block space-before="50pt">
						Förderkennzeichen: 01KX1319B
					</fo:block>
					<fo:block space-before="50pt">
						SVG: <fo:external-graphic src="barchart1.svg" content-height="30mm"/>
					</fo:block>
					<fo:block space-before="50pt">
						PNG: <fo:external-graphic src="barchart1.png" content-height="30mm"/>
					</fo:block>
					<fo:block id="end" />
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>

</xsl:stylesheet>
