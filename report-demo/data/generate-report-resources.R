#!/usr/bin/Rscript

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
tmp = read.csv2(file='export_for_reporting.csv', as.is=TRUE, na.strings='')

# create new data frame to contain clean values
df = data.frame(patient=tmp$patient_num, encounter=tmp$encounter_num)

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(tmp$birth_date,format="%F %H:%M:%S")
df$triage.ts = strptime(tmp$zeitpunkttriage,format="%F %H:%M:%S")
df$admit.ts = strptime(tmp$zeitpunktaufnahme,format="%F %H:%M:%S")
df$phys.ts = strptime(tmp$zeitpunktarztkontakt, format="%F %H:%M:%S")
df$therapy.ts = strptime(tmp$zeitpunkttherapie, format="%F %H:%M:%S")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(difftime(df$admit.ts,df$dob)/365.25)


# remove prefixes from string values
df$triage.result = as.factor(substring(tmp$triage, first=5))
df$cedis = as.factor(substring(tmp$cedis, first=9))
# TODO more columns



# Generate derived information

# Week day of admission
weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
df$admit.wd <- factor(x=strftime(df$admit.ts,format="%a"), levels=weekday.levels, ordered=TRUE)

# Hour of admission
hour.levels = sprintf('%02i',0:23)
df$admit.h <- factor(x=strftime(df$admit.ts,format="%H"), levels=hour.levels, ordered=TRUE)

# Time to triage
df$triage.d <- df$triage.ts - df$admit.ts

# Time to physician
df$phys.d <- df$phys.ts - df$admit.ts

# Time to therapy
df$therapy.d <- df$therapy.ts - df$admit.ts

# XHTML tables
source('xhtml-table.R')
xml.dir <- '../target/'

# Graphics & Plots
library(lattice)

gfx.dir <- '../target/'
gfx.ext <- '.svg'
gfx.dev <- 'svg'

# Counts per Hour
graph <- barchart(table(df$admit.h), horizontal=FALSE, xlab="Uhrzeit (Stunde)", ylab="Anzahl (n)")
# for viewing: print(graph)
# Save as SVG file
trellis.device(gfx.dev,file=paste0(gfx.dir,'admit.h',gfx.ext), width=8,height=4)
print(graph)
dev.off()
# Write table
xhtml.table(table(df$admit.h), file=paste0(xml.dir,'admit.h.xml'))


# Counts per Weekday
graph <- barchart(table(df$admit.wd), horizontal=FALSE, xlab="Wochentag", ylab="Anzahl (n)")
trellis.device('svg',file=paste0(gfx.dir,'admit.wd',gfx.ext),width=8,height=4)
print(graph)
dev.off()

# Time to physician
graph <- histogram(as.numeric(df$phys.d,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
trellis.device('svg',file=paste0(gfx.dir,'phys.d.hist',gfx.ext),width=8,height=4)
print(graph)
dev.off()

# Time to triage
graph <- histogram(as.numeric(df$triage.d,unit='mins'),xlab="Zeit von Aufnahme bis Triage in Minuten")
trellis.device('svg',file=paste0(gfx.dir,'triage.d.hist',gfx.ext),width=8,height=4)
print(graph)
dev.off()

# Time to therapy
graph <- histogram(as.numeric(df$therapy.d,unit='mins'),xlab="Zeit von Aufnahme bis zum Therapiebeginn in Minuten")
trellis.device('svg',file=paste0(gfx.dir,'therapy.d.hist',gfx.ext),width=8,height=4)
print(graph)
dev.off()


# Time to physician mean grouped by triage result
x <- aggregate(x=list(avg=df$phys.d), by=list(triage=df$triage.result), FUN=mean)
graph <- barchart(avg ~ triage, data=x, horizontal=FALSE, ylab="Durchschn. Zeit bis Arztkontakt", xlab="Triage")
trellis.device('svg',file=paste0(gfx.dir,'triage.phys.d.avg',gfx.ext),width=8,height=4)
print(graph)
dev.off()


# A little more sophisticated: Table with many aggregate functions
# list of aggregate functions we want to apply
agg.funs <- list(n=length, avg=mean, md=median, mi=min, ma=max)
agg.list <- lapply(agg.funs, function(fun){aggregate(x=df$phys.d, by=list(triage=df$triage.result),FUN=fun)$x})
x <- data.frame(triage=levels(df$triage.result), agg.list)
rm(agg.funs, agg.list)
xhtml.table(x, file=paste0(xml.dir,'triage.phys.d.xml'))



