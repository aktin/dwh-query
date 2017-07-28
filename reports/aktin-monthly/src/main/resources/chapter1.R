#Patient Sex
try({
  a <- table(df$sex,useNA = "no") #NA values are not possible in Histream patient
  b <- data.frame(Kategorie=factors$Geschlecht[!is.na(factors$Geschlecht)], Anzahl=gformat(a), Anteil=gformat((a / sum(a))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(a)),Anteil=gformat(100,digits=1)))
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
  graph <- histogram(x,xlab="Alter [Jahre]",ylab="Anzahl Patienten",type='count',breaks=seq(0,110,length=12),sub=paste('n =',length(df$age),', Werte größer 110 werden als 110 gewertet'),col=std_cols1)
  report.svg(graph, 'age')
}, silent=FALSE)