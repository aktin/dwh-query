#!/usr/bin/Rscript
#
# Description: Create XHTML for tabular data
# Author: R.W.Majeed
# Last-Modified: 2016-01-10
#

#library(XML)
xml.escape <- function(x){
	x <- gsub(pattern='&', replacement='&amp;', x=x)
	x <- gsub(pattern='<', replacement='&lt;', x=x)
	x <- gsub(pattern='>', replacement='&gt;', x=x)
	return(x)
}

#' Create XHTML for tabular data
#'
#' @param x Table data. can be of class data.frame or table
#' @param file File name where the table should be written. If '', stdout is used.
#' @param widths Widths in percent. Calculated equally spaced if null.
#' @param align Column alignment. If 'auto' (default) then automatically calculated by column class. Can be explicitly specified as vector of 'right','left','center'.
#' @param align.default alignment to use for non-numeric columns in auto align
#' @param na.subst substitution for NA,NaN,Inf,-Inf values
#' @examples
#' xhtml_table(OrchardSprays)
xhtml_table <- function(x, file='', widths=NULL, align='auto', align.default='left', na.subst=''){
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

	#Output XML-Header
	fd <- file(file, open="wt", encoding="UTF-8")
	cat('<?xml version="1.0" encoding="UTF-8"?>\n<table xmlns="http://www.w3.org/1999/xhtml">\n',file=fd,append=FALSE)
  
	for (i in 1:length(names(x))) {
		#newXMLNode("col", attrs = c(align = align[i],width=paste0(widths[i],'%')), parent=e)
		cat('\t<col align="',align[i],'" width="',widths[i],'%"/>\n',sep='',file=fd,append=TRUE)
	}
	cat("\t<thead>\n\t\t<tr>\n",file=fd,append=TRUE)
	#h <- newXMLNode("thead", parent=e)
	#tr <- newXMLNode("tr", parent=h)
	for( i in names(x) ){
		#newXMLNode(name="th", text=i, parent=tr)
		cat("\t\t\t<th>",xml.escape(i),"</th>\n", sep='',file=fd,append=TRUE)
	}
	cat('\t\t</tr>\n\t</thead>\n',file=fd,append=TRUE)
	#b <- newXMLNode("tbody", parent=e)
	cat("\t<tbody>\n",file=fd,append=TRUE)
	if( length(dim(x)) == 1 ){
		# only one dimension / row
		#tr <- newXMLNode("tr", parent=b)
		cat('\t\t<tr>',file=fd,append=TRUE)
		for( i in x ){
			if( is.na(i) | is.nan(i) | is.infinite(i)){
				i <- na.subst
			}
			#newXMLNode(name="td", text=i, parent=tr)
			cat('\t\t\t<td>',xml.escape(i),'</td>\n', sep='',file=fd,append=TRUE)
		}
		cat('\t\t</tr>\n',file=fd,append=TRUE)
	}else{
		# multiple rows
		for( r in 1:nrow(x) ){
			#tr <- newXMLNode("tr", parent=b)
			cat("\t\t<tr>\n",file=fd,append=TRUE)
			for( i in x[r,] ){
			  if( is.na(i) | is.nan(i) | is.infinite(i)){
					i <- na.subst
				}
				#newXMLNode(name="td", text=i, parent=tr)
				cat('\t\t\t<td>',xml.escape(i),'</td>\n', sep='',file=fd,append=TRUE)
			}
			cat('\t\t</tr>\n',file=fd,append=TRUE)
		}
	}
	cat('\t</tbody>\n</table>',file=fd,append=TRUE)
	close(fd);
}