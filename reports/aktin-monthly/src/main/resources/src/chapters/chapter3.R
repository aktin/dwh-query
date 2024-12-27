#Transport and referral
try({
  table_formatted <- table(df$transport,useNA = "always")
  names(table_formatted)[length(names(table_formatted))]<-'Keine Daten'
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=format_number(a), Anteil=format_number((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=format_number(sum(a)),Anteil=format_number(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  report_table(c,name='transport.xml',align=c('left','right','right'),widths=c(25,15,15))
  
  table_formatted <- table(df$referral,useNA = "always")
  names(table_formatted)[length(names(table_formatted))]<-'Keine Daten'
  a <- table_formatted
  b <- data.frame(Kategorie=names(a), Anzahl=format_number(a), Anteil=format_number((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=format_number(sum(a)),Anteil=format_number(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  report_table(c,name='referral.xml',align=c('left','right','right'),widths=c(60,15,15))
}, silent=FALSE)