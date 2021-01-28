#!/usr/bin/Rscript

report.generatedFile <- function(name){
  cat(paste(name,"\n",sep=""), file="r-generated-files.txt", append=TRUE)
}
gfx.dir <- ''
gfx.ext <- '.svg'
gfx.dev <- 'svg'
report.svg <- function(graph, name, width=8, height=4){
  trellis.device(gfx.dev,file=paste0(gfx.dir,name,gfx.ext), width=width,height=height)
  print(graph)
  no_output <- dev.off() #silent
  report.generatedFile(paste0(name,gfx.ext))
}
xml.dir <- ''
report.table <- function(x, name, widths=NULL, align='auto', align.default='left', na.subst=''){
  xhtml.table(x, file=paste0(xml.dir, name), widths=widths, align=align, align.default=align.default, na.subst=na.subst)
  report.generatedFile(name)
}

std_cols1 <- c("firebrick3")
std_cols3 <- c("firebrick3","dodgerblue","forestgreen")
std_cols5 <- c("firebrick3","orange","yellow","forestgreen","dodgerblue","aliceblue")

#std_cols3 <- c("firebrick3","blue","green")
#std_cols5 <- c("firebrick3","orange","yellow","green","blue","white")

round_df <- function(df, digits) {
  nums <- vapply(df, is.numeric, FUN.VALUE = logical(1))
  
  df[,nums] <- round(df[,nums], digits = digits)
  
  (df)
}

options(OutDec= ",")

gformat <- function(num,digits=0) {
  prettyOut <- format(round(as.numeric(num), digits), nsmall=digits, big.mark=".")
  (prettyOut)
}

stdabw <- function(x) {n=length(x) ; sqrt(var(x) * (n-1) / n)}

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
#tmp = read.csv2(file='Daten_ZNA_Entenhausen.csv', as.is=TRUE, na.strings='')
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='', colClasses = "character", encoding = "UTF-8",comment.char="")
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='', colClasses = "character", encoding = "UTF-8",comment.char="")
diag <- read.table(file='diagnoses.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='',stringsAsFactors=FALSE, colClasses = "character", encoding = "UTF-8",comment.char="")


# create new data frame for encounter data to contain clean values
df = data.frame(patient=enc$patient_id, encounter=enc$encounter_id)

# create new data frame for diagnoses data to contain clean values
df_diag <- data.frame(diagnosis=as.factor(substring(diag$diagnose_code, first=1, last=3)))

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';', colClasses = "character", encoding = "UTF-8",comment.char="")

#load ICD mapping table
icd = read.csv2(file='ICD-3Steller.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';', colClasses = "character", encoding = "UTF-8",comment.char="")

#load special factors (problems with umlauts if umlauts are placed in factor texts directly in R file)
factors = read.csv2(file='factors.csv', as.is=TRUE, na.strings='', header = TRUE, sep=';', encoding = "UTF-8",comment.char="")

source("parse_derive.R", encoding="UTF-8")

# XHTML tables
source('xhtml-table.R')

# Graphics & Plots
library(lattice)
library(tidyverse)

source("chapter1.R", encoding="UTF-8")
source("chapter2.R", encoding="UTF-8")
source("chapter3.R", encoding="UTF-8")
source("chapter4.R", encoding="UTF-8")
source("chapter5.R", encoding="UTF-8")
source("chapter6.R", encoding="UTF-8")
source("chapter7.R", encoding="UTF-8")
source("chapter8.R", encoding="UTF-8")
source("chapter9.R", encoding="UTF-8")
