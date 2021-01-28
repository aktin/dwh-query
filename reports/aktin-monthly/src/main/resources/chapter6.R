#Entlassung & Co
try({
  discharge_table <- table(df$discharge,useNA = "always")[c(14,7,1,2,3,6,5,4,11,9,13,10,8,12)]
  names(discharge_table)[1]<-'Keine Daten'
  discharge_table<-data.frame(discharge_table)
  graph<-ggplot(data=discharge_table, aes(x=Var1, y=Freq)) +
    geom_bar(stat="identity", fill="#046C9A")+
    labs(y="Anzahl Patienten", x = "")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.y = element_text(face="bold", color="#000000", size=12),
          axis.text.x = element_text(face="bold", color="#000000", size=12))+
    #scale_y_continuous(expand = c(0, 0.3))+
    coord_flip()
  report.svg(graph, 'discharge')
}, silent=FALSE)
try({
  discharge_table <- table(df$discharge,useNA = "always")[c(12,8,10,13,9,11,4,5,6,3,2,1,7,14)] #reorder rows
  names(discharge_table)[length(names(discharge_table))]<-'Keine Daten'
  b <- data.frame(Kategorie=factors$Discharge[!is.na(factors$Discharge)], Anzahl=gformat(discharge_table), Anteil=gformat((discharge_table / sum(discharge_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(discharge_table)),Anteil=gformat(100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  report.table(c,name='discharge.xml',align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)