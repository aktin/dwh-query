try({
  table_formatted <- table(df$triage.result,useNA = "ifany") #NA is already mapped
  a <- table_formatted
  b <- data.frame(Kategorie=factors$Triage[!is.na(factors$Triage)], Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  report.table(c,'triage.xml',align=c('left','right','right'),widths=c(20,15,15))
  
  graph <- barchart( a, horizontal=FALSE, xlab="Ersteinschätzung", ylab='Anzahl Patienten',col=std_cols5 ,origin=0)
  report.svg(graph, 'triage')
}, silent=FALSE)
