#TOP20 CEDIS
try({
  t <- table(df$cedis,useNA = "always") #frequencies
  y<-data.frame(df$cedis)
  y[is.na(y)] <- "999"
  #y<-y%>%drop_na()
  y<-y%>%filter(df.cedis !="999")
  y<-y%>%count(df.cedis)%>%top_n(20)
  x<-data.frame(df$cedis)
  x[is.na(x)] <- "999"
  x<-x%>%count(df.cedis)
  x<-x%>%filter(df.cedis=="999")
  if( length(which(y$n > 0)) > 0 ){ #needs to be re-written to also work correctly if there are less than 20 used codes
    # at least one CEDIS code available
    y$n[y$n==0] <- NA #remove unused
    y<-rbind(y,x)
    graph<-ggplot(data=y, aes(reorder(df.cedis,n),n)) +
      geom_bar(stat="identity", fill="#046C9A",width = 0.5)+
      labs(y = "Anzahl Patienten",x="CEDIS")+
      theme(plot.caption = element_text(hjust=0.5,size=12),
            panel.background = element_rect(fill = "white"),
            axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
            axis.text.x = element_text(face="bold", color="#000000", size=12),
            axis.text.y = element_text(face="bold", color="#000000", size=12))+
      scale_y_continuous(expand = c(0, 0.3),breaks=seq(0,max(y$n),30))+
      coord_flip()
    report.svg(graph, 'cedis_top')
    
    y<-y%>%filter(df.cedis !="999")
    y <- arrange(y, desc(n)) %>%
      mutate(rank = 1:nrow(y))
    y<-y[1:20,]
    y$label <- as.character(factor(y$df.cedis, levels=cedis[[1]], labels=cedis[[3]]))
    x$label <- as.character(factor(x$df.cedis, levels=cedis[[1]], labels=cedis[[3]]))
    #x$label[is.na(x$label)] <- "Vorstellungsgrund nicht dokumentiert"
    b <- data.frame(Code=y$df.cedis[1:20], Kategorie=y$label[1:20], Anzahl=gformat(y$n[1:20]), Anteil=gformat((y$n[1:20] / length(df$encounter))*100,digits = 1))
    c <- rbind(b, data.frame(Code='---',Kategorie="Summe TOP20",Anzahl=gformat(sum(y$n[1:20])),Anteil=gformat(sum(y$n[1:20]) / length(df$encounter)*100,digits=1)))
    d <- rbind(c,data.frame(Code=x$df.cedis, Kategorie=x$label, Anzahl=gformat(x$n), Anteil=gformat((x$n / length(df$encounter))*100,digits = 1)))
    d[,4] <- paste(d[,4],'%')
    report.table(d,name='cedis.xml',align=c('left','left','right','right'),widths=c(8,60,15,15))
    
  }else{ # no CEDIS codes at all
    y <- data.frame() 
    x <- data.frame(Var1=names(y),Freq=as.numeric(y))
    x <- rbind(x, data.frame(Var1='999',Freq=t['999'], row.names=NULL))
    x <- rbind(x, data.frame(Var1='NA',Freq=t[is.na(names(t))], row.names=NULL))
    x <- na.omit(x)
    graph<-ggplot(data=x, aes(reorder(Var1,Freq),Freq)) +
      geom_bar(stat="identity", fill="#046C9A")+
      labs(y = "Anzahl Patienten",x="CEDIS")+
      theme(plot.caption = element_text(hjust=0.5,size=12),
            panel.background = element_rect(fill = "white"),
            axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
            axis.text.x = element_text(face="bold", color="#000000", size=12),
            axis.text.y = element_text(face="bold", color="#000000", size=12))+
      scale_y_continuous(expand = c(0, 0.3),breaks=seq(0,max(x$Freq),30))+
      coord_flip()
    report.svg(graph, 'cedis_top')
    
    c <- data.frame(Code='---',Kategorie="Summe TOP20",Anzahl=gformat(0),Anteil=gformat(0,digits=1))
    d <- rbind(c,data.frame(Code='', Kategorie="Vorstellungsgrund nicht dokumentiert", Anzahl=gformat(length(df$encounter)), Anteil=gformat(( 1 )*100,digits = 1)))
    d[,4] <- paste(d[,4],'%')
    report.table(d,name='cedis.xml',align=c('left','left','right','right'),widths=c(8,60,15,15))
    }
}, silent=FALSE)

#CEDIS Groups
#This causes warnings (duplicate levels in factors, which is ok here), but it works :)
# xxx ToDo: what if there are lots of NAs?
try({
  cedis_cat_top <- factor(x=enc$cedis,t(cedis[1]),labels=t(cedis[2])) #map Categories
  x <- factor(cedis_cat_top)
  levels(x) <- list("Kardiovaskulär"="CV","HNO (Ohren)"="HNE","HNO (Mund, Rachen, Hals)"="HNM","HNO (Nase)"="HNN","Umweltbedingt"="EV","Gastrointestinal"="GI","Urogenital"="GU","Psychische Verfassung"="MH","Neurologisch"="NC","Geburtshilfe/Gynäkologie"="GY","Augenheilkunde"="EC","Orthopädisch/Unfall-chirurgisch"="OC","Respiratorisch"="RC","Haut"="SK","Substanzmissbrauch"="SA","Allgemeine und sonstige Beschwerden"="MC","Patient vor Ersteinschätzung wieder gegangen"="998","Unbekannt"="999")
  x <- table(x,useNA = 'always')
  names(x)[length(x)] <- "Vorstellungsgrund nicht dokumentiert"
  x<-data.frame(x)
  graph<-ggplot(data=x, aes(reorder(Var1,Freq),Freq)) +
    geom_bar(stat="identity", fill="#046C9A",width=0.5)+
    labs(y = "Anzahl Patienten",x="")+
    theme(plot.caption = element_text(hjust=0.5,size=12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size=12),panel.border = element_blank(),axis.line = element_line(color = 'black'),
          axis.text.x = element_text(face="bold", color="#000000", size=12),
          axis.text.y = element_text(face="bold", color="#000000", size=10))+
    scale_y_continuous(expand = c(0, 0.3),breaks=seq(0,max(x$Freq),50))+
    coord_flip()
  report.svg(graph, 'cedis_groups')
}, silent=FALSE)

#TOP20 ICD
try({
  ## Old simple Version (still necessary, table is based only on "F" data)
  f_diag <- df_diag$diagnosis[df_diag$fuehrend=='F' & !is.na(df_diag$fuehrend)] 
  t <- table(f_diag,useNA = "no") #frequencies
  x <- sort(t, decreasing = TRUE)
  names(x)[is.na(names(x))] <- 'NA'
  
  #calculate table for stacked barchart based on Zusatzkennzeichen for all diagnoses with "F"
  icd_stacked <- data.frame(mod_F=df_diag$diagnosis,mod_G=df_diag$diagnosis,mod_V=df_diag$diagnosis,mod_Z=df_diag$diagnosis,mod_A=df_diag$diagnosis)
  #remove all diagnoses that are not 'F'
  icd_stacked$mod_F[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_G[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_V[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_Z[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  icd_stacked$mod_A[!df_diag$fuehrend=='F' | is.na(df_diag$fuehrend)] <- NA
  #remove all diagnoses that have the wrong modifier
  icd_stacked$mod_G[(!df_diag$zusatz=='G') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_V[(!df_diag$zusatz=='V') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_Z[(!df_diag$zusatz=='Z') | is.na(df_diag$zusatz)] <- NA
  icd_stacked$mod_A[(!df_diag$zusatz=='A') | is.na(df_diag$zusatz)] <- NA
  #remove all diagnoses from 'mod_F' that have a modifier
  icd_stacked$mod_F[df_diag$zusatz =='G'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='V'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='Z'] <- NA
  icd_stacked$mod_F[df_diag$zusatz=='A'] <- NA
  #lots of silly transformations to get a matrix that is plotable as a stacked barchart
  stacktable <- data.frame(diag=names(table(icd_stacked$mod_F)),F=as.vector(table(icd_stacked$mod_F)),G=as.vector(table(icd_stacked$mod_G)),V=as.vector(table(icd_stacked$mod_V)),Z=as.vector(table(icd_stacked$mod_Z)),A=as.vector(table(icd_stacked$mod_A)))
  diag_order <- order(table(f_diag,useNA = "always"),decreasing = TRUE)
  stacktable <- t(stacktable[diag_order[1:20],])
  colnames(stacktable) <- stacktable[1,]
  if (! is.null(names(table(icd_stacked$mod_F)))) {
    stacktable <- stacktable[2:6,]
  }
  stacktable[is.na(stacktable)] <- 0
  stacktable <- apply(stacktable,2,as.numeric)
  rownames(stacktable) <- c("F","G","V","Z","A")
  stacktable <- t(stacktable)
  stacktable <- stacktable[complete.cases(stacktable),] #remove rows
  graph <- barchart(stacktable[dim(stacktable)[1]:1,1:5],xlab="Anzahl Patienten",sub="blau=Ohne Zusatzkennzeichen, grün=Gesichert, gelb=Verdacht, orange=Z.n., rot=Ausschluss",col=std_cols5[5:1],origin=0)
  #graph <- barchart( x [20:1], xlab="Anzahl Patienten",col=std_cols[1],origin=0)
  report.svg(graph, 'icd_top') 
  
  a <- t
  a <- sort(a, decreasing = TRUE)
  a <- a [1:20]
  codes <- names(a)
  #names(a) <- factor(names(a),t(icd[1]),labels=strtrim(t(icd[2]),60))
  names(a) <- factor(names(a),t(icd[1]),labels=t(icd[2]),61)
  a <- a[complete.cases(a)]
  if (length(a)==0) { 
    codes <-  a  #do not output ICD-codes with no occurence in the table => make the table shorter
  }
  #kat <- paste(codes,": ",names(a),sep = '')
  #b <- data.frame(Kategorie=kat, Anzahl=gformat(a), Anteil=gformat((a / sum(t))*100,digits = 1))
  b <- data.frame(Code=codes,Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / length(df$encounter))*100,digits = 1))
  ges <- sum(a)
  c <- rbind(b, data.frame(Code='---',Kategorie="Summe TOP20",Anzahl=gformat(ges),Anteil=gformat((ges / length(df$encounter)*100),digits = 1)))
  d <- rbind(c, data.frame(Code='',Kategorie="Nicht dokumentiert",Anzahl=gformat(length(df$encounter)-length(f_diag)), Anteil=gformat(((length(df$encounter)-length(f_diag)) / length(df$encounter))*100,digits = 1)))  #f_diag is 1 or 0 per encounter, df$enc is the number of enc. Counting NA is not enough since there may be multiple or no diagnoses per encounter
  d[,4] <- paste(d[,4],'%')
  report.table(d,name='icd.xml',align=c('left','left','right','right'),widths=c(8,62,15,15))
  
}, silent=FALSE)