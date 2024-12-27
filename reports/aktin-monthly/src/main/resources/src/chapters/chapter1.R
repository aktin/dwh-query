#Patient Sex
try({
  a <- data.frame(table(df$sex,useNA = "no")) #NA values are not possible in Histream patient
  b<-a
  b$Anteil<-(b$Freq/sum(b$Freq))*100
  colnames(b)<-c("Kategorie","Anzahl","Anteil")
  summe<-sum(b$Anzahl)
  b$Anzahl<-gformat(b$Anzahl)
  b$Anteil<-gformat(b$Anteil,digits=1)
  #b <- data.frame(Kategorie=factors$Geschlecht[!is.na(factors$Geschlecht)], Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(summe),Anteil=gformat(100,digits=1)))
  ##c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,3] <- paste(c[,3],'%')
  report.table(c,name='sex.xml',align=c('left','right','right'),widths=c(25,15,15))
}, silent=FALSE)

#Patient Age
try({
  Kennzahl <- c('Mittelwert','Median','Standardabweichung','Minimum','Maximum')
  Alter <- c(round(mean(na.omit(df$age)),0),round(median(na.omit(df$age)),0),round(stdabw(na.omit(df$age)),0),min(na.omit(df$age)),max(na.omit(df$age)))
  #Alter <- sprintf(fmt="%.1f",Alter)
  Alter <- paste(Alter,'Jahre')
  b <- data.frame(Kennzahl,Alter)
  report.table(b,name='age.xml',align=c('left','right'),widths=c(30,15))
}, silent=FALSE)
try({
  x<-df$age
  x[x>110] <- 110
  x[x<0] <- NA
  x<-x[!is.na(x)]
  x<-data.frame(x)
  graph<-ggplot(x, aes(x=x)) + 
    geom_histogram(color="black", fill="#046C9A",binwidth=5)+
    labs(x="Alter [Jahre]", y = "Anzahl Patienten",caption = paste('n =',length(df$age),', Werte größer 110 werden als 110 gewertet'))+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_x_continuous(expand = c(0, 3),breaks = seq(0, 100, 5)) +
    scale_y_continuous(expand = c(0, 0.3))+
    geom_vline(aes(xintercept=mean(x)),
               color="#e3000b", linetype="dashed", size=1)
    #geom_text(aes(x=mean(x), label="Mittelwert\n", y=50), colour="white", angle=90,size=4)
    report.svg(graph, 'age')
}, silent=FALSE)