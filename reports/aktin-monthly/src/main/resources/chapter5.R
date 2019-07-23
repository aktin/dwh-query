
try({
  a <- df$phys.d[df$phys.d<180]
  #outofbounds <- length(df$phys.d) - length(a)
  isNA <- length(df$phys.d[is.na(df$phys.d )])
  b <- a[!is.na(a)]
  positiveoutofbounds <- length(df$phys.ts[difftime(df$phys.ts,df$admit.ts,units="mins") > 60])
  negativeoutofbounds <- length(df$phys.ts[difftime(df$phys.ts,df$admit.ts,units="mins") < 0])
  #Absolute Zahlen
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,180,length=13),scales = list(x = list(at = seq(0,180,length=7))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 180 Minuten: ", outofbounds))
  #Relative Häufigkeiten
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Arztkontakt [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,180,length=13),scales = list(x = list(at = seq(0,180,length=7))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 180 Minuten: ", outofbounds),col=std_cols1)
  report.svg(graph, 'phys.d.hist')
}, silent=FALSE)

try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- factors$phys_txt[!is.na(factors$phys_txt)]
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,positiveoutofbounds,negativeoutofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,name='phys.d.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to triage
try({
  a <- df$triage.d[df$triage.d<60]
  #outofbounds <- length(df$triage.d) - length(a)
  isNA <- length(df$triage.d[is.na(difftime(df$triage.ts,df$admit.ts,units="mins"))])
  b <- a[!is.na(a)]
  positiveoutofbounds <- length(df$triage.ts[difftime(df$triage.ts,df$admit.ts,units="mins") > 60])
  negativeoutofbounds <- length(df$triage.ts[difftime(df$triage.ts,df$admit.ts,units="mins") < 0])
  #Absolute Zahlen
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Triage [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,60,length=13),sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds))
  #Relative Häufigkeiten
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis Triage [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,60,length=13),sub=paste("Fehlende Werte: ", isNA, "; Werte über 60 Minuten: ", outofbounds),col=std_cols1)
  report.svg(graph, 'triage.d.hist')
}, silent=FALSE)

try({
  used <- length(a)-length(a[is.na(a)])
  Kennzahl <- factors$triage_txt[!is.na(factors$triage_txt)]
  Zeit <- c(round(mean(na.omit(a)),1),median(na.omit(a)),round(stdabw(na.omit(a)),1),min(na.omit(a)),max(na.omit(a)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,positiveoutofbounds,negativeoutofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,name='triage.d.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to physician mean grouped by triage result
try({
  a <- df$phys.d[df$phys.d<180]
  outofbounds <- length(df$phys.d) - length(a)
  b <- df$triage.result[df$phys.d<180]
  y <- data.frame(time=a, triage=b)
  x <- with(y, tapply(time, list(triage), FUN=mean, na.rm=TRUE))
  x[is.na(x)] <- 0
  y <- data.frame(avg=as.numeric(x), triage=factor(names(x), levels=factors$Triage[!is.na(factors$Triage)]))
  # TODO error below: Error in units == (un1 <- units[1L]), comparison of these types is not implemented
  graph <- barchart(avg ~ triage, data=y, horizontal=FALSE, col=std_cols5 ,ylab="Durchschn. Zeit bis Arztkontakt [Min.]", xlab="Triage-Gruppe",sub=paste("Werte über 180 Minuten (unberücksichtigt): ", outofbounds),ylim=c(0,signif(max(as.numeric(y$avg)),digits=1) +10),origin=0)
  #text(graph,x$triage,labels = x$triage,pos=1)
  report.svg(graph, 'triage.phys.d.avg')
}, silent=FALSE)


try({
  y2 <- data.frame(triage=df$triage.result, time=df$phys.d)
  
  agg.funs <- list(Mittelwert=mean, Median=median, Minimum=min, Maximum=max)
  agg.list <- lapply(agg.funs, function(fun){
    with(y2, tapply(time, list(triage), FUN=fun, na.rm=TRUE))})
    
  agg.list$Mittelwert <- round(agg.list$Mittelwert,1)
  agg.list$Median <- round(agg.list$Median,0)
  agg.list$Anzahl <- with(y2, tapply(time, list(triage), FUN=length))
  agg.list$Anzahl[is.na(agg.list$Anzahl)] <- 0
  kat <- as.character((y$triage))
  x <- data.frame(Kategorie=kat,agg.list)
  rm(agg.funs, agg.list)
  report.table(x,'triage.phys.d.xml',align=c('left','right','right','right','right','right'),width=15)
}, silent=FALSE)