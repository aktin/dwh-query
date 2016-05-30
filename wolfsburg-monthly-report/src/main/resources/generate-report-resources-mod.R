#!/usr/bin/Rscript

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
#tmp = read.csv2(file='Daten_ZNA_Entenhausen.csv', as.is=TRUE, na.strings='')
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')


# create new data frame to contain clean values
df = data.frame(patient=pat$patient_num, encounter=enc$encounter_num)

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE)

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(pat$birth_date,format="%F")
df$triage.ts = strptime(enc$zeitpunkttriage,format="%F %H:%M")
df$admit.ts = strptime(enc$zeitpunktaufnahme,format="%F %H:%M")
df$phys.ts = strptime(enc$zeitpunktarztkontakt, format="%F %H:%M")
df$therapy.ts = strptime(enc$zeitpunkttherapie, format="%F %H:%M")
df$discharge.ts = strptime(enc$zeitpunktentlassung, format="%F %H:%M")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(difftime(df$admit.ts,df$dob)/365.25)


# remove prefixes from string values
df$triage.result = as.factor(substring(enc$triage, first=5))
df$cedis = as.factor(substring(enc$cedis, first=9))
df$diagnosis = as.factor(substring(enc$Diagnose, first=9, last =11)) #ICD10 Codes Category (3-char only)
df$discharge = as.factor(substring(enc$Entlassung, first=7))
# TODO more columns



# Generate derived information

# Week day of admission
weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
df$admit.wd <- factor(x=strftime(df$admit.ts,format="%a"), levels=weekday.levels, ordered=TRUE)

# Hour of admission
hour.levels = sprintf('%02i',0:23)
df$admit.h <- factor(x=strftime(df$admit.ts,format="%H"), levels=hour.levels, ordered=TRUE)

#Hour of admission per weekday
admit.hwd <- matrix(,7,24)
for(i in 1:length(weekday.levels)) {
  admit.hwd[i,] <- as.vector(table(factor(x=strftime(df$admit.ts[df$admit.wd == weekday.levels[i]],format="%H"), levels=hour.levels, ordered=TRUE)))
}

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

# Time to discharge
df$discharge.d <- df$discharge.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$discharge.d[df$discharge.d < 0] <- NA
df$discharge.d[df$discharge.d > 24*60] <- NA     #could be more than 24 hours!

# Referral Codes
df$referral <- factor(x=enc$zuweisung)

#Transport Codes
df$transport <- factor(x=enc$transportmittel)

#CEDIS Codes
df$cedis <- factor(x=enc$cedis,t(cedis[1]))


# XHTML tables
source('xhtml-table.R')
xml.dir <- ''

# Graphics & Plots
#todo: main/sub Überschriften sinnvoll setzen und x/ylab ggf. anpassen
library(lattice)

gfx.dir <- ''
gfx.ext <- '.svg'
gfx.dev <- 'svg'

# Counts per Hour
try({
  graph <- barchart(table(df$admit.h), horizontal=FALSE, xlab="Uhrzeit (Stunde)", ylab="Anzahl (n)")
  # for viewing: print(graph)
  # Save as SVG file
  trellis.device(gfx.dev,file=paste0(gfx.dir,'admit.h',gfx.ext), width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)
# Write table
try({
  xhtml.table(table(df$admit.h), file=paste0(xml.dir,'admit.h.xml'))
}, silent=FALSE)


# Counts per Weekday
try({
  graph <- barchart(table(df$admit.wd), horizontal=FALSE, xlab="Wochentag", ylab="Anzahl (n)")
  trellis.device('svg',file=paste0(gfx.dir,'admit.wd',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Counts per Hour/Weekday
try({
  colors <- rainbow(length(weekday.levels)) 
  svg(paste0(gfx.dir,'admit.hwd','.svg'))
  plot(admit.hwd[1,], xlab="Uhrzeit (Stunde)", ylab="Anzahl (n)")   #ToDo: How To Plot a Matrix?
  for (i in 1:length(weekday.levels)) {
    lines(admit.hwd[i,],type="b",col=colors[i])
  }
  legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  #trellis.device('svg',file=paste0(gfx.dir,'admit.hwd',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)

#Transport and referral
try({
  xhtml.table(table(df$transport,useNA = "always"), file=paste0(xml.dir,'transport.xml'))
  xhtml.table(table(df$referral,useNA = "always"), file=paste0(xml.dir,'referral.xml'))
}, silent=FALSE)

# Time to physician
try({
  #graph <- barchart(table(df$phys.d), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
  #graph <- plot(cumsum(table(df$phys.d)), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
  svg(paste0(gfx.dir,'phys.d.hist','.svg'))
  plot(ecdf(df$phys.d), xlim=c(1,200), xlab="Zeit von Aufnahme bis Arztkontakt in Minuten", ylab="Cummulative Percentage")
  #trellis.device('svg',file=paste0(gfx.dir,'phys.d.hist',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)

# Time to triage
try({
  graph <- histogram(as.numeric(df$triage.d,unit='mins'),xlab="Zeit von Aufnahme bis Triage in Minuten")
  trellis.device('svg',file=paste0(gfx.dir,'triage.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to therapy
try({
  graph <- histogram(as.numeric(df$therapy.d,unit='mins'),xlab="Zeit von Aufnahme bis zum Therapiebeginn in Minuten")
  trellis.device('svg',file=paste0(gfx.dir,'therapy.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to physician mean grouped by triage result
try({
  x <- aggregate(x=list(avg=df$phys.d), by=list(triage=df$triage.result), FUN=mean)
  graph <- barchart(avg ~ triage, data=x, horizontal=FALSE, ylab="Durchschn. Zeit bis Arztkontakt", xlab="Triage")
  trellis.device('svg',file=paste0(gfx.dir,'triage.phys.d.avg',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to discharge
try({
  graph <- histogram(as.numeric(df$discharge.d,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung in Minuten")
  trellis.device('svg',file=paste0(gfx.dir,'discharge.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

try({
  output <- c('','intern','extern','Fallzahl','1000','500','Mittelwert','120','180')
  dim(output) <- c(3,3)
  xhtml.table(output, file=paste0(xml.dir,'discharge.d.xml'))
}, silent=FALSE)

# A little more sophisticated: Table with many aggregate functions
# list of aggregate functions we want to apply
try({
  agg.funs <- list(n=length, avg=mean, md=median, mi=min, ma=max)
  agg.list <- lapply(agg.funs, function(fun){aggregate(x=df$phys.d, by=list(triage=df$triage.result),FUN=fun)$x})
  x <- data.frame(triage=levels(df$triage.result), agg.list)
  rm(agg.funs, agg.list)
  xhtml.table(x, file=paste0(xml.dir,'triage.phys.d.xml'))
}, silent=FALSE)

#Entlassung & Co
try({
  graph <- barchart( table(df$discharge), xlab="Entlassung & Verlegung (Häufigkeit)")
  trellis.device('svg',file=paste0(gfx.dir,'discharge',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)
try({
  xhtml.table(table(df$discharge,useNA = "always"), file=paste0(xml.dir,'discharge.xml'))
}, silent=FALSE)

#TOP10 CEDIS
try({
  t <- table(df$cedis) #frequencies
  x <- sort(t, decreasing = TRUE)
  graph <- barchart( x [1:10], xlab="Top 10 CEDIS Vorstellungsgründe (Häufigkeit)")
  trellis.device('svg',file=paste0(gfx.dir,'cedis_top10',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

#CEDIS Groups
#HOW can I map the CEDIS levels? 001-012=CV, 051-056=HNE,...
#This causes warnings, but it works :)
try({
  df$cedis <- factor(x=enc$cedis,t(cedis[1]),labels=t(cedis[2]))  #map Categories
  df$cedis <- factor(x=df$cedis,t(cedis[1]),labels=t(cedis[3]))   #map Labels
  df$cedis <- droplevels(df$cedis) #not ideal, only showing used categories
  graph <- barchart(df$cedis,xlab="CEDIS Vorstellungsgründe nach Gruppen (Häufigkeit)")
  trellis.device('svg',file=paste0(gfx.dir,'cedis_groups',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

#TOP20 ICD
try({
  t <- table(df$diagnosis) #frequencies
  x <- sort(t, decreasing = TRUE)
  graph <- barchart( x [1:20], xlab="Top 20 ICD Abschlussdiagnosen (Häufigkeit)")
  trellis.device('svg',file=paste0(gfx.dir,'icd_top20',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

#calculate number of patients per hour of day
try({
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
}, silent=FALSE)

#calculate length of observation based on admission day
try({
  crowd.len <- max(df$admit.ts) - min(df$admit.ts)
  svg(paste0(gfx.dir,'crowding','.svg'))
  plot(crowding/as.numeric(round(crowd.len)),xlab = 'Hour of Day',ylab='Average number of patients in ER')
  #trellis.device('svg',file=paste0(gfx.dir,'crowding',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)