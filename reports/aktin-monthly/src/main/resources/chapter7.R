try({
  
  #df$los = df$discharge.ts - df$admit.ts
  df$los = difftime(df$discharge.ts,df$admit.ts,units="mins")
  lt_zero <- length(df$los[df$los < 0]) - length(df$los[is.na(df$los)])
  gt_day <- length(df$los[df$los > 24*60]) - length(df$los[is.na(df$los)])
  los_NA <- sum(is.na(df$los))
  los <- df$los[df$los<=24*60]
  los <- los[!is.na(los)]
  los <- los[los>0]
  los_invalid <- sum(is.na(df$discharge.d)) - sum(is.na(df$los))
  los_removed <- length(df$los)-length(los)-los_NA-los_invalid
  los_erfasst=length(los)
  
  Kennzahl <- factors$los_txt[!is.na(factors$los_txt)]
  Zeit <- c(round(mean(na.omit(los)),1),median(na.omit(los)),round(stdabw(na.omit(los)),1),min(na.omit(los)),max(na.omit(los)))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(los_erfasst,los_NA,lt_zero,gt_day,Zeit)
 
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,name='los.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to discharge
try({
  a <- df$discharge.d[df$discharge.d<600]
  outofbounds <- length(df$discharge.d) - length(a)
  isNA <- length(df$discharge.d[is.na(df$discharge.d )])
  b <- a[!is.na(a)]
  #graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung [Minuten]",ylab="Anzahl Patienten",type='count',breaks=seq(0,600,length=11),scales = list(x = list(at = seq(0,600,length=11))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 600 Minuten: ", outofbounds),col=std_cols1)
  graph <- histogram(as.numeric(b,unit='mins'),xlab="Zeit von Aufnahme bis zur Entlassung/Verlegung [Minuten]",ylab="Relative Häufigkeit [%]",breaks=seq(0,600,length=11),scales = list(x = list(at = seq(0,600,length=11))),sub=paste("Fehlende Werte: ", isNA, "; Werte über 600 Minuten: ", outofbounds),col=std_cols1)
  report.svg(graph, 'discharge.d.hist')
}, silent=FALSE)