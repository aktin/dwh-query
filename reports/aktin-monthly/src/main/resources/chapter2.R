# Counts per Hour
try({
  df2<-data.frame(table(df$admit.h)/length(levels(df$admit.day)))
  graph<-ggplot(data=df2, aes(x=Var1, y=Freq)) +
    geom_bar(stat="identity", fill="#046C9A")+
    labs(x=paste('Uhrzeit [Stunde]; n =',sum(!is.na(df$admit.h))), y = "Durchschnittliche Anzahl Patienten")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_y_continuous(expand = c(0, 0.01))
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
  plottable<-data.frame(plottable)
  graph<-ggplot(data=plottable, aes(x=Var1, y=Freq)) +
    geom_bar(stat="identity", fill="#046C9A")+
    labs(x=paste('Wochentag; n =',sum(!is.na(df$admit.h))), y = "Durchschnittliche Anzahl Patienten")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_y_continuous(expand = c(0, 0.3))
  report.svg(graph, 'admit.wd')
}, silent=FALSE)


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
  
  y3<-c("00","01","02","03","04","05","06","07","08","09","10","11","12","13","14","15","16","17","18","19","20","21","22","23")
  y1<-weekday/days_weekday
  y2<-weekend/days_weekend
  df3<-data.frame(y3,y1)
  df4<-data.frame(y3,y2)
  rm(y1,y2,y3)
  df3$y3 <- as.numeric(as.character(df3$y3))
  df4$y3 <- as.numeric(as.character(df4$y3))
  graph<-ggplot() + 
    geom_line(data = df3, aes(x = y3, y = y1), color = "#890700") +
    geom_line(data = df4, aes(x = y3,y= y2), color = "#FA9B06") +
    xlab('Uhrzeit [Stunde]') +
    ylab('Durchschnittliche Fallzahl')+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_x_continuous(expand = c(0, 1),breaks = seq(0, 23, 2))+
    geom_point(data = df3, aes(x = y3, y = y1), color = "#890700",fill = "#890700",shape=22,size=3)+
    geom_point(data = df4, aes(x = y3,y= y2), color = "#FA9B06",fill = "#FA9B06",shape=24,size=3)
    report.svg(graph, 'admit.hwd.weekend')
 }, silent=FALSE)

#Admit Day
try({
  df2<-subset(df,!is.na(admit.day))
  Datum <- names(table(format(df2$admit.day,format='%d.%m.%Y',tz='GMT')))
  #Wochentag <- weekdays(as.Date(names(table(df$admit.day))))
  Wochentag <- format(as.Date(names(table(df2$admit.day))),format='%u',tz='GMT')
  Wochentag <- factor(Wochentag)
  levels(Wochentag) <- list("Montag"="1","Dienstag"="2","Mittwoch"="3","Donnerstag"="4","Freitag"="5","Samstag"="6", "Sonntag"=7)
  Anzahl <- as.vector(table(format(df2$admit.day,format='%d.%m.%Y',tz='GMT')))
  b <- data.frame(Datum,Wochentag,Anzahl)
  report.table(b,name='admit.d.xml',align=c('left','right','right'),widths=c(25,15,13))
}, silent=FALSE)
