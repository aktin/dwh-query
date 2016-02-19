#!/usr/bin/Rscript

setwd("E:/GIT/aktin/dwh-query/report-demo/data/")

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
tmp = read.csv2(file='Daten_ZNA_Entenhausen.csv', as.is=TRUE, na.strings='')

# create new data frame to contain clean values
df = data.frame(patient=tmp$patient_num, encounter=tmp$encounter_num)

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE)

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(tmp$birth_date,format="%F")
df$triage.ts = strptime(tmp$zeitpunkttriage,format="%F %H:%M")
df$admit.ts = strptime(tmp$zeitpunktaufnahme,format="%F %H:%M")
df$phys.ts = strptime(tmp$zeitpunktarztkontakt, format="%F %H:%M")
df$therapy.ts = strptime(tmp$zeitpunkttherapie, format="%F %H:%M")
df$discharge.ts = strptime(tmp$entlassung, format="%F %H:%M")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(difftime(df$admit.ts,df$dob)/365.25)


# remove prefixes from string values
df$triage.result = as.fadfctor(substring(tmp$triage, first=5))
df$cedis = as.factor(substring(tmp$cedis, first=9))
df$diagnosis = as.factor(substring(tmp$Diagnose, first=9, last =11))
# TODO more columns



# Generate derived information

# Week day of admission
weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
df$admit.wd <- factor(x=strftime(df$admit.ts,format="%a"), levels=weekday.levels, ordered=TRUE)

# Hour of admission
hour.levels = sprintf('%02i',0:23)
df$admit.h <- factor(x=strftime(df$admit.ts,format="%H"), levels=hour.levels, ordered=TRUE)

# Hour of discharge
df$discharge.h <- factor(x=strftime(df$discharge.ts,format="%H"), levels=hour.levels, ordered=TRUE)

# Time to triage
df$triage.d <- df$triage.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$triage.d[df$triage.d < 0] <- NA
df$triage.d[df$triage.d > 24*60] <- NA

# Time to physician
df$phys.d <- df$phys.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$phys.d[df$phys.d < 0] <- NA
df$phys.d[df$phys.d > 24*60] <- NA

# Time to therapy
df$therapy.d <- df$therapy.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$therapy.d[df$therapy.d < 0] <- NA
df$therapy.d[df$therapy.d > 24*60] <- NA

# Referral Codes
df$referral <- factor(x=tmp$zuweisung)

#Transport Codes
df$transport <- factor(x=tmp$transportmittel)

#CEDIS Codes
df$cedis <- factor(x=tmp$cedis,t(cedis[1]))


# XHTML tables
source('xhtml-table.R')
xml.dir <- '../target/'

# Graphics & Plots
#todo: main/sub Überschriften sinnvoll setzen und x/ylab ggf. anpassen
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
#graph <- barchart(table(df$phys.d), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
#graph <- plot(cumsum(table(df$phys.d)), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
graph <- plot(ecdf(df$phys.d), xlim=c(1,200), xlab="Zeit von Aufnahme bis Arztkontakt in Minuten", ylab="Cummulative Percentage")
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

#TOP10 CEDIS
t <- table(df$cedis) #frequencies
x <- sort(t, decreasing = TRUE)
graph <- barchart( x [1:10], xlab="Top 10 CEDIS Vorstellungsgründe (Häufigkeit)")
trellis.device('svg',file=paste0(gfx.dir,'cedis_top10',gfx.ext),width=8,height=4)
print(graph)
dev.off()

#CEDIS Groups
#HOW can I map the CEDIS levels? 001-012=CV, 051-056=HNE,...
#This causes warnings, but it works :)
df$cedis <- factor(x=tmp$cedis,t(cedis[1]),labels=t(cedis[2]))
graph <- barchart(df$cedis,xlab="CEDIS Vorstellungsgründe nach Gruppen (Häufigkeit)")
trellis.device('svg',file=paste0(gfx.dir,'cedis_groups',gfx.ext),width=8,height=4)
print(graph)
dev.off()

#TOP20 ICD
t <- table(df$diagnosis) #frequencies
x <- sort(t, decreasing = TRUE)
graph <- barchart( x [1:20], xlab="Top 20 ICD Abschlussdiagnosen (Häufigkeit)")
trellis.device('svg',file=paste0(gfx.dir,'icd_top20',gfx.ext),width=8,height=4)
print(graph)
dev.off()

#calculate number of patients per hour of day
admit <- as.numeric(df$admit.h)
discharge <- as.numeric(df$discharge.h)
crowding <- rep(0,24)
for (i in 1:length(admit)){
  if (!is.na(admit[i]) & !is.na(discharge[i])) {
    if (admit[i] < discharge[i]) {
      for (j in admit[i]:discharge[i]) {
        crowding[j] <- crowding[j] +1
      }
    }
    if (admit[i] >= discharge[i]) {
      for (j in discharge[i]:admit[i]) {
        crowding[j] <- crowding[j] +1
      }
    }
  }
}
#calculate length of observation based on admission day
crowd.len <- max(df$admit.ts) - min(df$admit.ts)
graph <- plot(crowding/as.numeric(round(crowd.len)),xlab = 'Hour of Day',ylab='Average number of patients in ER')
trellis.device('svg',file=paste0(gfx.dir,'crowding',gfx.ext),width=8,height=4)
print(graph)
dev.off()