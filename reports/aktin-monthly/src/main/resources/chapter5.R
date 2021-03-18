
try({
  df$phys.d<-as.numeric(df$phys.d)
  a <- df$phys.d[df$phys.d<181]
  isNA <- length(df$phys.d[is.na(df$phys.d )])
  b <- a[!is.na(a)]
  c<-table(a<0)
  c<-data.frame(c)
  c<-c%>%filter(Var1==TRUE)
  c<-c$Freq
  c<-ifelse(is.integer(c),"0",c)
  positiveoutofbounds <- length(df$phys.d) - length(a)
  negativeoutofbounds <- c
   b<-data.frame(b)
  b<-data.frame(b)
  b$b<-as.numeric(b$b)
  b<-b%>%filter(b>-1 & b<181)
  b$x<-"Zeit"
  graph<-ggplot(data=b,aes(x=x,y=b))+
    geom_boxplot(fill="#046C9A",width=0.5)+
    #geom_jitter(width = 0.05,alpha=0.2)+
    labs(y = "Zeit von Aufnahme bis Arztkontakt [Minuten]",
         caption = paste("Fehlende Werte: ", isNA, "; Werte > 180 Minuten: ", positiveoutofbounds,"; Werte < 0 Minuten: ", negativeoutofbounds))+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.y = element_text(face="bold", color="#000000", size=12),
          axis.title.x = element_blank(),
          axis.ticks.x=element_blank(),
          axis.text.x=element_blank())+
    scale_y_continuous(breaks=seq(0,200,20))
  graph2<- ggplot(b, aes(x = b)) +  
    geom_histogram(aes(y = 100*(..count..)/sum(..count..)),bins = 12,color="black", fill="#046C9A",boundary=0)+
    scale_x_continuous(breaks=seq(0,180,length=7))+
    labs(y = "Relative Häufigkeit [%]")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.y = element_text(face="bold", color="#000000", size=12),
          axis.title.x = element_blank(),
          #axis.ticks.x=element_blank(),
          #axis.text.x=element_blank(),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(color="#e3000b",size=12,face="bold"))
    report.svg(graph, 'phys.d.box')
    report.svg(graph2, 'phys.d.hist')
}, silent=FALSE)

try({
  used <- length(b$b)
  Kennzahl <- factors$phys_txt[!is.na(factors$phys_txt)]
  Zeit <- c(round(mean(b$b),1),median(b$b),round(stdabw(b$b),1),min(b$b),max(b$b))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,positiveoutofbounds,negativeoutofbounds,Zeit)
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,name='phys.d.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to triage
try({
  df$triage.d<-as.numeric(df$triage.d)
  a <- df$triage.d[df$triage.d<61]
  isNA <- length(df$triage.d[is.na(df$triage.d)])
  b <- a[!is.na(a)]
  c<-table(a<0)
  c<-data.frame(c)
  c<-c%>%filter(Var1==TRUE)
  c<-c$Freq
  c<-ifelse(is.integer(c),"0",c)
  positiveoutofbounds <- length(df$triage.d) - length(a)
  negativeoutofbounds <- c
  b<-data.frame(b)
  b$b<-as.numeric(b$b)
  b<-b%>%filter(b>-1 & b<61)
  b$x<-"Zeit"
  z<-b%>%filter(b<11)
  z<-length(z$b)
  
  graph<-ggplot(data=b,aes(x=x,y=b))+
    geom_boxplot(fill="#046C9A",width=0.5)+
    #geom_jitter(width = 0.05,alpha=0.2)+
    geom_hline(aes(yintercept = 10, linetype = "Ersteinschätzung innerhalb 10 Minuten"), color = "red", size = 1)+ 
    labs(y = "Zeit von Aufnahme bis Triage [Minuten]",
         caption = paste("Fehlende Werte: ", isNA, "; Werte > 60 Minuten: ", positiveoutofbounds,"; Werte < 0 Minuten: ", negativeoutofbounds,"; Werte innerhalb 10 min: ",z))+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.y = element_text(face="bold", color="#000000", size=12),
          axis.title.x = element_blank(),
          axis.ticks.x=element_blank(),
          axis.text.x=element_blank(),
          legend.position = "bottom",legend.title = element_blank())+
    coord_cartesian(ylim = c(0, 60))
    #scale_y_continuous(breaks=seq(0,max(b$b),4))
  graph2<- ggplot(b, aes(x = b)) +  
    geom_histogram(aes(y = 100*(..count..)/sum(..count..)),bins = 12,color="black", fill="#046C9A",boundary=0)+
    scale_x_continuous(breaks=seq(0,60,length=7))+
    labs(y = "Relative Häufigkeit [%]")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.y = element_text(face="bold", color="#000000", size=12),
          axis.title.x = element_blank(),
          #axis.ticks.x=element_blank(),
          #axis.text.x=element_blank(),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(color="#e3000b",size=12,face="bold"))
  
  report.svg(graph, 'triage.d.box')
  report.svg(graph2, 'triage.d.hist')
}, silent=FALSE)

try({
  used <- length(b$b)
  Kennzahl <- factors$triage_txt[!is.na(factors$triage_txt)]
  Zeit <- c(round(mean(b$b),1),median(b$b),round(stdabw(b$b),1),min(b$b),max(b$b))
  Zeit <- sprintf(fmt="%.0f",Zeit)
  Zeit <- paste(Zeit, 'Min')
  Zeit <- c(used,isNA,positiveoutofbounds,negativeoutofbounds,Zeit)
  
  b <- data.frame(Kennzahl,Zeit)
  report.table(b,name='triage.d.xml',align=c('left','right'),widths=c(45,15))
}, silent=FALSE)

# Time to physician mean grouped by triage result
try({
  df$phys.d<-as.numeric(df$phys.d)
  a <- df$phys.d[df$phys.d<181]
  c<-table(a==0)
  c<-data.frame(c)
  c<-c%>%filter(Var1==TRUE)
  c<-c$Freq
  outofbounds <- length(df$phys.d) - length(a)
  b <- df$triage.result[df$phys.d<181]
  y <- data.frame(time=a, triage=b)
  y$time<-as.numeric(y$time)
  y<-y%>%filter(!is.na(time))
  graph<-ggplot(data=y, aes(x=triage, y=time,fill=triage)) +
    geom_boxplot(show.legend = FALSE)+
    labs(x="Triage-Gruppe", y = "Durchschn. Zeit bis Arztkontakt [Min.]",caption =paste("Werte über 180 Minuten (unberücksichtigt): ", outofbounds," ; Fehlender Zeitstempel Arztkontakt:",sum(is.na(df$phys.d))) )+
    scale_fill_manual(values = c("Rot"="red","Orange"= "orange", "Gelb"="yellow2", "Grün"="green4","Blau"= "blue","Ohne"= "grey48"))+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=12))+
    scale_y_continuous(breaks=seq(0,max(y$time),10),expand =c (0,0.3))
  report.svg(graph, 'triage.phys.d.avg')
}, silent=FALSE)


try({
  y2 <- data.frame(triage=df$triage.result, time=df$phys.d)
  y2<-y2%>%dplyr::filter(time<181)
  agg.funs <- list(Mittelwert=mean, Median=median, Minimum=min, Maximum=max)
  agg.list <- lapply(agg.funs, function(fun){
    with(y2, tapply(time, list(triage), FUN=fun, na.rm=TRUE))})
  
  agg.list$Mittelwert <- round(agg.list$Mittelwert,1)
  agg.list$Median <- round(agg.list$Median,0)
  agg.list$Anzahl <- with(y2, tapply(time, list(triage), FUN=length))
  agg.list$Anzahl[is.na(agg.list$Anzahl)] <- 0
  kat<-c("Rot","Orange","Gelb","Grün","Blau","Ohne")
 
  x <- data.frame(Kategorie=kat,agg.list)
  rm(agg.funs, agg.list)
  report.table(x,'triage.phys.d.xml',align=c('left','right','right','right','right','right'),width=15)
}, silent=FALSE)