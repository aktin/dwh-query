#TOP20 CEDIS
try({
  t <- table(df$cedis,useNA = "always") #frequencies
  y <- t
  #remove it and put it at the bottom - "TOP20+Unknown+NA"
  y[names(y)=='999'] <- 0
  y[is.na(names(y))] <- 0
  if( length(which(y > 0)) > 0 ){
    # at least one CDIS code available
    y <- sort(y, decreasing = TRUE)
    y <- y [1:20]
    y[y==0] <- NA #remove unused
    y[length(y)+1] <- t["999"]
    names(y)[length(y)] <- "999"
    y[length(y)+1] <- t[length(t)]
    names(y)[length(y)] <- "NA"
    graph <- barchart(rev(y), xlab="Anzahl Patienten",col=std_cols1,origin=0)
  }else{
    # no CEDIS codes at all
    y <- data.frame() 
    x <- data.frame(Var1=names(y),Freq=as.numeric(y))
    x <- rbind(x, data.frame(Var1='999',Freq=t['999'], row.names=NULL))
    x <- rbind(x, data.frame(Var1='NA',Freq=t[is.na(names(t))], row.names=NULL))
    x <- na.omit(x)
    graph <- barchart(data=x, Var1 ~ Freq, xlab="Anzahl Patienten",col=std_cols1,origin=0)
  }
  report.svg(graph, 'cedis_top')
  
  x <- data.frame(Var1=names(y),Freq=as.numeric(y))
  #x <- rbind(x, data.frame(Var1='999',Freq=t['999'], row.names=NULL))
  #x <- rbind(x, data.frame(Var1='NA',Freq=t[is.na(names(t))], row.names=NULL))
  x <- na.omit(x)
  
  y <- x
  y$label <- as.character(factor(y$Var1, levels=cedis[[1]], labels=cedis[[3]]))
  y$label[is.na(y$label)] <- "Vorstellungsgrund nicht dokumentiert"
  #kat <- paste(y$Var1,y$label, sep = ': ')
  #b <- data.frame(Kategorie=kat, Anzahl=gformat(y$Freq), Anteil=gformat((y$Freq / sum(t))*100,digits = 1))
  b <- data.frame(Code=y$Var1, Kategorie=y$label, Anzahl=gformat(y$Freq), Anteil=gformat((y$Freq / sum(t))*100,digits = 1))
  #b <- rbind(b, data.frame(Kategorie="Vorstellungsgrund nicht dokumentiert",Anzahl=gformat(sum(is.na(df$cedis))), Anteil=gformat((sum(is.na(df$cedis)) / length(df$cedis))*100,digits = 1)))
  c <- rbind(b, data.frame(Code='',Kategorie="Summe",Anzahl=gformat(sum(y$Freq)),Anteil=gformat(sum(y$Freq) / length(df$cedis)*100,digits=1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,4] <- paste(c[,4],'%')
  report.table(c,name='cedis.xml',align=c('left','left','right','right'),widths=c(8,60,15,15))
}, silent=FALSE)

#CEDIS Groups
#This causes warnings (duplicate levels in factors, which is ok here), but it works :)
# xxx ToDo: what if there are lots of NAs?
try({
  cedis_cat_top <- factor(x=enc$cedis,t(cedis[1]),labels=t(cedis[2])) #map Categories
  #cedis_cat_top <- factor(x=cedis_cat_top,t(cedis[1]),labels=t(cedis[3]))   #map Labels
  #cedis_cat_top <- droplevels(cedis_cat_top) #not ideal, only showing used categories
  #cedis_cat_top <- sort(cedis_cat_top, decreasing = FALSE)
  x <- factor(cedis_cat_top)
  levels(x) <- list("Kardiovaskulär"="CV","HNO (Ohren)"="HNE","HNO (Mund, Rachen, Hals)"="HNM","HNO (Nase)"="HNN","Umweltbedingt"="EV","Gastrointestinal"="GI","Urogenital"="GU","Psychische Verfassung"="MH","Neurologisch"="NC","Geburtshilfe/Gynäkologie"="GY","Augenheilkunde"="EC","Orthopädisch/Unfall-chirurgisch"="OC","Respiratorisch"="RC","Haut"="SK","Substanzmissbrauch"="SA","Allgemeine und sonstige Beschwerden"="MC","Patient vor Ersteinschätzung wieder gegangen"="998","Unbekannt"="999")
  x <- table(x,useNA = 'always')
  names(x)[length(x)] <- "Vorstellungsgrund nicht dokumentiert"
  graph <- barchart(rev(x),xlab="Anzahl Patienten",col=std_cols1,origin=0)
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
  if (! is.null(codes)) {
    codes <-  codes[complete.cases(codes)]
  }
  #kat <- paste(codes,": ",names(a),sep = '')
  #b <- data.frame(Kategorie=kat, Anzahl=gformat(a), Anteil=gformat((a / sum(t))*100,digits = 1))
  b <- data.frame(Code=codes,Kategorie=names(a), Anzahl=gformat(a), Anteil=gformat((a / sum(t))*100,digits = 1))
  b <- rbind(b, data.frame(Code='',Kategorie="Nicht dokumentiert",Anzahl=gformat(length(df$encounter)-length(f_diag)), Anteil=gformat(((length(df$encounter)-length(f_diag)) / length(f_diag))*100,digits = 1)))  #f_diag is 1 or 0 per encounter, df$enc is the number of enc. Counting NA is not enough since there may be multiple or no diagnoses per encounter
  ges <- sum(a)+(length(df$encounter)-length(f_diag))
  c <- rbind(b, data.frame(Code='',Kategorie="Summe",Anzahl=gformat(ges),Anteil=gformat((ges / length(f_diag)*100),digits = 1)))
  #c[,3] <- sprintf(fmt="%.1f",c[,3])
  c[,4] <- paste(c[,4],'%')
  report.table(c,name='icd.xml',align=c('left','left','right','right'),widths=c(8,62,15,15))
  
}, silent=FALSE)