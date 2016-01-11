#!/usr/bin/r

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
tmp = read.csv2(file='export_for_reporting.csv', as.is=TRUE, na.strings='')

# create new data frame to contain clean values
df = data.frame(patient=tmp$patient_num, encounter=tmp$encounter_num)

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(tmp$birth_date,format="")
df$triage.ts = strptime(tmp$zeitpunkttriage,format="%F %H:%M:%S")
df$admit.ts = strptime(tmp$zeitpunktaufnahme,format="%F %H:%M:%S")
df$phys.ts = strptime(tmp$zeitpunktarztkontakt, format="%F %H:%M:%S")

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




# Graphics & Plots
library(lattice)

gfx.dir <- '../target/'
gfx.ext <- '.svg'
gfx.dev <- 'svg'

# Counts per Hour
graph <- barchart(table(df$admit.h), horizontal=FALSE, xlab="Uhrzeit (Stunde)", ylab="Anzahl (n)")
# for viewing: print(graph)
# save as EPS file
trellis.device(gfx.dev,file=paste0(gfx.dir,'admit.h',gfx.ext), width=8,height=4)
print(graph)
dev.off()

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


# Create XHTML tables
source('xhtml-table.R')
xml.dir <- '../target/'

xhtml.table(table(df$admit.h), file=paste0(xml.dir,'admit.h.xml'))
