#Isolierte Patienten
try({
  df$combined_iso <- factor(enc$isolation_grund) #Code ISO is redundant with Reason; CAVE: if Reverse and Reason are given, only Reverse is used
  levels(df$combined_iso) <- list("Keine Isolation"="ISO:NEG","Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Umkehrisolation"="RISO","Andere"="OTH")
  df$combined_iso[df$isolation == 'Keine Isolation'] <- 'Keine Isolation'
  df$combined_iso[df$isolation == 'Umkehrisolation'] <- 'Umkehrisolation'
  
  isoreason_table <- table(df$combined_iso,useNA = "always")
  names(isoreason_table)[length(names(isoreason_table))]<-'Keine Daten'
  b <- data.frame(Kategorie=names(isoreason_table), Anzahl=format_number(isoreason_table), Anteil=format_number((isoreason_table / sum(isoreason_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=format_number(sum(isoreason_table)),Anteil=format_number(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  report_table(c,name='isoreason.xml',align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)

#Multiresistente Erreger
try({
  mrsa_patient <- cbind(df$keime,df$keime_mrsa,df$keime_3mrgn,df$keime_4mrgn,df$keime_vre)
  mrsa_patient <- rowSums(mrsa_patient,na.rm=TRUE)
  mrsa_table <-                   data.frame(Kategorie="MRSA", Bekannt=sum(df$keime_mrsa == 'MRSA',na.rm=TRUE), Verdacht=sum(df$keime_mrsa == 'MRSA:SUSP',na.rm=TRUE))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="3-MRGN",Bekannt=sum(df$keime_3mrgn == '3MRGN',na.rm=TRUE),Verdacht=sum(df$keime_3mrgn == '3MRGN:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="4-MRGN",Bekannt=sum(df$keime_4mrgn == '4MRGN',na.rm=TRUE),Verdacht=sum(df$keime_4mrgn == '4MRGN:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="VRE",Bekannt=sum(df$keime_vre == 'VRE',na.rm=TRUE),Verdacht=sum(df$keime_vre == 'VRE:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Andere",Bekannt=sum(df$keime_andere == 'OTH',na.rm=TRUE),Verdacht=sum(df$keime_andere == 'OTH:SUSP',na.rm=TRUE)))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Keine Keime",Bekannt=sum(df$keime == 'AMRO:NEG',na.rm=TRUE),Verdacht='-'))
  mrsa_table <- rbind(mrsa_table, data.frame(Kategorie="Keine Angabe",Bekannt=sum(mrsa_patient<1),Verdacht='-'))
  report_table(mrsa_table,name='multiresistant.xml',align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)

#Patienten in der NA
#Überfüllung
try({
  a<-data.frame(as.POSIXct(df$admit.ts),as.POSIXct(df$discharge.ts))
  colnames(a)<-c("admit.ts","discharge.ts")
  a<-subset(a,!(is.na(admit.ts) | is.na(discharge.ts)))
  a <- a[(order(as.Date(a$admit.ts))),]
  a$in_na <- sapply(a$admit.ts, function(enter){sum(a$admit.ts <= enter & a$discharge.ts > enter)})
  init_last_month<-a[1,1]
  init_last_month<-as.Date(init_last_month)
  end_last_month<-length(a$admit.ts)
  end_last_month<-a[end_last_month,1]
  end_last_month<-as.Date(end_last_month)
  #a$Uhrzeit<-substr(a$admit.ts,12,16)
  a$Uhrzeit<- strptime(a$admit.ts, "%Y-%m-%d %H:%M:%S")
  a$Uhrzeit <- as.numeric(format(a$Uhrzeit, "%H")) +as.numeric(format(a$Uhrzeit, "%M"))/60
  a$Uhrzeit<-a$Uhrzeit*3600
  # a$Uhrzeit<-as.numeric(lubridate::hm(a$Uhrzeit))
  a$tag<-as.Date(a$admit.ts)
  #a$tag_g<-weekdays(a$admit.ts)
  a$tag_g<-format(a$admit.ts, format = "%a")
  #a$year<-lubridate::year(a$admit.ts)
  a$year<-as.numeric(format(a$admit.ts,'%Y'))
  #a$day<-lubridate::day(a$admit.ts)
  a$day<-as.numeric(format(a$admit.ts,'%d'))
  a$woche<-ifelse(a$tag<=init_last_month+6,1,
                  ifelse(a$tag<=init_last_month+13,2,
                         ifelse(a$tag<=init_last_month+20,3,
                                ifelse(a$tag<=init_last_month+27,4,
                                       ifelse(a$tag>=init_last_month+28,5,6)))))
  
  

  #x-Achse Bezeichung
  time_labels<-c("00:00","02:00","04:00","06:00","08:00","10:00","12:00","14:00","16:00","18:00","20:00","22:00","24:00")
  
  #fehlende 0 und 24 Uhr Zeiten zur Vollständigen Linie
  fehlend<-data.frame(tag=as.Date(init_last_month,origin = "1970-01-01"):as.Date(end_last_month,origin = "1970-01-01"),Uhrzeit=0)
  fehlend$tag<-as.Date(fehlend$tag,origin = "1970-01-01")
  a<-full_join(a,fehlend)
  a <- `row.names<-`(a[with(a,order(tag,Uhrzeit)),], NULL)
  a<-fill(a, tag_g,day,woche, .direction = 'up')
  
  fehlend<-data.frame(tag=as.Date(init_last_month,origin = "1970-01-01"):as.Date(end_last_month,origin = "1970-01-01"),Uhrzeit=86400)
  fehlend$tag<-as.Date(fehlend$tag,origin = "1970-01-01")
  a<-full_join(a,fehlend)
  
  a <- `row.names<-`(a[with(a,order(tag,Uhrzeit)),], NULL)
  a<-fill(a, in_na,tag_g,day,woche, .direction = 'down')
  a <- a[-nrow(a),]
  a$tag<-format(a$tag, format="%m-%d")
  
  
  #title Diagramme
  woche1<-paste('Woche 1',init_last_month,'-',init_last_month+6)
  woche2<-paste('Woche 2',init_last_month+7,'-',init_last_month+13)
  woche3<-paste('Woche 3',init_last_month+14,'-',init_last_month+20)
  woche4<-paste('Woche 4',init_last_month+21,'-',init_last_month+27)
  woche5<-paste('Woche 5',init_last_month+28,'-',end_last_month)
  woche5<-paste('Woche 5',init_last_month+28,'-',init_last_month+31)
  
  a_woche1<-a%>%filter(woche==1)
  a_woche1<-subset(a_woche1,!(is.na(in_na)))
  graph<- ggplot(data=a_woche1, aes(x=Uhrzeit, y=in_na, fill=tag,group=1))+ 
    geom_line()+
    facet_grid(tag+tag_g~.)+
    theme_bw()+
    xlab("Uhrzeit")+
    ylab("Anzahl Patienten")+
    scale_x_continuous(breaks = seq(0,86400, by=7200),expand = c(0,0),labels = time_labels)+
    coord_cartesian(xlim=c(0,86400))+
    ggtitle(woche1)
  report_svg(graph, 'stayone')
  
  a_woche2<-a%>%filter(woche==2)
  graph2<- ggplot(data=a_woche2, aes(x=Uhrzeit, y=in_na, fill=tag,group=1))+ 
    geom_line()+
    facet_grid(tag+tag_g~.)+
    theme_bw()+
    xlab("Uhrzeit")+
    ylab("Anzahl Patienten")+
    scale_x_continuous(breaks = seq(0,86400, by=7200),expand = c(0,0),labels = time_labels)+
    coord_cartesian(xlim=c(0,86400))+
    ggtitle(woche2)
  report_svg(graph2, 'staytwo')
  
  a_woche3<-a%>%filter(woche==3)
  graph3<- ggplot(data=a_woche3, aes(x=Uhrzeit, y=in_na, fill=tag,group=1))+ 
    geom_line()+
    facet_grid(tag+tag_g~.)+
    theme_bw()+
    xlab("Uhrzeit")+
    ylab("Anzahl Patienten")+
    scale_x_continuous(breaks = seq(0,86400, by=7200),expand = c(0,0),labels = time_labels)+
    coord_cartesian(xlim=c(0,86400))+
    ggtitle(woche3)
  report_svg(graph3, 'staythree')
  
  a_woche4<-a%>%filter(woche==4)
  graph4<- ggplot(data=a_woche4, aes(x=Uhrzeit, y=in_na, fill=tag,group=1))+ 
    geom_line()+
    facet_grid(tag+tag_g~.)+
    theme_bw()+
    xlab("Uhrzeit")+
    ylab("Anzahl Patienten")+
    scale_x_continuous(breaks = seq(0,86400, by=7200),expand = c(0,0),labels = time_labels)+
    coord_cartesian(xlim=c(0,86400))+
    ggtitle(woche4)
  report_svg(graph4, 'stayfour')
  
  a_woche5<-a%>%filter(woche==5 & admit.ts<init_last_month+31)
  graph5<- ggplot(data=a_woche5, aes(x=Uhrzeit, y=in_na, fill=tag,group=1))+ 
    geom_line()+
    facet_grid(tag+tag_g~.)+
    theme_bw()+
    xlab("Uhrzeit")+
    ylab("Anzahl Patienten")+
    scale_x_continuous(breaks = seq(0,86400, by=7200),expand = c(0,0),labels = time_labels)+
    coord_cartesian(xlim=c(0,86400))+
    ggtitle(woche5)
  report_svg(graph5, 'stayfive')
}, silent=FALSE)
