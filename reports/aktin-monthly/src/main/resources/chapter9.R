#Isolierte Patienten
try({
  df$combined_iso <- factor(enc$isolation_grund) #Code ISO is redundant with Reason; CAVE: if Reverse and Reason are given, only Reverse is used
  levels(df$combined_iso) <- list("Keine Isolation"="ISO:NEG","Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Umkehrisolation"="RISO","Andere"="OTH")
  df$combined_iso[df$isolation == 'Keine Isolation'] <- 'Keine Isolation'
  df$combined_iso[df$isolation == 'Umkehrisolation'] <- 'Umkehrisolation'
  
  isoreason_table <- table(df$combined_iso,useNA = "always")
  names(isoreason_table)[length(names(isoreason_table))]<-'Keine Daten'
  b <- data.frame(Kategorie=names(isoreason_table), Anzahl=gformat(isoreason_table), Anteil=gformat((isoreason_table / sum(isoreason_table))*100,digits = 1))
  c <- rbind(b, data.frame(Kategorie="Summe",Anzahl=gformat(sum(isoreason_table)),Anteil=gformat(100,digits=1)))
  c[,3] <- paste(c[,3],'%')
  report.table(c,name='isoreason.xml',align=c('left','right','right'),widths=c(30,15,15))
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
  report.table(mrsa_table,name='multiresistant.xml',align=c('left','right','right'),widths=c(30,15,15))
}, silent=FALSE)
