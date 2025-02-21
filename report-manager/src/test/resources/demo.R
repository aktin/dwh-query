library(lattice)

report_generated_files <- function(name){
	cat(paste(name,"\n",sep=""), file="r-generated-files.txt", append=TRUE)
}

options(encoding = "UTF-8")
source("include.R")

## oder source("include.R", encoding="UTF-8")

