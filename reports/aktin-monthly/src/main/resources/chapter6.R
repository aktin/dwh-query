#Entlassung & Co
try({
  discharge_table <- table(df$discharge,useNA = "always")[c(12,8,10,13,9,11,4,5,6,3,2,1,7,14)] #reorder rows
  names(discharge_table)[length(names(discharge_table))]<-'Keine Daten'
  graph <- barchart(discharge_table[c(13:1)], xlab="Anzahl Patienten",col=std_cols1,origin=0)
  report.svg(graph, 'discharge')
}, silent=FALSE)
try({
  b <- data.frame(Kategorie=factors$Discharge[!is.na(factors$Discharge)], Anzahl=gformat(discharge_table), Anteil=gformat((discharge_table / sum(discharge_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(discharge_table)),Anteil=gformat(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  report.table(c,name='discharge.xml',align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)