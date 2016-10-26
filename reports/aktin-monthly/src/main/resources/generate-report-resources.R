#!/usr/bin/Rscript

#setwd("E:/GIT/aktin/dwh-query/wolfsburg-monthly-report/src/main/resources/")

std_cols1 <- c("firebrick3")
std_cols3 <- c("firebrick3","blue","green")
std_cols5 <- c("firebrick3","orange","yellow","green","blue","white")

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
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
diag <- read.table(file='diagnoses.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='',stringsAsFactors=FALSE, colClasses = c("character"))

# create new data frame for encounter data to contain clean values
df = data.frame(patient=pat$patient_id, encounter=enc$encounter_id)

# create new data frame for diagnoses data to contain clean values
df_diag <- data.frame(diagnosis=as.factor(substring(diag$diagnose_code, first=1, last=3)))

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';')

#load ICD mapping table
icd = read.csv2(file='ICD-3Steller.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';')

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(pat$geburtsdatum_ts,format="%F")
#df$triage.ts = strptime(enc$triage_ts,format="%F %H:%M")
#df$admit.ts = strptime(enc$aufnahme_ts,format="%F %H:%M")
#df$phys.ts = strptime(enc$arztkontakt_ts, format="%F %H:%M")
#df$therapy.ts = strptime(enc$therapiebeginn_ts, format="%F %H:%M")
#df$discharge.ts = strptime(enc$entlassung_ts, format="%F %H:%M")
df$triage.ts = strptime(enc$triage_ts,format="%FT%H:%M")
df$admit.ts = strptime(enc$aufnahme_ts,format="%FT%H:%M")
df$phys.ts = strptime(enc$arztkontakt_ts, format="%FT%H:%M")
df$therapy.ts = strptime(enc$therapiebeginn_ts, format="%FT%H:%M")
df$discharge.ts = strptime(enc$entlassung_ts, format="%FT%H:%M")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(as.numeric(difftime(df$admit.ts,df$dob))/365.25)
df$sex = factor(pat$geschlecht)
levels(df$sex) <- list("male"="male","female"="female")

df$triage.result = as.factor(enc$triage)
levels(df$triage.result) <- list("Rot"="1","Orange"="2","Gelb"="3","Gruen"="4","Blau"="5","Ohne"="NA")
df$triage.result[is.na(df$triage.result)] <- 'Ohne'

# Referral Codes
df$referral <- factor(x=enc$zuweisung)
#levels(df$referral) <- list("Vertragsarzt"="AKTIN:REFERRAL:VAP","KV-Notfallpraxis am Krankenhaus"="AKTIN:REFERRAL:KVNPIK","	KV-Notdienst ausserhalb des Krankenhauses	"="AKTIN:REFERRAL:KVNDAK","Rettungsdienst"="AKTIN:REFERRAL:RD","Notarzt"="AKTIN:REFERRAL:NA","Klinik/Verlegung"="AKTIN:REFERRAL:KLINV","Zuweisung nicht durch Arzt"="AKTIN:REFERRAL:NPHYS")
levels(df$referral) <- list("Vertragsarzt"="VAP","KV-Notfallpraxis am Krankenhaus"="KVNPIK","	KV-Notdienst ausserhalb des Krankenhauses	"="KVNDAK","Rettungsdienst"="RD","Notarzt"="NA","Klinik/Verlegung"="KLINV","Zuweisung nicht durch Arzt"="NPHYS")

#Transport Codes
df$transport <- factor(x=enc$transportmittel)
#levels(df$transport) <- list("Ohne"="AKTIN:TRANSPORT:NA","KTW"="AKTIN:TRANSPORT:1","RTW"="AKTIN:TRANSPORT:2","NAW/NEF/ITW"="AKTIN:TRANSPORT:3","RTH/ITH"="AKTIN:TRANSPORT:4","Anderes"="AKTIN:TRANSPORT:OTH")
levels(df$transport) <- list("Ohne"="NA","KTW"="1","RTW"="2","NAW/NEF/ITW"="3","RTH/ITH"="4","Anderes"="OTH")

#Discharge Codes
df$discharge = factor(x=enc$entlassung)
#levels(df$discharge) <- list("Tod"="AKTIN:DISCHARGE:1","Gegen aerztl. Rat"="AKTIN:DISCHARGE:2","Abbruch durch Pat."="AKTIN:DISCHARGE:3","Nach Hause"="AKTIN:DISCHARGE:4","Zu weiterbehandl. Arzt"="AKTIN:DISCHARGE:5","Kein Arztkontakt"="AKTIN:DISCHARGE:6","Sonstiges"="AKTIN:DISCHARGE:OTH","Intern: Funktion"="AKTIN:TRANSFER:1","Extern: Funktion"="AKTIN:TRANSFER:2","Intern: Ueberwachung"="AKTIN:TRANSFER:3","Extern: Ueberwachung"="AKTIN:TRANSFER:4","Intern: Normalstation"="AKTIN:TRANSFER:5","Extern: Normalstation"="AKTIN:TRANSFER:6")
levels(df$discharge) <- list("Tod"="DISCHARGE:1","Gegen aerztl. Rat"="DISCHARGE:2","Abbruch durch Pat."="DISCHARGE:3","Nach Hause"="DISCHARGE:4","Zu weiterbehandl. Arzt"="DISCHARGE:5","Kein Arztkontakt"="DISCHARGE:6","Sonstiges"="DISCHARGE:OTH","Intern: Funktion"="TRANSFER:1","Extern: Funktion"="TRANSFER:2","Intern: Ueberwachung"="TRANSFER:3","Extern: Ueberwachung"="TRANSFER:4","Intern: Normalstation"="TRANSFER:5","Extern: Normalstation"="TRANSFER:6")

#Isolation Codes
df$isolation = factor(x=enc$isolation)
levels(df$isolation) <- list("Isolation"="ISO","Keine Isolation"="ISO:NEG","Umkehrisolation"="RISO")
df$isolation_grund = factor(x=enc$isolation_grund)
levels(df$isolation_grund) <- list("Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Andere"="OTH")

#CEDIS Codes
df$cedis <- factor(x=enc$cedis,t(cedis[1]))

#Multiresistente Erreger
df$keime <- factor(enc$keime)
df$keime_mrsa <- factor(enc$keime_mrsa)
df$keime_3mrgn <- factor(enc$keime_3mrgn)
df$keime_4mrgn <- factor(enc$keime_4mrgn)
df$keime_vre <- factor(enc$keime_vre)
df$keime_andere <- factor(enc$keime_andere)

#Diagnoses
df_diag$fuehrend = diag$diagnose_fuehrend
df_diag$zusatz = diag$diagnose_zusatz


# Generate derived information

# Week day of admission
weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
df$admit.wd <- factor(x=strftime(df$admit.ts,format="%a"), levels=weekday.levels, ordered=TRUE)

# Day of admission
df$admit.day <- factor(x=strftime(df$admit.ts,format="%F"), ordered=TRUE)

# Day of discharge
df$discharge.day <- factor(x=strftime(df$discharge.ts,format="%F"), ordered=TRUE)

# Hour of admission
hour.levels = sprintf('%02i',0:23)
df$admit.h <- factor(x=strftime(df$admit.ts,format="%H"), levels=hour.levels, ordered=TRUE)

#Hour of admission per weekday
admit.hwd <- matrix(NA,7,24)
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

# Time phys to therapy
df$therapy.d <- df$therapy.ts - df$phys.ts
# Values out of bounds (<0h or >24h) => NA
df$therapy.d[df$therapy.d < 0] <- NA
df$therapy.d[df$therapy.d > 24*60] <- NA

# Time to discharge
df$discharge.d <- df$discharge.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$discharge.d[df$discharge.d < 0] <- NA
df$discharge.d[df$discharge.d > 24*60] <- NA     #could be more than 24 hours!

# XHTML tables
source('xhtml-table.R')
xml.dir <- ''

# Graphics & Plots
library(lattice)

gfx.dir <- ''
gfx.ext <- '.svg'
gfx.dev <- 'svg'

# Counts per Hour
try({
  graph <- barchart(table(df$admit.h)/length(levels(df$admit.day)), horizontal=FALSE, xlab="Uhrzeit [Stunde]", ylab="Durchschnittliche Anzahl Patienten",col=std_cols1,origin=0)
  trellis.device(gfx.dev,file=paste0(gfx.dir,'admit.h',gfx.ext), width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)
# Write table
try({
  table_pretty <- format(round(table(df$admit.h)[1:12]/length(levels(df$admit.day)), 1), nsmall=1, big.mark=".")
  xhtml.table(table_pretty, file=paste0(xml.dir,'admit.h.xml'),align='center')
  table_pretty <- format(round(table(df$admit.h)[13:24]/length(levels(df$admit.day)), 1), nsmall=1, big.mark=".")
  xhtml.table(table_pretty, file=paste0(xml.dir,'admit2.h.xml'),align='center')
}, silent=FALSE)

#calculate number of weekdays in the current period (month)
#limitation: days without patients at the end or start will be excluded
weekdaycounts=rep(0,7) #Mo-So
if (length(df$admit.wd) > 0) {
  wbindex <- 0
  for (i in 1:length(df$admit.wd)){
    if (! is.na(df$admit.wd[i])) {
      if (i == 1) {
        wbindex <- as.numeric(sapply(as.character(df$admit.wd[i]), switch, 
                                     Mo = 1, 
                                     Di = 2, 
                                     Mi = 3, 
                                     Do = 4, 
                                     Fr = 5, 
                                     Sa = 6, 
                                     So = 7))
        weekdaycounts[wbindex] <- weekdaycounts[wbindex]+1
      } else if (df$admit.wd[i] != df$admit.wd[i-1]) {
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
}

# Counts per Weekday
try({
  plottable <- round(table(df$admit.wd)/weekdaycounts,digits = 0)
  plottable[is.na(plottable)] <- 0
  graph <- barchart(plottable, horizontal=FALSE, xlab="Wochentag", ylab="Durchschnittliche Anzahl Patienten",col=std_cols1,origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'admit.wd',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

# Counts per Hour/Weekday
#try({
#  colors <- rainbow(length(weekday.levels)) 
#  svg(paste0(gfx.dir,'admit.hwd','.svg'))
#  plot(admit.hwd[1,], xlab="Uhrzeit [Stunde]", ylab="Durchschnittliche Anzahl Patienten")   #ToDo: How To Plot a Matrix?
#  for (i in 1:length(weekday.levels)) {
#    lines(admit.hwd[i,],type="b",col=colors[i])
#  }
#  legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
#  no_output <- dev.off() #silent
#}, silent=FALSE)

# Counts per Hour/Weekday vs. Weekend
try({
  weekday <- c(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  weekend <- c(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  for (i in 1:24) {
    weekday[i] <- admit.hwd[1,i]+admit.hwd[2,i]+admit.hwd[3,i]+admit.hwd[4,i]+admit.hwd[5,i]
    weekend[i] <- admit.hwd[6,i]+admit.hwd[7,i]
  }
  svg(paste0(gfx.dir,'admit.hwd.weekend','.svg'))
  plot(weekend/2, type='n', xlab="Uhrzeit [Stunde]", ylab="Anzahl Patienten",ylim=c(0,max(weekday/5,weekend/2)),xaxp=c(0,24,12))
  
  lines(weekday/5,type="b",col="blue",pch=15)
  lines(weekend/2,type="b",col="firebrick3",pch=17)

  #legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  no_output <- dev.off() #silent
}, silent=FALSE)

#Transport and referral
try({
  table_formatted <- table(df$transport,useNA = "always")
  names(table_formatted)[length(names(table_formatted))]<-'Keine Daten'
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'transport.xml'),align=c('left','right','right'),widths=c(25,15,15))
  
  table_formatted <- table(df$referral,useNA = "always")
  names(table_formatted)[length(names(table_formatted))]<-'Keine Daten'
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'referral.xml'),align=c('left','right','right'),widths=c(60,15,15))
}, silent=FALSE)

try({
  a <- df$phys.d[df$phys.d<180]
  outofbounds <- length(df$phys.d) - length(a)
  isNA <- length(df$phys.d[is.na(df$phys.d )])
  b <- a[!is.na(a)]
  #Absolute Zahlen
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,180,length=13),scales = list(x = list(at = seq(0,180,length=7))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 180 Minuten: ", outofbounds))
  #Relative Häufigkeiten
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,180,length=13),scales = list(x = list(at = seq(0,180,length=7))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 180 Minuten: ", outofbounds),col=std_cols1)
  trellis.device('svg',file=paste0(gfx.dir,'phys.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- c('Anzahl Zeiten beruecksichtigt','Anzahl fehlende Zeiten','Anzahl ungueltige Werte (>180min)','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,outofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  xhtml.table(b, file=paste0(xml.dir,'phys.d.xml'),align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to triage
try({
  a <- df$triage.d[df$triage.d<60]
  outofbounds <- length(df$triage.d) - length(a)
  isNA <- length(df$triage.d[is.na(df$triage.d )])
  b <- a[!is.na(a)]
  #Absolute Zahlen
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Triage [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,60,length=13),sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  #Relative Häufigkeiten
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Triage [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,60,length=13),sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds),col=std_cols1)
  trellis.device('svg',file=paste0(gfx.dir,'triage.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- c('Anzahl Zeiten beruecksichtigt','Anzahl fehlende Zeiten','Anzahl ungueltige Werte (>60min)','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,outofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  xhtml.table(b, file=paste0(xml.dir,'triage.d.xml'),align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

if (FALSE) { ##not used

# Time physician to therapy
tryCatch({
  a <- df$therapy.d[df$therapy.d<60]
  outofbounds <- length(df$therapy.d) - length(a)
  isNA <- length(df$therapy.d[is.na(df$therapy.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit vom ersten Arztkontakt bis zum Therapiebeginn [Minuten]",ylab="relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  trellis.device('svg',file=paste0(gfx.dir,'therapy.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, error = function(err) {
  par(mar = c(0,0,0,0))
  svg(paste0(gfx.dir,'therapy.d.hist','.svg'))
  plot(c(0, 1), c(0, 1), ann = F, bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n')
  no_output <- dev.off()
  par(mar = c(5, 4, 4, 2) + 0.1)
  #ecode <- file.copy(paste0(gfx.dir,'dummy',gfx.ext),paste0(gfx.dir,'therapy.d.hist',gfx.ext))
  return(paste("R-Plot failed: ",err))  #only for debug
}, silent=TRUE)

}
  
try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- c('Anzahl Zeiten beruecksichtigt','Anzahl fehlende Zeiten','Anzahl ungueltige Werte (>60min)','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,outofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  xhtml.table(b, file=paste0(xml.dir,'therapy.d.xml'),align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to physician mean grouped by triage result
try({
  a <- df$phys.d[df$phys.d<180]
  outofbounds <- length(df$phys.d) - length(a)
  b <- df$triage.result[df$phys.d<180]
  y <- data.frame(time=a, triage=b)
  x <- with(y, tapply(time, list(triage), FUN=mean, na.rm=TRUE))
  x[is.na(x)] <- 0
  y <- data.frame(avg=as.numeric(x), triage=factor(names(x), levels=c("Rot","Orange","Gelb","Gruen","Blau","Ohne")))
  # TODO error below: Error in units == (un1 <- units[1L]), comparison of these types is not implemented
  graph <- barchart(avg ~ triage, data=y, horizontal=FALSE, col=std_cols5 ,ylab="Durchschn. Zeit bis Arztkontakt [Min.]", xlab="Triage-Gruppe",sub=paste("Werte über 180 Minuten (unberücksichtigt): ", outofbounds),ylim=c(0,signif(max(as.numeric(y$avg)),digits=1) +10),origin=0)
  #text(graph,x$triage,labels = x$triage,pos=1)
  trellis.device('svg',file=paste0(gfx.dir,'triage.phys.d.avg',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

# Time to discharge
try({
  a <- df$discharge.d[df$discharge.d<600]
  outofbounds <- length(df$discharge.d) - length(a)
  isNA <- length(df$discharge.d[is.na(df$discharge.d )])
  b <- a[!is.na(a)]
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,600,length=11),scales = list(x = list(at = seq(0,600,length=11))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 600 Minuten: ", outofbounds),col=std_cols1)
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,600,length=11),scales = list(x = list(at = seq(0,600,length=11))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 600 Minuten: ", outofbounds),col=std_cols1)
  trellis.device('svg',file=paste0(gfx.dir,'discharge.d.hist',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

#try({
#  output <- c('','intern','extern','Fallzahl','1000','500','Mittelwert','120','180')
#  dim(output) <- c(3,3)
#  xhtml.table(output, file=paste0(xml.dir,'discharge.d.xml'))
#}, silent=FALSE)

try({
  table_formatted <- table(df$triage.result,useNA = "ifany") #NA is already mapped
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'triage.xml'),align=c('left','right','right'),widths=c(20,15,15))
  
  graph <- barchart( a, horizontal=FALSE, xlab="Ersteinschätzung", ylab='Anzahl Patienten',col=c("firebrick3","orange","yellow","green","blue","white"),origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'triage',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

# TODO make sure that this works with NO data
try({ ##this does not work if not all triage levels are present in the data; aggregate coerces df$triage.result to factor, losing all unused levels :(
  agg.funs <- list(Mittelwert=mean, Median=median, Minimum=min, Maximum=max)
  agg.list <- lapply(agg.funs, function(fun){aggregate(x=df$phys.d, by=list(triage=df$triage.result),FUN=fun, na.rm=TRUE)$x})
  agg.list$Mittelwert <- round(agg.list$Mittelwert,1)
  agg.list$Median <- round(agg.list$Median,0)
  agg.length <- aggregate(x=list(count=df$phys.d), by=list(triage=df$triage.result), FUN=length)
  x <- data.frame(Kategorie=levels(df$triage.result), Anzahl=agg.length$count, agg.list)
  rm(agg.funs, agg.list)
  #x[,3] <- paste(x[,3],'min')
  xhtml.table(x, file=paste0(xml.dir,'triage.phys.d.xml'),align=c('left','right','right','right','right','right'),width=15)
}, silent=FALSE)

#Entlassung & Co
try({
  discharge_table <- table(df$discharge,useNA = "always")[c(12,8,10,13,9,11,4,5,6,3,2,1,7,14)] #reorder rows
  names(discharge_table)[length(names(discharge_table))]<-'Keine Daten'
  graph <- barchart(discharge_table[c(13:1)], xlab="Anzahl Patienten",col=std_cols1,origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'discharge',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)
try({
  b <- data.frame(Kategorie=names(discharge_table), Anzahl=gformat(discharge_table), Anteil=gformat((discharge_table / sum(discharge_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(discharge_table)),Anteil=gformat(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'discharge.xml'),align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)

#TOP20 CEDIS
try({
  t <- table(df$cedis,useNA = "always") #frequencies
  y <- t
  #remove it and put it at the bottom - "TOP20+Unknown+NA"
  y[names(y)=='999'] <- 0
  y[is.na(names(y))] <- 0
  y <- sort(y, decreasing = TRUE)
  y <- y [1:20]
  y[y==0] <- NA #remove unused
  y <- y[complete.cases(y)] #remove unused
  x <- data.frame(y)
  x <- rbind(x, data.frame(Var1='999',Freq=t['999'], row.names=NULL))
  x <- rbind(x, data.frame(Var1='NA',Freq=t[is.na(names(t))], row.names=NULL))
  #names(y) <- factor(names(y),t(cedis[1]),labels=t(cedis[3]))
  #names(y)[is.na(names(y))] <- 'Keine Daten'
  graph <- barchart(data=x, Var1 ~ Freq, xlab="Anzahl Patienten",col=std_cols1,origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'cedis_top',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
  
  y <- x
  y$label <- as.character(factor(y$Var1, levels=cedis[[1]], labels=cedis[[3]]))
  y$label[is.na(y$label)] <- "Vorstellungsgrund nicht dokumentiert"
  kat <- paste(y$Var1,y$label, sep = ': ')
  b <- data.frame(Kategorie=kat, Anzahl=gformat(y$Freq), Anteil=gformat((y$Freq / sum(t))*100,digits = 1))
  #b <- rbind(b, data.frame(Kategorie="Vorstellungsgrund nicht dokumentiert",Anzahl=gformat(sum(is.na(df$cedis))), Anteil=gformat((sum(is.na(df$cedis)) / length(df$cedis))*100,digits = 1)))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(y$Freq)),Anteil=gformat(sum(y$Freq) / length(df$cedis)*100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'cedis.xml'),align=c('left','right','right'),widths=c(60,15,15))
}, silent=FALSE)

#CEDIS Groups
#This causes warnings (duplicate levels in factors, which is ok here), but it works :)
# xxx ToDo: what if there are lots of NAs?
try({
  cedis_cat_top <- factor(x=enc$cedis,t(cedis[1]),labels=t(cedis[2])) #map Categories
  #cedis_cat_top <- factor(x=cedis_cat_top,t(cedis[1]),labels=t(cedis[3]))   #map Labels
  #cedis_cat_top <- droplevels(cedis_cat_top) #not ideal, only showing used categories
  #cedis_cat_top <- sort(cedis_cat_top, decreasing = FALSE)
  x <- factor(cedis_cat_top)
  levels(x) <- list("Kardiovaskulaer"="CV","HNO (Ohren)"="HNE","HNO (Mund, Rachen, Hals)"="HNM","HNO (Nase)"="HNN","Umweltbedingt"="EV","Gastrointestinal"="GI","Urogenital"="GU","Psychische Verfassung"="MH","Neurologisch"="NC","Geburtshilfe/Gynaekologie"="GY","Augenheilkunde"="EC","Orthopaedisch/Unfall-chirurgisch"="OC","Respiratorisch"="RC","Haut"="SK","Substanzmissbrauch"="SA","Allgemeine und sonstige Beschwerden"="MC","Patient vor Ersteinschaetzung wieder gegangen"="998","Unbekannt"="999")
  x <- table(x,useNA = 'always')
  names(x)[length(x)] <- "Vorstellungsgrund nicht dokumentiert"
  graph <- barchart(rev(x),xlab="Anzahl Patienten",col=std_cols1,origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'cedis_groups',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

#TOP20 ICD
try({
  ## Old simple Version (still necessary, table is based only on "F" data)
  f_diag <- df_diag$diagnosis[df_diag$fuehrend=='F' & !is.na(df_diag$fuehrend)] 
  t <- table(f_diag,useNA = "no") #frequencies
  x <- sort(t, decreasing = TRUE)
  names(x)[is.na(names(x))] <- 'NA'
  
  #calculate table for stacked barchart based on Zusatzkennzeichen for all diagnoses with "F"
  icd_stacked <- data.frame(mod_F=df_diag$diagnosis,mod_G=df_diag$diagnosis,mod_V=df_diag$diagnosis,mod_Z=df_diag$diagnosis,mod_A=df_diag$diagnosis)
  #remove all diagnoses that are not 'F'
  icd_stacked$mod_F[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_G[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_V[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_Z[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_A[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  #remove all diagnoses that have the wrong modifier
  icd_stacked$mod_G[(!df_diag$zusatz=='G') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_V[(!df_diag$zusatz=='V') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_Z[(!df_diag$zusatz=='Z') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_A[(!df_diag$zusatz=='A') | is.na(df_diag$zusatz)] <- NA
  #remove all diagnoses from 'mod_F' that have a modifier
  icd_stacked$mod_F[df_diag$zusatz =='G'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='V'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='Z'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='A'] <- NA
  #lots of silly transformations to get a matrix that is plotable as a stacked barchart
  stacktable <- data.frame(diag=names(table(icd_stacked$mod_F)),F=as.vector(table(icd_stacked$mod_F)),G=as.vector(table(icd_stacked$mod_G)),V=as.vector(table(icd_stacked$mod_V)),Z=as.vector(table(icd_stacked$mod_Z)),A=as.vector(table(icd_stacked$mod_A)))
  diag_order <- order(table(f_diag,useNA = "always"),decreasing = TRUE)
  stacktable <- t(stacktable[diag_order[1:20],])
  colnames(stacktable) <- stacktable[1,]
  stacktable <- stacktable[2:6,]
  stacktable <- apply(stacktable,2,as.numeric)
  rownames(stacktable) <- c("F","G","V","Z","A")
  stacktable <- t(stacktable)
  stacktable <- stacktable[complete.cases(stacktable),] #remove rows
  graph <- barchart(stacktable[dim(stacktable)[1]:1,1:5],xlab="Anzahl Patienten",sub="blau=Ohne Zusatzkennzeichen, grün=Gesichert, gelb=Verdacht, orange=Z.n., rot=Ausschluss",col=std_cols5[5:1],origin=0)
  #graph <- barchart( x [20:1], xlab="Anzahl Patienten",col=std_cols[1],origin=0)
  trellis.device('svg',file=paste0(gfx.dir,'icd_top',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
  
  a <- t
  a <- sort(a, decreasing = TRUE)
  a <- a [1:20]
  codes <- names(a)
  names(a) <- factor(names(a),t(icd[1]),labels=strtrim(t(icd[2]),60))
  a <- a[complete.cases(a)]
  codes <-  codes[complete.cases(codes)]
  kat <- paste(codes,": ",names(a),sep = '')
  b <- data.frame(Kategorie=kat, Anzahl=gformat(a), Anteil=gformat((a / sum(t))*100,digits = 1))
  b <- rbind(b, data.frame(Kategorie="Nicht dokumentiert",Anzahl=gformat(sum(is.na(f_diag))), Anteil=gformat((sum(is.na(f_diag)) / length(f_diag))*100,digits = 1)))
  ges <- sum(is.na(f_diag))+sum(a)
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(ges),Anteil=gformat((ges / length(f_diag)*100),digits = 1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'icd.xml'),align=c('left','right','right'),widths=c(70,15,15))
  
}, silent=FALSE)
  

#Patient Sex
try({
  table_formatted <- table(df$sex,useNA = "always")
  names(table_formatted) <- c('Weiblich', 'Maennlich','Keine Angabe')
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
  ##c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'sex.xml'),align=c('left','right','right'),widths=c(25,15,15))
}, silent=FALSE)

#Patient Age
try({
  Kennzahl <- c('Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Alter <- c(round(mean(na.omit(df$age)),0),median(na.omit(df$age)),round(stdabw(na.omit(df$age)),0),min(na.omit(df$age)),max(na.omit(df$age)))
  #Alter <- sprintf(fmt="%.1f",Alter)
  Alter <- paste(Alter,'Jahre')
  b <- data.frame(Kennzahl,Alter)
  xhtml.table(b, file=paste0(xml.dir,'age.xml'),align=c('left','right'),widths=c(30,15))
}, silent=FALSE)
try({
  x<-df$age
  x[x>110] <- 110
  x[x<0] <- NA
  x<-x[!is.na(x)]
  graph <- histogram(x,xlab="Alter [Jahre]",ylab="Anzahl Patienten",type='count',breaks=seq(0,110,length=12),sub='Werte größer 110 werden als 110 gewertet',col=std_cols1)
  trellis.device('svg',file=paste0(gfx.dir,'age',gfx.ext),width=8,height=4)
  print(graph)
  no_output <- dev.off() #silent
}, silent=FALSE)

#Admit Day
try({
  Datum <- names(table(format(as.Date(df$admit.day), '%d.%m.%Y')))
  Wochentag <- weekdays(as.Date(names(table(df$admit.day))))
  Anzahl <- as.vector(table(df$admit.day))
  b <- data.frame(Datum,Wochentag,Anzahl)
  xhtml.table(b, file=paste0(xml.dir,'admit.d.xml'),align=c('left','right','right'),widths=c(25,15,13))
}, silent=FALSE)

if (FALSE) { ###long comment xxx todo: fix/test crowding

#calculate number of patients per hour of day
try({
  admit <- as.numeric(df$admit.h)-1  
  admit <- admit[!is.na(df$admit.h)]
  admit <- admit[!is.na(df$discharge.h)]
  discharge <- as.numeric(df$discharge.h)-1 
  discharge <- discharge[!is.na(df$admit.h)]
  discharge <- discharge[!is.na(df$discharge.h)]
  crowding <- rep(0,24)
  for (i in 1:length(admit)){
    if (!is.na(admit[i]) & !is.na(discharge[i])) {
      if (admit[i] < discharge[i]) {
        for (j in (admit[i]+1):(discharge[i]+1)) {
          crowding[j] <- crowding[j] +1
        }
      }
      if (admit[i] >= discharge[i]) {
        for (j in (admit[i]+1):24) {
          crowding[j] <- crowding[j] +1
        }
        for (j in 1:(discharge[i]+1)) {
          crowding[j] <- crowding[j] +1
        }
      }
    }
  }
}, silent=FALSE)

#Alternative Crowding Calc
reftime <- Sys.time()
crowd_alt <- rep(0,length(df$admit.day)*4) #enough array space for NA +/-
events_alt <- rep(reftime,length(df$admit.day)*4)
crowd <- data.frame(count=crowd_alt,time=events_alt,index=crowd_alt)
come_length <- length(df$admit.ts)+1
go_length <- length(df$discharge.ts)+1
come_index <- 1
go_index <- 1
patcount <- 0 # arbitrary start (avg?!)
eventcount <- 1
while ((come_index < come_length) & (go_index < go_length)) {
  if (is.na(df$discharge.ts[go_index])) #testing, better cleaning needed
  {
    crowd$count[eventcount] <- -1
    crowd$index[eventcount] <- go_index
    crowd$time[eventcount] <- df$admit.ts[go_index] #substract admit because of NA in discharge +1-1=0
    eventcount  <- eventcount+1
    patcount <- patcount-1
    go_index <- go_index+1
  } else {
    if (df$admit.ts[come_index] < df$discharge.ts[go_index]) { #next event is a "come"
      crowd$count[eventcount] <- 1
      crowd$index[eventcount] <- come_index
      crowd$time[eventcount] <- df$admit.ts[come_index]
      eventcount  <- eventcount+1
      patcount <- patcount +1;
      come_index <- come_index+1
    } else { #next event is a "go"
      crowd$count[eventcount] <- -1
      crowd$index[eventcount] <- go_index
      crowd$time[eventcount] <- df$discharge.ts[go_index]
      eventcount  <- eventcount+1
      patcount <- patcount-1
      go_index <- go_index+1
    }
  }
}
while ((go_index < go_length)) { #patients leaving after come_index is through
  if (is.na(df$discharge.ts[go_index])) #testing, better cleaning needed
  {
    crowd$count[eventcount] <- -1
    crowd$index[eventcount] <- go_index
    crowd$time[eventcount] <- df$admit.ts[go_index] #substract admit because of NA in discharge +1-1=0
    eventcount  <- eventcount+1
    patcount <- patcount-1
    go_index <- go_index+1
  } else {
    if (df$discharge.ts[go_index] < df$discharge.ts[2218]) { #"go" is before end of the month
      crowd$count[eventcount] <- -1
      crowd$index[eventcount] <- go_index
      crowd$time[eventcount] <- df$discharge.ts[go_index]
      eventcount  <- eventcount+1
      patcount <- patcount-1
      go_index <- go_index+1
    }
    else { #check next
      go_index <- go_index+1
    }
  }
}
count_sorted <- crowd$count[order(crowd$time)] #+1/-1 in correct order
time_sorted <- crowd$time[order(crowd$time)] #times in correct order
index_sorted <- crowd$index[order(crowd$time)] #index in correct order
crowdperday <- matrix(0,length(levels(df$admit.day)),24)
crowdperday_max <- matrix(0,length(levels(df$admit.day)),24)
crowdperday_min <- matrix(0,length(levels(df$admit.day)),24)
patcount <- 0
i <- 1
ts_first <- df$admit.ts[1] #xxx
ts_last <- df$discharge.ts[2214] #xxx
crowdtime.day <- factor(x=strftime(time_sorted[time_sorted >= ts_first],format="%F"), ordered=TRUE)
crowdtime.h <- factor(x=strftime(time_sorted,format="%H"), levels=hour.levels, ordered=TRUE)
while (time_sorted[i] < ts_first) {
  i <- i+1
}
for (d in 1:length(levels(df$admit.day))) {
  for (h in 1:24) {
    crowdperday[d,h] <- patcount
    crowdperday_max[d,h] <- patcount
    crowdperday_min[d,h] <- patcount
    while ((as.integer(crowdtime.day[i]) <= d) && (as.integer(crowdtime.h[i]) <= h) && (crowd$time <= ts_last)){
      if (count_sorted[i] == 1) {
        patcount = patcount +1
        crowdperday[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])] <- patcount
        if (patcount > crowdperday_max[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])]) {
          crowdperday_max[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])] <- patcount
        }
      }
      if (count_sorted[i] == -1) {
        patcount = patcount -1
        crowdperday[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])] <- patcount
        if (patcount < crowdperday_min[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])]) {
          crowdperday_min[as.integer(df$admit.day[index_sorted[i]]),as.integer(df$admit.h[index_sorted[i]])] <- patcount
        }
      }
      i <- i+1
    }
  }
}

#calculate Top Counts of Patients in ER (crowding)
try({
  crowdperday = matrix(0,length(levels(df$admit.day)),24)
  index <- rep(1:length(levels(df$admit.day)))
  d <- 1
  for (i in 1:length(admit)){
    if (i > 1) {
      if (df$admit.day[i] > df$admit.day[i-1]) {
        d <- d+1
      }
    }
    if (!is.na(df$admit.ts[i]) & !is.na(df$discharge.ts[i]) & (df$admit.ts[i] < df$discharge.ts[i])) {
      if (df$admit.h[i] < df$discharge.h[i]) {
        for (j in (admit[i]+1):(discharge[i]+1)) { 
          crowdperday[d,j] <- crowdperday[d,j] +1
        }
      }
      if (df$admit.h[i] >= df$discharge.h[i]) {
        for (j in (admit[i]+1):24) {
          crowdperday[d,j] <- crowdperday[d,j] +1
        }
        for (j in 1:(discharge[i]+1)) {
          if (d < length(levels(df$admit.day))) {
            crowdperday[d+1,j] <- crowdperday[d+1,j] +1
          }
        }
      }
    }
  }
  pmax <- apply (crowdperday,1,max)
  whichmax=apply(crowdperday,1,which.max)
  pmin <- apply (crowdperday,1,min)
  whichmin=apply(crowdperday,1,which.min)
  pmean <- round(apply (crowdperday,1,mean),digits=0)
  c <- data.frame(Datum,MaxTime=paste(whichmax,'Uhr'),MaxPat=pmax,MinTime=paste(whichmin,'Uhr'),MinPat=pmin,MeanPat=pmean)
  xhtml.table(c, file=paste0(xml.dir,'crowding.d.xml'),align=c('left','right','right','right','right','right'),widths=c(15,15,15,15,15,15))
}, silent=FALSE)

#calculate length of observation based on admission day
try({
  crowd.len <- max(df$admit.ts) - min(df$admit.ts)
  #plot(apply (crowdperday,2,max),xlab = 'Uhrzeit [Stunde]',ylab='Anwesende Patienten',ylim=c(min(apply (crowdperday,2,min)),max(apply (crowdperday,2,max))),sub='rot=Maximum, blau=Minimum, grün=Durchschnitt')
  #lines(crowding/as.numeric(round(crowd.len)),type="b",col=colors[2])
  #lines(apply (crowdperday,2,max),type="b",col=colors[1])
  #lines(apply (crowdperday,2,min),type="b",col=colors[3])
  svg(paste0(gfx.dir,'crowding','.svg'))
  plot(apply(crowdperday_max,1,max),xlab = 'Uhrzeit [Stunde]',ylab='Anwesende Patienten',ylim=c(min(apply(crowdperday_min,1,min)),max(apply(crowdperday_max,1,max))),sub='rot=Maximum, blau=Minimum, grün=Durchschnitt')
  lines(colSums(crowdperday)/as.numeric(round(crowd.len)),type="b",col=std_cols3[2])
  lines(apply(crowdperday_max,1,max),type="b",col=std_cols3[1])
  lines(apply(crowdperday_min,1,min),type="b",col=std_cols3[3])
  no_output <- dev.off() #silent
}, silent=FALSE)


#Aufnahme vs. Entlassung
#Testberechnung für einen Tag
try({
  admits <- rep(0,24)
  discharges <- rep(0,24)
  for (i in 1:length(df$admit.h)){
    if (!is.na(df$admit.day[i])) {
      if (as.Date(df$admit.day[i],'%Y-%m-%d') == as.Date('2015-11-27','%Y-%m-%d')) {
        hour_admit <- df$admit.h[i]
        admits[hour_admit] <- admits[hour_admit]  + 1
      }
    }
    if (!is.na(df$discharge.day[i])) {
      if (as.Date(df$discharge.day[i],'%Y-%m-%d') == as.Date('2015-11-27','%Y-%m-%d')) {
        hour_discharge <- df$discharge.h[i]
        discharges[hour_discharge] <- discharges[hour_discharge]  + 1
      }
    }
  }
  svg(paste0(gfx.dir,'admit_discharge','.svg'))
  plot(crowdperday[27,],xlab = 'Uhrzeit [Stunde]',ylab='Durchschnittliche Anzahl Patienten',ylim=c(0,65),sub='rot=Anzahl Patienten, blau=Aufnahmen, grün=Entlassungen')
  lines(crowdperday[27,],type="b",col=std_cols3[1])
  lines(admits,type="b",col=std_cols3[3])
  lines(discharges,type="b",col=std_cols3[2])
  no_output <- dev.off() #silent
}, silent=FALSE)


}#### end long comment
  
try({
  
  df$los = df$discharge.ts - df$admit.ts
  lt_zero <- length(df$los[df$los < 0]) - length(df$los[is.na(df$los)])
  gt_day <- length(df$los[df$los > 24*60]) - length(df$los[is.na(df$los)])
  los_NA <- sum(is.na(df$los))
  los <- df$los[df$los<=10*60]
  los <- los[!is.na(los)]
  los <- los[los>0]
  los_invalid <- sum(is.na(df$discharge.d)) - sum(is.na(df$los))
  los_removed <- length(df$los)-length(los)-los_NA-los_invalid
  los_erfasst=length(los)
  
  Kennzahl <- c('Anzahl Zeiten beruecksichtigt','Anzahl fehlende Zeiten','Anzahl ungueltige Werte (<0h)','Anzahl ungueltige Werte (>24h)','Anzahl zwischen 10h und 24h','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(round(mean(na.omit(los)),1),median(na.omit(los)),round(stdabw(na.omit(los)),1),min(na.omit(los)),max(na.omit(los)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(los_erfasst,los_NA,lt_zero,gt_day,los_removed,Zeit)
 
  b <- data.frame(Kennzahl,Zeit)
  xhtml.table(b, file=paste0(xml.dir,'los.xml'),align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

#Isolierte Patienten
try({
  df$combined_iso <- factor(enc$isolation_grund) #Code ISO is redundant with Reason; CAVE: if Reverse and Reason are given, only Reverse is used
  levels(df$combined_iso) <- list("Keine Isolation"="ISO:NEG","Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Umkehrisolation"="RISO","Andere"="OTH")
  df$combined_iso[df$isolation == 'Keine Isolation'] <- 'Keine Isolation'
  df$combined_iso[df$isolation == 'Umkehrisolation'] <- 'Umkehrisolation'
  
  isoreason_table <- table(df$combined_iso,useNA = "always")
  names(isoreason_table)[length(names(isoreason_table))]<-'Keine Daten'
  b <- data.frame(Kategorie=names(isoreason_table), Anzahl=gformat(isoreason_table), Anteil=gformat((isoreason_table / sum(isoreason_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(isoreason_table)),Anteil=gformat(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  xhtml.table(c, file=paste0(xml.dir,'isoreason.xml'),align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)

#Multiresistente Erreger
try({
  mrsa_patient <- cbind(df$keime,df$keime_mrsa,df$keime_3mrgn,df$keime_4mrgn,df$keime_vre)
  mrsa_patient <- rowSums(mrsa_patient,na.rm=TRUE)
  mrsa_table <-                   data.frame(Kategorie="MRSA", Bekannt=sum(df$keime_mrsa == 'MRSA',na.rm=TRUE), Verdacht=sum(df$keime_mrsa == 'MRSA:SUSP',na.rm=TRUE))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="3-MRGN",Bekannt=sum(df$keime_3mrgn == '3MRGN',na.rm=TRUE),Verdacht=sum(df$keime_mrsa == '3MRGN:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="4-MRGN",Bekannt=sum(df$keime_3mrgn == '4MRGN',na.rm=TRUE),Verdacht=sum(df$keime_mrsa == '4MRGN:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="VRE",Bekannt=sum(df$keime_3mrgn == 'VRE',na.rm=TRUE),Verdacht=sum(df$keime_mrsa == 'VRE:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Andere",Bekannt=sum(df$keime_3mrgn == 'OTH',na.rm=TRUE),Verdacht=sum(df$keime_mrsa == 'OTH:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Keine Keime",Bekannt=sum(df$keime == 'AMRO:NEG',na.rm=TRUE),Verdacht='-'))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Keine Angabe",Bekannt=sum(mrsa_patient<1),Verdacht='-'))
  xhtml.table(mrsa_table, file=paste0(xml.dir,'multiresistant.xml'),align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)
