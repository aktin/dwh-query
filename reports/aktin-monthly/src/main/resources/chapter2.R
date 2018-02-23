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
#df$admit is not ordered, but for this loop it needs to be ordered by date
tempdf <- df[c("admit.day","admit.wd")]
orderdf <- tempdf[order(tempdf$admit.day),]
weekdaycounts=rep(0,7) #Mo-So
if (length(orderdf$admit.wd) > 0) {
  wbindex <- 0
  lastwd <- 0
  for (i in 1:length(orderdf$admit.wd)){
    if (! is.na(orderdf$admit.wd[i])) {
     if (orderdf$admit.wd[i] != lastwd) {
          wbindex <- as.numeric(sapply(as.character(orderdf$admit.wd[i]), switch, 
                                       Mo = 1, 
                                       Di = 2, 
                                       Mi = 3, 
                                       Do = 4, 
                                       Fr = 5, 
                                       Sa = 6, 
                                       So = 7))
          weekdaycounts[wbindex] <- weekdaycounts[wbindex]+1
          lastwd <- orderdf$admit.wd[i]
        }
    }
  }
}
rm(tempdf,orderdf) 

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
  # c(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
  weekday <- rep(0,24)
  weekend <- rep(0,24)
  for (i in 1:24) {
    weekday[i] <- admit.hwd[1,i]+admit.hwd[2,i]+admit.hwd[3,i]+admit.hwd[4,i]+admit.hwd[5,i]
    weekend[i] <- admit.hwd[6,i]+admit.hwd[7,i]
  }
  days_weekday <- sum(weekdaycounts[1:5])
  days_weekend <- sum(weekdaycounts[6:7])
  svg(paste0(gfx.dir,'admit.hwd.weekend','.svg'))
  plot(y=weekend/days_weekend, x=seq(0,23), type='n', xlab=paste('Uhrzeit [Stunde]; n =',(sum(weekday)+sum(weekend))), ylab="Anzahl Patienten",ylim=c(0,max(weekday/days_weekday,weekend/days_weekend)),xaxp=c(0,24,12))
  
  lines(y=weekday/days_weekday, x=seq(0,23), type="b",col=std_cols3[2],pch=15)
  lines(y=weekend/days_weekend, x=seq(0,23), type="b",col=std_cols3[1],pch=17)

  #legend('topleft',1:length(weekday.levels), legend=weekday.levels, cex=0.8, col=colors, title="Tage")
  no_output <- dev.off() #silent
  report.generatedFile('admit.hwd.weekend.svg')
}, silent=FALSE)

#Admit Day
try({
  Datum <- names(table(format(df$admit.day,format='%d.%m.%Y',tz='GMT')))
  #Wochentag <- weekdays(as.Date(names(table(df$admit.day))))
  Wochentag <- format(as.Date(names(table(df$admit.day))),format='%u',tz='GMT')
  Wochentag <- factor(Wochentag)
  levels(Wochentag) <- list("Montag"="1","Dienstag"="2","Mittwoch"="3","Donnerstag"="4","Freitag"="5","Samstag"="6", "Sonntag"=7)
  Anzahl <- as.vector(table(format(df$admit.day,format='%d.%m.%Y',tz='GMT')))
  b <- data.frame(Datum,Wochentag,Anzahl)
  report.table(b,name='admit.d.xml',align=c('left','right','right'),widths=c(25,15,13))
}, silent=FALSE)

#calculate number of patients per hour of day (per day) -- Crowding
try({
  admit <- as.numeric(df$admit.h)-1  #[0-23] admit is always available, but admit.h may not be available if the precision is only yyyymmdd
  #admit <- admit[!is.na(df$admit.h)] #no good
  #admit <- admit[!is.na(df$discharge.h)] #no good
  discharge <- as.numeric(df$discharge.h)-1 #[0-23] discharge.h might be missing
  #discharge <- discharge[!is.na(df$admit.h)] #no good
  #discharge <- discharge[!is.na(df$discharge.h)] #no good
  num_days <- length(names(table(format(df$admit.day,format='%d.%m.%Y',tz='GMT'))))
  mcrowding <- matrix(data = 0, nrow = num_days, ncol = 24, byrow = FALSE, dimnames = NULL)
  for (i in 1:length(admit)){
    if (!is.na(admit[i]) & !is.na(discharge[i]) & (df$admit.ts[i] <= df$discharge.ts[i])) { #admit & discharge hours available and admit <= discharge
      j <- admit[i]+1 #admit hour
      d <- as.numeric(df$admit.day[i]) #index of admission day
      c_hours <- 0
      c_days <- 0
      repeat {
        mcrowding[d,j] <- mcrowding[d,j] +1
        c_hours <- c_hours + 1
        j <- j + 1
        if (j > 24) {
          j <- 1
          d <- d+1
          c_days <- c_days + 1
          c_hours <- 0
        }
        if (as.numeric(strftime(df$admit.ts[i],tz="GMT",format="%H"))+c_hours > as.numeric(strftime(df$discharge.ts[i],tz="GMT",format="%H"))
            & (as.Date(df$admit.ts[i])+c_days >= as.Date(df$discharge.ts[i]) )){
          break
        }
      }
    }
    if (!is.na(admit[i]) & is.na(discharge[i])) { #admit available but no discharge => only count admit.h (1 hour stay)
      j <- admit[i]+1 #admit hour
      d <- as.numeric(df$admit.day[i]) #index of admission day
      mcrowding[d,j] <- mcrowding[d,j] +1
    }
  }
  
  #no output yet
  
  #bwplot(mcrowding[1:30,19])
  #bwplot(mcrowding[1:30,1])
}, silent=FALSE)
