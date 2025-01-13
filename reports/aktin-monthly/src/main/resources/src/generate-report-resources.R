#!/usr/bin/Rscript
library(ggplot2)
library(lattice)

options(OutDec = ",")

base_dir <- "reports/aktin-monthly/src/main/resources"
data_dir <- file.path(base_dir, "data")
src_dir <- file.path(base_dir, "src")
chapters_dir <- file.path(src_dir, "chapters")

enc <- read.table(
  file = file.path(data_dir, "encounters.txt"),
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

pat <- read.table(
  file = file.path(data_dir, "patients.txt"),
  header = TRUE,
  sep = "\t",
  as.is = TRUE,
  na.strings = "",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

diag <- read.table(
  file = file.path(data_dir, "diagnoses.txt"),
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
  file = file.path(data_dir, "CEDIS.csv"),
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

icd <- read.csv2(
  file = file.path(data_dir, "ICD-3Steller.csv"),
  as.is = TRUE, na.strings = "",
  header = FALSE, sep = ";",
  colClasses = "character",
  encoding = "UTF-8",
  comment.char = ""
)

factors <- read.csv2(
  file = file.path(data_dir, "factors.csv"),
  as.is = TRUE, na.strings = "",
  header = TRUE, sep = ";",
  encoding = "UTF-8",
  comment.char = ""
)

source(file.path(src_dir, "helper.R"), encoding = "UTF-8", echo = FALSE)
source(file.path(src_dir, "parse_derive.R"), encoding = "UTF-8")
source(file.path(src_dir, "xhtml-table.R"))
source(file.path(src_dir, "localisation.R"))

for (i in 1:9) {
  source(file.path(chapters_dir, paste0("chapter", i, ".R")), encoding = "UTF-8")
}
