#!/usr/bin/Rscript

#setwd("E:/GIT/aktin/dwh-query/wolfsburg-monthly-report/src/main/resources/")

round_df <- function(df, digits) {
  nums <- vapply(df, is.numeric, FUN.VALUE = logical(1))
  
  df[,nums] <- round(df[,nums], digits = digits)
  
  (df)
}

stdabw <- function(x) {n=length(x) ; sqrt(var(x) * (n-1) / n)}

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
#tmp = read.csv2(file='Daten_ZNA_Entenhausen.csv', as.is=TRUE, na.strings='')
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')


# create new data frame to contain clean values
df = data.frame(patient=pat$patient_id, encounter=enc$dokument_id)

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE)

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(pat$geburtsdatum_ts,format="%F")
df$triage.ts = strptime(enc$triage_ts,format="%F %H:%M")
df$admit.ts = strptime(enc$aufnahme_ts,format="%F %H:%M")
df$phys.ts = strptime(enc$arztkontakt_ts, format="%F %H:%M")
df$therapy.ts = strptime(enc$therapiebeginn_ts, format="%F %H:%M")
df$discharge.ts = strptime(enc$entlassung_ts, format="%F %H:%M")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(as.numeric(difftime(df$admit.ts,df$dob))/365.25)


# remove prefixes from string values
df$triage.result = as.factor(substring(enc$triage, first=5))
levels(df$triage.result) <- c('Rot','Orange','Gelb','Gruen','Blau')
df$cedis = as.factor(substring(enc$cedis, first=9))
df$diagnosis = as.factor(substring(enc$diagnose_fuehrend, first=9, last =11)) #ICD10 Codes Category (3-char only)
#df$discharge = as.factor(substring(enc$entlassung, first=7))
df$sex = pat$geschlecht
# TODO more columns



# Generate derived information

# Week day of admission
weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
df$admit.wd <- factor(x=strftime(df$admit.ts,format="%a"), levels=weekday.levels, ordered=TRUE)

# Day of admission
df$admit.d <- factor(x=strftime(df$admit.ts,format="%F"), ordered=TRUE)

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
df$therapy.d <- df$therapy.ts - df$phys.ts
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
levels(df$referral) <- list("Ohne"="AKTIN:TRANSPORT:NA","KTW"="AKTIN:TRANSPORT:1","RTW"="AKTIN:TRANSPORT:2","NAW"="AKTIN:TRANSPORT:3","RTH"="AKTIN:TRANSPORT:4","Anderes"="AKTIN:TRANSPORT:OTH")

#Transport Codes
df$transport <- factor(x=enc$transportmittel)
levels(df$transport) <- list("Ohne"="AKTIN:TRANSPORT:NA","KTW"="AKTIN:TRANSPORT:1","RTW"="AKTIN:TRANSPORT:2","NAW"="AKTIN:TRANSPORT:3","RTH"="AKTIN:TRANSPORT:4","Anderes"="AKTIN:TRANSPORT:OTH")

#Discharge Codes
df$discharge = factor(x=enc$entlassung)
levels(df$discharge) <- list("Tod"="AKTIN:DISCHARGE:1","Gegen aerztl. Rat"="AKTIN:DISCHARGE:2","Abbruch durch Pat."="AKTIN:DISCHARGE:3","Nach Hause"="AKTIN:DISCHARGE:4","Zu weiterbehandl. Arzt"="AKTIN:DISCHARGE:5","Kein Arztkontakt"="AKTIN:DISCHARGE:6","Sonstiges"="AKTIN:DISCHARGE:OTH","Intern: Funktion"="AKTIN:TRANSFER:1","Extern: Funktion"="AKTIN:TRANSFER:2","Intern: Ueberwachung"="AKTIN:TRANSFER:3","Extern: Ueberwachung"="AKTIN:TRANSFER:4","Intern: Normalstation"="AKTIN:TRANSFER:5","Extern: Normalstation"="AKTIN:TRANSFER:6")

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
  graph <- barchart(table(df$admit.h), horizontal=FALSE, xlab="Uhrzeit [Stunde]", ylab="Anzahl Patienten")
  # for viewing: print(graph)
  # Save as SVG file
  trellis.device(gfx.dev,file=paste0(gfx.dir,'admit.h',gfx.ext), width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)
# Write table
try({
  xhtml.table(table(df$admit.h)[1:12], file=paste0(xml.dir,'admit.h.xml'),align='center')
  xhtml.table(table(df$admit.h)[13:24], file=paste0(xml.dir,'admit2.h.xml'),align='center')
}, silent=FALSE)

#calculate number of weekdays in the current period (month)
weekdaycounts=rep(0,7) #Mo-So
wbindex <- 0
for (i in 2:length(df$admit.wd)){
  if (! is.na(df$admit.wd[i])) {
    if (i == 2 || df$admit.wd[i] != df$admit.wd[i-1]) {
      wbindex <- as.numeric(sapply(as.character(df$admit.wd[i]), switch, 
                                   Mo = 1, 
                                   Di = 2, 
                                   Mi = 3, 
                                   Do = 4, 
                                   Fr = 5, 
                                   Sa = 6, 
                                   So = 7))
      weekdaycounts[wbindex] <- weekdaycounts[wbindex]+1
    }
  }
}

# Counts per Weekday
try({
  graph <- barchart(round(table(df$admit.wd)/weekdaycounts,digits = 0), horizontal=FALSE, xlab="Wochentag", ylab="Durchschnittliche Anzahl Patienten")
  trellis.device('svg',file=paste0(gfx.dir,'admit.wd',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Counts per Hour/Weekday
try({
  colors <- rainbow(length(weekday.levels)) 
  svg(paste0(gfx.dir,'admit.hwd','.svg'))
  plot(admit.hwd[1,], xlab="Uhrzeit [Stunde]", ylab="Anzahl Patienten")   #ToDo: How To Plot a Matrix?
  for (i in 1:length(weekday.levels)) {
    lines(admit.hwd[i,],type="b",col=colors[i])
  }
  legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  #trellis.device('svg',file=paste0(gfx.dir,'admit.hwd',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)

# Counts per Hour/Weekday vs. Weekend
try({
  weekday <- c(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  weekend <- c(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  colors <- rainbow(2) 
  for (i in 1:24) {
    weekday[i] <- admit.hwd[1,i]+admit.hwd[2,i]+admit.hwd[3,i]+admit.hwd[4,i]+admit.hwd[5,i]
    weekend[i] <- admit.hwd[6,i]+admit.hwd[7,i]
  }
  svg(paste0(gfx.dir,'admit.hwd.weekend','.svg'))
  plot(weekend/2, xlab="Uhrzeit [Stunde]", ylab="Anzahl Patienten")  
  
  lines(weekday/5,type="b",col=colors[2])
  lines(weekend/2,type="b",col=colors[1])

  #legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  dev.off()
}, silent=FALSE)

#Transport and referral
try({
  #TODO ACHTUNG Bezeichnung so nicht richtig!
  #names(table_formatted) <- c('Ohne','KTW','RTW', 'NAW', 'RTH', 'Anderes', 'keine Daten')
  names(table_formatted)[length(names(table_formatted))]<-'keine Daten'
  a <- table_formatted
  b <- data.frame(Wert=names(a), Anzahl=as.numeric(a), Prozent=as.numeric(round((a / sum(a))*100,digits = 1)))
  c <- rbind(b, data.frame(Wert="Summe",Anzahl=sum(a),Prozent=as.numeric(round(sum(b$Prozent),digits = 1))))
  c[,3] <- sprintf(fmt="%.1f",c[,3])
  xhtml.table(c, file=paste0(xml.dir,'transport.xml'),align=c('left','right','right'),widths=c(20,20,20))
  
  table_formatted <- table(df$referral,useNA = "always")
  names(table_formatted) <- c('Notarzt', 'Ohne', 'Praxis', 'keine Daten')
  a <- table_formatted
  b <- data.frame(Wert=names(a), Anzahl=as.numeric(a), Prozent=as.numeric(round((a / sum(a))*100,digits = 1)))
  c <- rbind(b, data.frame(Wert="Summe",Anzahl=sum(a),Prozent=as.numeric(round(sum(b$Prozent),digits = 1))))
  c[,3] <- sprintf(fmt="%.1f",c[,3])
  xhtml.table(c, file=paste0(xml.dir,'referral.xml'),align=c('left','right','right'),widths=c(20,20,20))
}, silent=FALSE)

# Time to physician
try({
  #graph <- barchart(table(df$phys.d), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
  #graph <- plot(cumsum(table(df$phys.d)), horizontal=FALSE, xlab="Zeit von Aufnahme bis Arztkontakt in Minuten")
  svg(paste0(gfx.dir,'phys.d.ecdf','.svg'))
  plot(ecdf(df$phys.d), xlim=c(1,200), xlab="Zeit von Aufnahme bis Arztkontakt in Minuten", ylab="Cummulative Percentage")
  #trellis.device('svg',file=paste0(gfx.dir,'phys.d.hist',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)
try({
  a <- df$phys.d[df$phys.d<180]
  outofbounds <- length(df$phys.d) - length(a)
  isNA <- length(df$phys.d[is.na(df$phys.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt in Minuten",ylab="Relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 180 Minuten: ", outofbounds))
  trellis.device('svg',file=paste0(gfx.dir,'phys.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)


# Time to triage
try({
  a <- df$triage.d[df$triage.d<60]
  outofbounds <- length(df$triage.d) - length(a)
  isNA <- length(df$triage.d[is.na(df$triage.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Triage in Minuten",ylab="Relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  trellis.device('svg',file=paste0(gfx.dir,'triage.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to therapy
try({
  a <- df$therapy.d[df$therapy.d<60]
  outofbounds <- length(df$therapy.d) - length(a)
  isNA <- length(df$therapy.d[is.na(df$therapy.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zum Therapiebeginn in Minuten",ylab="relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  trellis.device('svg',file=paste0(gfx.dir,'therapy.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to physician mean grouped by triage result
try({
  x <- aggregate(x=list(avg=df$phys.d), by=list(triage=df$triage.result), FUN=mean, na.rm=TRUE)
  graph <- barchart(avg ~ triage, data=x, horizontal=FALSE, ylab="Durchschn. Zeit bis Arztkontakt", xlab="Triage")
  trellis.device('svg',file=paste0(gfx.dir,'triage.phys.d.avg',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# Time to discharge
try({
  a <- df$discharge.d[df$discharge.d<600]
  outofbounds <- length(df$discharge.d) - length(a)
  isNA <- length(df$discharge.d[is.na(df$discharge.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung in Minuten",ylab="relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 600 Minuten: ", outofbounds))
  trellis.device('svg',file=paste0(gfx.dir,'discharge.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

#try({
#  output <- c('','intern','extern','Fallzahl','1000','500','Mittelwert','120','180')
#  dim(output) <- c(3,3)
#  xhtml.table(output, file=paste0(xml.dir,'discharge.d.xml'))
#}, silent=FALSE)

try({
  table_formatted <- table(df$triage.result,useNA = "always")
  #TODO ACHTUNG Bezeichnung so nicht richtig!
  names(table_formatted) <- c('Rot','Orange','Gelb', 'Gruen', 'Blau', 'Ohne')
  a <- table_formatted
  b <- data.frame(Wert=names(a), Anzahl=as.numeric(a), Prozent=as.numeric(round((a / sum(a))*100,digits = 1)))
  c <- rbind(b, data.frame(Wert="Summe",Anzahl=sum(a),Prozent=as.numeric(round(sum(b$Prozent),digits = 1))))
  c[,3] <- sprintf(fmt="%.1f",c[,3])
  xhtml.table(c, file=paste0(xml.dir,'triage.xml'),align=c('left','right','right'),widths=c(20,20,20))
  
  x <-  table(df$triage.result,useNA = "always")
  #x[is.na(x)] <- 'keine'
  #table_formatted <- table(x)
  #table_formatted[is.na(table_formatted)] <- 'keine'
  #names(x) <- c('Rot','Orange','Gelb','Gruen','Blau','Ohne')
  graph <- barchart( x, horizontal=FALSE, xlab="Ersteinschätzung", ylab='Anzahl Patienten')
  trellis.device('svg',file=paste0(gfx.dir,'triage',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

# A little more sophisticated: Table with many aggregate functions
# list of aggregate functions we want to apply
#try({
#  agg.funs <- list(n=length, avg=mean, med=median, min=min, max=max)
#  agg.list <- lapply(agg.funs, function(fun){aggregate(x=df$phys.d, by=list(triage=df$triage.result),FUN=fun)$x})#
#  agg.list$avg <- round(agg.list$avg,1)
#  x <- data.frame(triage=levels(df$triage.result), agg.list)
#  rm(agg.funs, agg.list)
#  xhtml.table(x, file=paste0(xml.dir,'triage.phys.d.xml'),align=c('left','right','right','right','right','right'),width=10)
#}, silent=FALSE)
# list of aggregate functions we want to apply
try({
  agg.funs <- list(avg=mean, med=median, min=min, max=max)
  agg.list <- lapply(agg.funs, function(fun){aggregate(x=df$phys.d, by=list(triage=df$triage.result),FUN=fun, na.rm=TRUE)$x})
  agg.list$avg <- round(agg.list$avg,1)
  agg.list$med <- round(agg.list$med,0)
  agg.length <- aggregate(x=list(count=df$phys.d), by=list(triage=df$triage.result), FUN=length)
  x <- data.frame(triage=levels(df$triage.result), n=agg.length$count, agg.list)
  rm(agg.funs, agg.list)
  xhtml.table(x, file=paste0(xml.dir,'triage.phys.d.xml'),align=c('left','right','right','right','right','right'),width=10)
}, silent=FALSE)

#Entlassung & Co
try({
  graph <- barchart( table(df$discharge), xlab="Entlassung & Verlegung (Häufigkeit)")
  trellis.device('svg',file=paste0(gfx.dir,'discharge',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)
try({
  a <- table(df$discharge,useNA = "always")
  names(a)[length(names(a))]<-'keine Daten'
  b <- data.frame(Wert=names(a), Anzahl=as.numeric(a), Prozent=as.numeric(round((a / sum(a))*100,digits = 1)))
  c <- rbind(b, data.frame(Wert="Summe",Anzahl=sum(a),Prozent=as.numeric(round(sum(b$Prozent),digits = 1))))
  c[,3] <- sprintf(fmt="%.1f",c[,3])
  xhtml.table(c, file=paste0(xml.dir,'discharge.xml'),align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)

#TOP10 CEDIS
try({
  t <- table(df$cedis) #frequencies
  x <- sort(t, decreasing = TRUE)
  y <- x [1:10]
  names(y) <- c('Unbekannt','Schmerzen obere Extremitaet','Schmerzen untere Extremitaet','Bauchschmerzen','Verletzung obere Extremitaet','Brustschmerz (kardial)','Luftnot','Rueckenschmerzen','Verletzung untere Extremitaet','Hypertonie')
  graph <- barchart( y, xlab="Top 10 CEDIS Vorstellungsgründe (Häufigkeit)")
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

#Patient Sex
try({
  table_formatted <- table(df$sex,useNA = "always")
  names(table_formatted) <- c('Weiblich', 'Maennlich','keine Angabe')
  a <- table_formatted
  b <- data.frame(Wert=names(a), Anzahl=as.numeric(a), Prozent=as.numeric(round((a / sum(a))*100,digits = 1)))
  c <- rbind(b, data.frame(Wert="Summe",Anzahl=sum(a),Prozent=as.numeric(round(sum(b$Prozent),digits = 1))))
  c[,3] <- sprintf(fmt="%.1f",c[,3])
  xhtml.table(c, file=paste0(xml.dir,'sex.xml'),align=c('left','right','right'),widths=c(25,15,15))
}, silent=FALSE)

#Patient Age
try({
  Wert <- c('Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Alter <- c(round(mean(na.omit(df$age)),1),median(na.omit(df$age)),round(stdabw(na.omit(df$age)),1),min(na.omit(df$age)),max(na.omit(df$age)))
  Alter <- sprintf(fmt="%.1f",Alter)
  b <- data.frame(Wert,Alter)
  xhtml.table(b, file=paste0(xml.dir,'age.xml'),align=c('left','right'),widths=c(30,15))
}, silent=FALSE)
try({
  graph <- histogram(na.omit(df$age),xlab="Alter",ylab="Relative Häufigkeit [%]")
  trellis.device('svg',file=paste0(gfx.dir,'age',gfx.ext),width=8,height=4)
  print(graph)
  dev.off()
}, silent=FALSE)

#Admit Day
try({
  Datum <- names(table(df$admit.d))
  Anzahl <- as.vector(table(df$admit.d))
  b <- data.frame(Datum,Anzahl)
  xhtml.table(b, file=paste0(xml.dir,'admit.d.xml'),align=c('left','right'),widths=c(25,15))
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

#calculate Top Counts of Patients in ER (crowding)
try({
  crowdperday = matrix(0,length(levels(df$admit.d)),24)
  index <- rep(1:length(levels(df$admit.d)))
  d <- 1
  for (i in 1:length(admit)){
    if (i > 1) {
      if (df$admit.d[i] > df$admit.d[i-1]) {
        d <- d+1
      }
    }
    if (!is.na(df$admit.ts[i]) & !is.na(df$discharge.ts[i])) {
      if (df$admit.ts[i] < df$discharge.ts[i]) {
        for (j in admit[i]:discharge[i]) {
          crowdperday[d,j] <- crowdperday[d,j] +1
        }
      }
      if (admit[i] >= discharge[i]) {
        for (j in discharge[i]:admit[i]) {
          crowdperday[d,j] <- crowdperday[d,j] +1
        }
      }
    }
  }
  pmax <- apply (crowdperday,1,max)
  whichmax=apply(crowdperday,1,which.max)
  pmin <- apply (crowdperday,1,min)
  whichmin=apply(crowdperday,1,which.min)
  pmean <- round(apply (crowdperday,1,mean),digits=0)
  c <- data.frame(Datum=levels(df$admit.d),MaxTime=paste(whichmax,'Uhr'),MaxPat=pmax,MinTime=paste(whichmin,'Uhr'),MinPat=pmin,MeanPat=pmean)
  xhtml.table(c, file=paste0(xml.dir,'crowding.d.xml'),align=c('left','right','right','right','right','right'),widths=c(15,15,15,15,15,15))
}, silent=FALSE)

#calculate length of observation based on admission day
try({
  colors=rainbow(3)
  crowd.len <- max(df$admit.ts) - min(df$admit.ts)
  svg(paste0(gfx.dir,'crowding','.svg'))
  plot(apply (crowdperday,2,max),xlab = 'Uhrzeit [Stunde]',ylab='Anwesende Patienten',ylim=c(min(apply (crowdperday,2,min)),max(apply (crowdperday,2,max))),sub='rot=Maximum, blau=Minimum, gruen=Durchschnitt')
  lines(crowding/as.numeric(round(crowd.len)),type="b",col=colors[2])
  lines(apply (crowdperday,2,max),type="b",col=colors[1])
  lines(apply (crowdperday,2,min),type="b",col=colors[3])
  #trellis.device('svg',file=paste0(gfx.dir,'crowding',gfx.ext),width=8,height=4)
  #print(graph)
  dev.off()
}, silent=FALSE)

try({
  df$los = df$discharge.ts - df$admit.ts
  los_erfasst=length(df$los)-length(df$los[is.na(df$los)])
  Wert <- c('Anzahl Zeiten erfasst',' ','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(los_erfasst,' ',round(mean(na.omit(df$los)),1),median(na.omit(df$los)),round(stdabw(na.omit(df$los)),1),min(na.omit(df$los)),max(na.omit(df$los)))
  #Alter <- sprintf(fmt="%.1f",Alter)
  b <- data.frame(Wert,Zeit)
  xhtml.table(b, file=paste0(xml.dir,'los.xml'),align=c('left','right'),widths=c(30,15))
}, silent=FALSE)

#Aufnahme vs. Entlassung
#Testberechnung für einen Tag
#TODO... das klappt so noch nicht
try({
  testday=df$admit.h[1933:2011]
  admits <- rep(0,24)
  discharges <- rep(0,24)
  for (i in 1:length(testday)){
    hour_admit <- df$admit.h[1932+i]
    admits[hour_admit] <- admits[hour_admit]  + 1
    hour_dicharge <- df$discharge.h[1932+i]
    discharges[hour_dicharge] <- discharges[hour_dicharge]  + 1
  }
  colors=rainbow(3)
  svg(paste0(gfx.dir,'output','.svg'))
  plot(crowdperday[27,],xlab = 'Uhrzeit [Stunde]',ylab='Anzahl Patienten',ylim=c(0,65),sub='rot=Anzahl Patienten, blau=Aufnahmen, gruen=Entlassungen')
  lines(crowdperday[27,],type="b",col=colors[1])
  lines(admits,type="b",col=colors[3])
  lines(discharges,type="b",col=colors[2])
  dev.off()
  
}, silent=FALSE)
