#!/usr/bin/Rscript
#
# Description: Create XHTML for tabular data
# Author: R.W.Majeed
# Last-Modified: 2016-01-10
#

library(XML)

xhtml.table <- function(x, file=NULL, widths=NULL){
	e <- newXMLNode("table",namespaceDefinitions="http://www.w3.org/1999/xhtml")
	# TODO detect column type and set align and width automatically
	if( is.null(widths) ){
		widths <- round(100/length(names(x)))
	}
	if( length(widths) == 1 ){
		widths <- rep(widths, times=length(names(x)))
	}else if( length(widths) != length(names(x)) ){
		stop('length of widths does not match length of names(x)')
	}
	
	for (i in 1:length(names(x))) {
		newXMLNode("col", attrs = c(align = "center",width=paste0(widths[i],'%')), parent=e)
	}
	h <- newXMLNode("thead", parent=e)
	tr <- newXMLNode("tr", parent=h)
	for( i in names(x) ){
		newXMLNode(name="th", text=i, parent=tr)
	}
	b <- newXMLNode("tbody", parent=e)
	# TODO allow more dimensions ==> multiple rows
	tr <- newXMLNode("tr", parent=b)
	for( i in x ){
		newXMLNode(name="td", text=i, parent=tr)
	}
	
	doc = newXMLDoc(node=e)
	if( !is.null(file) ){
		saveXML(doc, file=file, indent=TRUE)
	}else{
		return(doc)
	}
}

