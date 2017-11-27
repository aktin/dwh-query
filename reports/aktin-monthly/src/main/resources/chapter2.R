# Counts per Hour
try({
  graph <- barchart(table(df$admit.h)/length(levels(df$admit.day)), horizontal=FALSE, xlab=paste('Uhrzeit [Stunde]; n =',sum(!is.na(df$admit.h))), ylab="Durchschnittliche Anzahl Patienten",col=std_cols1,origin=0)
  report.svg(graph, 'admit.h')
}, silent=FALSE)
# Write table
try({
  table_pretty <- format(round(table(df$admit.h)[1:12]/length(levels(df$admit.day)), 1), nsmall=1, big.mark=".")
  report.table(table_pretty,name='admit.h.xml',align='center')
  table_pretty <- format(round(table(df$admit.h)[13:24]/length(levels(df$admit.day)), 1), nsmall=1, big.mark=".")
  report.table(table_pretty,name='admit2.h.xml',align='center')
}, silent=FALSE)

#calculate number of weekdays in the current period (month)
#limitation/feature: days without patients will be excluded
weekdaycounts=rep(0,7) #Mo-So
if (length(df$admit.wd) > 0) {
  wbindex <- 0
  lastwd <- 0
  for (i in 1:length(df$admit.wd)){
    if (! is.na(df$admit.wd[i])) {
     if (df$admit.wd[i] != lastwd) {
          wbindex <- as.numeric(sapply(as.character(df$admit.wd[i]), switch, 
                                       Mo = 1, 
                                       Di = 2, 
                                       Mi = 3, 
                                       Do = 4, 
                                       Fr = 5, 
                                       Sa = 6, 
                                       So = 7))
          weekdaycounts[wbindex] <- weekdaycounts[wbindex]+1
          lastwd <- df$admit.wd[i]
        }
    }
  }
}

# Counts per Weekday
try({
  plottable <- round(table(df$admit.wd)/weekdaycounts,digits = 1)
  plottable[is.na(plottable)] <- 0
  graph <- barchart(plottable, horizontal=FALSE, xlab=paste('Wochentag; n =',sum(!is.na(df$admit.h))), ylab="Durchschnittliche Anzahl Patienten",col=std_cols1,origin=0)
  report.svg(graph, 'admit.wd')
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
  plot(weekend/2, type='n', xlab=paste('Uhrzeit [Stunde]; n =',(sum(weekday)+sum(weekend))), ylab="Anzahl Patienten",ylim=c(0,max(weekday/5,weekend/2)),xaxp=c(0,24,12))
  
  lines(weekday/5,type="b",col=std_cols3[2],pch=15)
  lines(weekend/2,type="b",col=std_cols3[1],pch=17)

  #legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  no_output <- dev.off() #silent
  report.generatedFile('admit.hwd.weekend.svg')
}, silent=FALSE)

#Admit Day
try({
  Datum <- names(table(format(df$admit.day, '%d.%m.%Y',tz='GMT')))
  #Wochentag <- weekdays(as.Date(names(table(df$admit.day))))
  Wochentag <- format(as.Date(names(table(df$admit.day))), '%u',tz='GMT')
  Wochentag <- factor(Wochentag)
  levels(Wochentag) <- list("Montag"="1","Dienstag"="2","Mittwoch"="3","Donnerstag"="4","Freitag"="5","Samstag"="6", "Sonntag"=7)
  Anzahl <- as.vector(table(format(df$admit.day, '%d.%m.%Y',tz='GMT')))
  b <- data.frame(Datum,Wochentag,Anzahl)
  report.table(b,name='admit.d.xml',align=c('left','right','right'),widths=c(25,15,13))
}, silent=FALSE)