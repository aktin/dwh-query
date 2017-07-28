# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(merge(enc,pat,by="patient_id")$geburtsdatum_ts,tz="GMT",format="%F")
#df$triage.ts = strptime(enc$triage_ts,tz="GMT",format="%F %H:%M")
#df$admit.ts = strptime(enc$aufnahme_ts,tz="GMT",format="%F %H:%M")
#df$phys.ts = strptime(enc$arztkontakt_ts,tz="GMT",format="%F %H:%M")
#df$therapy.ts = strptime(enc$therapiebeginn_ts,tz="GMT",format="%F %H:%M")
#df$discharge.ts = strptime(enc$entlassung_ts,tz="GMT",format="%F %H:%M")
df$triage.ts = strptime(enc$triage_ts,tz="GMT",format="%FT%H:%M")
#Precision in CDA: day+more -- conversion is only successful if source has precision minute+more! NA-values are handled later
df$admit.ts = strptime(enc$aufnahme_ts,tz="GMT",format="%FT%H:%M")
df$admit.day = strptime(enc$aufnahme_ts,tz="GMT",format="%F")
df$phys.ts = strptime(enc$arztkontakt_ts,tz="GMT",format="%FT%H:%M")
df$therapy.ts = strptime(enc$therapiebeginn_ts,tz="GMT",format="%FT%H:%M")
#Precision in CDA: day+more -- conversion is only successful if source has precision minute+more! NA-values are handled later
df$discharge.ts = strptime(enc$entlassung_ts,tz="GMT",format="%FT%H:%M")

df$age = (df$admit.day$year - df$dob$year) - 1 * ((df$admit.day$mon < df$dob$mon) | (df$admit.day$mon == df$dob$mon & df$admit.day$mday < df$dob$mday))
df$sex = factor(merge(enc,pat,by="patient_id")$geschlecht)
levels(df$sex) <- list("male"="male","female"="female","unbestimmt"="indeterminate")

df$triage.result = as.factor(enc$triage)
levels(df$triage.result) <- list("Rot"="1","Orange"="2","Gelb"="3","Grün"="4","Blau"="5","Ohne"="NA")
df$triage.result[is.na(df$triage.result)] <- 'Ohne'

# Referral Codes
df$referral <- factor(x=enc$zuweisung)
levels(df$referral) <- list("Vertragsarzt"="VAP","KV-Notfallpraxis am Krankenhaus"="KVNPIK","	KV-Notdienst ausserhalb des Krankenhauses	"="KVNDAK","Rettungsdienst"="RD","Notarzt"="NA","Klinik/Verlegung"="KLINV","Ohne/Zuweisung nicht durch Arzt"="NPHYS","Andere"="OTH")

#Transport Codes
df$transport <- factor(x=enc$transportmittel)
levels(df$transport) <- list("Ohne"="NA","KTW"="1","RTW"="2","NAW/NEF/ITW"="3","RTH/ITH"="4","Anderes"="OTH")

#Discharge Codes
df$discharge = factor(x=enc$entlassung)
levels(df$discharge) <- list("Tod"="DISCHARGE:1","Gegen ärztl. Rat"="DISCHARGE:2","Abbruch durch Pat."="DISCHARGE:3","Nach Hause"="DISCHARGE:4","Zu weiterbehandl. Arzt"="DISCHARGE:5","Kein Arztkontakt"="DISCHARGE:6","Sonstiges"="DISCHARGE:OTH","Intern: Funktion"="TRANSFER:1","Extern: Funktion"="TRANSFER:2","Intern: Überwachung"="TRANSFER:3","Extern: Überwachung"="TRANSFER:4","Intern: Normalstation"="TRANSFER:5","Extern: Normalstation"="TRANSFER:6")

#Isolation Codes
df$isolation = factor(x=enc$isolation)
levels(df$isolation) <- list("Isolation"="ISO","Keine Isolation"="ISO:NEG","Umkehrisolation"="RISO")
df$isolation_grund = factor(x=enc$isolation_grund)
levels(df$isolation_grund) <- list("Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Andere"="OTH")

#CEDIS Codes
df$cedis <- factor(x=enc$cedis,t(cedis[1]))

#Multiresistente Erreger
df$keime <- factor(enc$keime)
df$keime_mrsa <- factor(enc$keime_mrsa)
df$keime_3mrgn <- factor(enc$keime_3mrgn)
df$keime_4mrgn <- factor(enc$keime_4mrgn)
df$keime_vre <- factor(enc$keime_vre)
df$keime_andere <- factor(enc$keime_andere)

#Diagnoses
df_diag$fuehrend = diag$diagnose_fuehrend
df_diag$zusatz = diag$diagnose_zusatz


# Generate derived information


weekday.levels <- c('Mo','Di','Mi','Do','Fr','Sa','So')
# Weekday Index
admit.wdi <- as.integer(strftime(df$admit.ts,"%u"))
# Weekday of admission
df$admit.wd <- factor(x=weekday.levels[admit.wdi], levels=weekday.levels, ordered=TRUE)

# Day of admission
# (Extract Admit.ts again with lower precision)
df$admit.day <- factor(x=strftime(strptime(enc$aufnahme_ts,tz="GMT",format="%F"),tz="GMT",format="%F"), ordered=TRUE)

# Day of discharge
df$discharge.day <- factor(x=strftime(df$discharge.ts,tz="GMT",format="%F"), ordered=TRUE)

# Hour of admission
hour.levels = sprintf('%02i',0:23)
df$admit.h <- factor(x=strftime(df$admit.ts,tz="GMT",format="%H"), levels=hour.levels, ordered=TRUE)

#Hour of admission per weekday
admit.hwd <- matrix(NA,7,24)
for(i in 1:length(weekday.levels)) {
  admit.hwd[i,] <- as.vector(table(factor(x=strftime(df$admit.ts[df$admit.wd == weekday.levels[i]],tz="GMT",format="%H"), levels=hour.levels, ordered=TRUE)))
}

# Hour of discharge
df$discharge.h <- factor(x=strftime(df$discharge.ts,tz="GMT",format="%H"), levels=hour.levels, ordered=TRUE)

# Time to triage
df$triage.d <- df$triage.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$triage.d[df$triage.d < 0] <- NA
df$triage.d[df$triage.d > 24*60] <- NA

# Time to physician
df$phys.d <- df$phys.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$phys.d[df$phys.d < 0] <- NA
df$phys.d[df$phys.d > 24*60] <- NA

# Time phys to therapy
df$therapy.d <- df$therapy.ts - df$phys.ts
# Values out of bounds (<0h or >24h) => NA
df$therapy.d[df$therapy.d < 0] <- NA
df$therapy.d[df$therapy.d > 24*60] <- NA

# Time to discharge
df$discharge.d <- df$discharge.ts - df$admit.ts
# Values out of bounds (<0h or >24h) => NA
df$discharge.d[df$discharge.d < 0] <- NA
df$discharge.d[df$discharge.d > 24*60] <- NA     #could be more than 24 hours!