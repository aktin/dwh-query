#!/usr/bin/Rscript
#
# Description: Create XHTML for tabular data
# Author: R.W.Majeed
# Last-Modified: 2016-01-10
#

library(XML)

#' Create XHTML for tabular data
#'
#' @param x Table data. can be of class data.frame or table
#' @param file File name where the table should be written. If null, stdout is used.
#' @param widths Widths in percent. Calculated equally spaced if null.
#' @param align Column alignment. If 'auto' (default) then automatically calculated by column class. Can be explicitly specified as vector of 'right','left','center'.
#' @param align.default alignment to use for non-numeric columns in auto align
#' @param na.subst substitution for NA,NaN,Inf,-Inf values
#' @examples
#' xhtml.table(OrchardSprays)
xhtml.table <- function(x, file=NULL, widths=NULL, align='auto', align.default='left', na.subst=''){
	e <- newXMLNode("table",namespaceDefinitions="http://www.w3.org/1999/xhtml")
	# equally spaced widths
	if( is.null(widths) ){
		widths <- round(100/length(names(x)))
	}
	if( length(widths) == 1 ){
		widths <- rep(widths, times=length(names(x)))
	}else if( length(widths) != length(names(x)) ){
		stop('length of widths does not match length of names(x)')
	}

	# alignment of data rows
	if( length(align) == 1 ){
		if( align == 'auto' ){
			align <- rep(align.default, times=length(names(x)))
			for( i in 1:length(names(x)) ){
				if( 'numeric' == class(x[[i]]) ){
					align[i] <- 'right'
				}
			}
		}else{
			# repeat align for all columns
			align <- rep(align, times=length(names(x)))
		}
	}else if( length(align) != length(names(x)) ){
		stop('length of align does not match length of names(x)')
	}

	for (i in 1:length(names(x))) {
		newXMLNode("col", attrs = c(align = align[i],width=paste0(widths[i],'%')), parent=e)
	}
	h <- newXMLNode("thead", parent=e)
	tr <- newXMLNode("tr", parent=h)
	for( i in names(x) ){
		newXMLNode(name="th", text=i, parent=tr)
	}
	b <- newXMLNode("tbody", parent=e)
	if( length(dim(x)) == 1 ){
		# only one dimension / row
		tr <- newXMLNode("tr", parent=b)
		for( i in x ){
			if( is.na(i) | is.nan(i) | is.infinite(i)){
				i <- na.subst
			}
			newXMLNode(name="td", text=i, parent=tr)
		}
	}else{
		# multiple rows
		for( r in 1:nrow(x) ){
			tr <- newXMLNode("tr", parent=b)
			for( i in x[r,] ){
			  if( is.na(i) | is.nan(i) | is.infinite(i)){
					i <- na.subst
				}
				newXMLNode(name="td", text=i, parent=tr)
			}
		}
	}
	doc = newXMLDoc(node=e)
	if( !is.null(file) ){
		# test supported encodings
		no_output <- saveXML(doc, file=file, encoding = "utf-8", indent=TRUE) #silent
	}else{
		return(doc)
	}
}