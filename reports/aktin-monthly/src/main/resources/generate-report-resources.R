#!/usr/bin/Rscript
library(ggplot2)
library(lattice)

options(OutDec = ",")

enc <- read.table(
  file = "encounters.txt",
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

pat <- read.table(
  file = "patients.txt",
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

diag <- read.table(
  file = "diagnoses.txt",
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
  file = "CEDIS.csv",
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

icd <- read.csv2(
  file = "ICD-3Steller.csv",
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

factors <- read.csv2(
  file = "factors.csv",
  as.is = TRUE, na.strings = "",
  header = TRUE, sep = ";",
  encoding = "UTF-8",
  comment.char = ""
)

source("helper.R", encoding = "UTF-8", echo = FALSE)
source("parse_derive.R", encoding = "UTF-8")
source("xhtml-table.R")
source("localisation.R")

for (i in 1:9) {
  source(paste0("chapter", i, ".R"), encoding = "UTF-8")
}
