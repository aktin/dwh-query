try({
  table_formatted <- table(df$triage.result,useNA = "ifany") #NA is already mapped
  a <- table_formatted
  b <- data.frame(Kategorie=factors$Triage[!is.na(factors$Triage)], Anzahl=format_number(a), Anteil=format_number((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=format_number(sum(a)),Anteil=format_number(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  report_table(c,'triage.xml',align=c('left','right','right'),widths=c(20,15,15))
  

  a<-data.frame(a)
  graph<-ggplot(data=a, aes(x=Var1, y=Freq,fill=Var1)) +
    geom_bar(stat="identity",position = "dodge", colour = "black",show.legend = FALSE)+
    labs(x="Ersteinschätzung", y = "Anzahl Patienten")+
    scale_fill_manual(values = c("red", "orange", "yellow2", "green4", "blue", "grey48"))+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_y_continuous(expand = c(0, 0.01))
  report_svg(graph, 'triage')
}, silent=FALSE)
