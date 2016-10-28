#!/usr/bin/Rscript

report.generatedFile <- function(name){
	cat(paste(name,"\n",sep=""), file="r-generated-files.txt", append=TRUE)
}
gfx.dir <- ''
gfx.ext <- '.svg'
gfx.dev <- 'svg'
report.svg <- function(graph, name, width=8, height=4){
  trellis.device(gfx.dev,file=paste0(gfx.dir,name,gfx.ext), width=width,height=height)
  print(graph)
  no_output <- dev.off() #silent
  report.generatedFile(paste0(name,gfx.ext))
}
xml.dir <- ''
report.table <- function(x, name, widths=NULL, align='auto', align.default='left', na.subst=''){
  xhtml.table(x, file=paste0(xml.dir, name), widths=widths, align=align, align.default=align.default, na.subst=na.subst)
  report.generatedFile(name)
}

std_cols1 <- c("firebrick3")
std_cols3 <- c("firebrick3","dodgerblue","forestgreen")
std_cols5 <- c("firebrick3","orange","yellow","forestgreen","dodgerblue","aliceblue")

#std_cols3 <- c("firebrick3","blue","green")
#std_cols5 <- c("firebrick3","orange","yellow","green","blue","white")

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
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='', colClasses = "character", encoding = "UTF-8")
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='', colClasses = "character", encoding = "UTF-8")
diag <- read.table(file='diagnoses.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='',stringsAsFactors=FALSE, colClasses = "character", encoding = "UTF-8")

# create new data frame for encounter data to contain clean values
df = data.frame(patient=pat$patient_id, encounter=enc$encounter_id)

# create new data frame for diagnoses data to contain clean values
df_diag <- data.frame(diagnosis=as.factor(substring(diag$diagnose_code, first=1, last=3)))

#load CEDIS mapping table
cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';', colClasses = "character", encoding = "UTF-8")

#load ICD mapping table
icd = read.csv2(file='ICD-3Steller.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';', colClasses = "character", encoding = "UTF-8")

#load special factors (problems with umlauts if umlauts are placed in factor texts directly in R file)
factors = read.csv2(file='factors.csv', as.is=TRUE, na.strings='', header = TRUE, sep=';', encoding = "UTF-8")

source("parse_derive.R", encoding="UTF-8")

# XHTML tables
source('xhtml-table.R')

# Graphics & Plots
library(lattice)

source("chapter1.R", encoding="UTF-8")
source("chapter2.R", encoding="UTF-8")
source("chapter3.R", encoding="UTF-8")
source("chapter4.R", encoding="UTF-8")
source("chapter5.R", encoding="UTF-8")
source("chapter6.R", encoding="UTF-8")
source("chapter7.R", encoding="UTF-8")
source("chapter8.R", encoding="UTF-8")
source("chapter9.R", encoding="UTF-8")



#################################################################
###starting from here everything is dead code (unfinished/unused)
#################################################################
if (FALSE) { ##not used

  
  
# Time physician to therapy
tryCatch({
  a <- df$therapy.d[df$therapy.d<60]
  outofbounds <- length(df$therapy.d) - length(a)
  isNA <- length(df$therapy.d[is.na(df$therapy.d )])
  b <- a[!is.na(a)]
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit vom ersten Arztkontakt bis zum Therapiebeginn [Minuten]",ylab="relative Häufigkeit [%]",sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  report.svg(graph, 'therapy.d.hist')
}, error = function(err) {
  par(mar = c(0,0,0,0))
  svg(paste0(gfx.dir,'therapy.d.hist','.svg'))
  plot(c(0, 1), c(0, 1), ann = F, bty = 'n', type = 'n', xaxt = 'n', yaxt = 'n')
  no_output <- dev.off()
  par(mar = c(5, 4, 4, 2) + 0.1)
  #ecode <- file.copy(paste0(gfx.dir,'dummy',gfx.ext),paste0(gfx.dir,'therapy.d.hist',gfx.ext))
  return(paste("R-Plot failed: ",err))  #only for debug
}, silent=TRUE)


  
try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- c('Anzahl Zeiten beruecksichtigt','Anzahl fehlende Zeiten','Anzahl ungueltige Werte (>60min)','Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,outofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,'therapy.d.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

  

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
  report.table(c,name='crowding.d.xml',align=c('left','right','right','right','right','right'),widths=c(15,15,15,15,15,15))
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
