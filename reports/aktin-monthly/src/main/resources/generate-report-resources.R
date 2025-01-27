#!/usr/bin/Rscript
library(ggplot2)
library(lattice)

options(OutDec = ",")

#/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/

enc <- read.table(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/encounters.txt",
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

pat <- read.table(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/patients.txt",
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

diag <- read.table(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/diagnoses.txt",
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  stringsAsFactors = FALSE,
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

cedis <- read.csv2(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/CEDIS.csv",
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

icd <- read.csv2(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/ICD-3Steller.csv",
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

factors <- read.csv2(
  file = "/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/factors.csv",
  as.is = TRUE, na.strings = "",
  header = TRUE, sep = ";",
  encoding = "UTF-8",
  comment.char = ""
)

source("/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/helper.R", encoding = "UTF-8", echo = FALSE)
source("/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/parse_derive.R", encoding = "UTF-8")
source("/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/xhtml-table.R")
source("/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/localisation.R")

for (i in 1:9) {
  source(paste0("/Users/AKTIN/Documents/git/dwh-query/reports/aktin-monthly/src/main/resources/chapter", i, ".R"), encoding = "UTF-8")
}
