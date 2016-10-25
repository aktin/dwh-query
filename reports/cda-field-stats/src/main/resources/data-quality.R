#!/usr/bin/Rscript

#std_cols1 <- c("firebrick3")
#std_cols3 <- c("firebrick3","blue","green")
#std_cols5 <- c("firebrick3","orange","yellow","green","blue","white")

#round_df <- function(df, digits) {
#  nums <- vapply(df, is.numeric, FUN.VALUE = logical(1))
#  
#  df[,nums] <- round(df[,nums], digits = digits)
#  
#  (df)
#}

options(OutDec= ",")

gformat <- function(num,digits=0) {
  prettyOut <- format(round(as.numeric(num), digits), nsmall=digits, big.mark=".")
  (prettyOut)
}

#stdabw <- function(x) {n=length(x) ; sqrt(var(x) * (n-1) / n)}

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
diag <- read.table(file='diagnoses.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='',stringsAsFactors=FALSE, colClasses = c("character"))

# create new data frame for encounter data to contain clean values
df = data.frame(patient=pat$patient_id, encounter=enc$encounter_id)

# create new data frame for diagnoses data to contain clean values
df_diag <- data.frame(diagnosis=as.factor(substring(diag$diagnose_code, first=1, last=3)))

#load CEDIS mapping table
#cedis = read.csv2(file='CEDIS.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';')

#load ICD mapping table
#icd = read.csv2(file='ICD-3Steller.csv', as.is=TRUE, na.strings='', header = FALSE, sep=';')

# parse timestamps and date fields
# The timestamp values are assumed to belong to the local timezone
# TODO check timezones 
df$dob = strptime(pat$geburtsdatum_ts,format="%F")
#df$triage.ts = strptime(enc$triage_ts,format="%F %H:%M")
#df$admit.ts = strptime(enc$aufnahme_ts,format="%F %H:%M")
#df$phys.ts = strptime(enc$arztkontakt_ts, format="%F %H:%M")
#df$therapy.ts = strptime(enc$therapiebeginn_ts, format="%F %H:%M")
#df$discharge.ts = strptime(enc$entlassung_ts, format="%F %H:%M")
df$triage.ts = strptime(enc$triage_ts,format="%FT%H:%M")
df$admit.ts = strptime(enc$aufnahme_ts,format="%FT%H:%M")
df$phys.ts = strptime(enc$arztkontakt_ts, format="%FT%H:%M")
df$therapy.ts = strptime(enc$therapiebeginn_ts, format="%FT%H:%M")
df$discharge.ts = strptime(enc$entlassung_ts, format="%FT%H:%M")

# TODO This is probably not the ideal way to calculate the age
df$age = floor(as.numeric(difftime(df$admit.ts,df$dob))/365.25)
df$sex = factor(pat$geschlecht)
levels(df$sex) <- list("male"="male","female"="female")

df$triage.result = as.factor(enc$triage)
levels(df$triage.result) <- list("Rot"="1","Orange"="2","Gelb"="3","Gruen"="4","Blau"="5","Ohne"="NA")
df$triage.result[is.na(df$triage.result)] <- 'Ohne'

# Referral Codes
df$referral <- factor(x=enc$zuweisung)
#levels(df$referral) <- list("Vertragsarzt"="AKTIN:REFERRAL:VAP","KV-Notfallpraxis am Krankenhaus"="AKTIN:REFERRAL:KVNPIK","	KV-Notdienst ausserhalb des Krankenhauses	"="AKTIN:REFERRAL:KVNDAK","Rettungsdienst"="AKTIN:REFERRAL:RD","Notarzt"="AKTIN:REFERRAL:NA","Klinik/Verlegung"="AKTIN:REFERRAL:KLINV","Zuweisung nicht durch Arzt"="AKTIN:REFERRAL:NPHYS")
levels(df$referral) <- list("Vertragsarzt"="VAP","KV-Notfallpraxis am Krankenhaus"="KVNPIK","	KV-Notdienst ausserhalb des Krankenhauses	"="KVNDAK","Rettungsdienst"="RD","Notarzt"="NA","Klinik/Verlegung"="KLINV","Zuweisung nicht durch Arzt"="NPHYS")

#Transport Codes
df$transport <- factor(x=enc$transportmittel)
#levels(df$transport) <- list("Ohne"="AKTIN:TRANSPORT:NA","KTW"="AKTIN:TRANSPORT:1","RTW"="AKTIN:TRANSPORT:2","NAW/NEF/ITW"="AKTIN:TRANSPORT:3","RTH/ITH"="AKTIN:TRANSPORT:4","Anderes"="AKTIN:TRANSPORT:OTH")
levels(df$transport) <- list("Ohne"="NA","KTW"="1","RTW"="2","NAW/NEF/ITW"="3","RTH/ITH"="4","Anderes"="OTH")

#Discharge Codes
df$discharge = factor(x=enc$entlassung)
#levels(df$discharge) <- list("Tod"="AKTIN:DISCHARGE:1","Gegen aerztl. Rat"="AKTIN:DISCHARGE:2","Abbruch durch Pat."="AKTIN:DISCHARGE:3","Nach Hause"="AKTIN:DISCHARGE:4","Zu weiterbehandl. Arzt"="AKTIN:DISCHARGE:5","Kein Arztkontakt"="AKTIN:DISCHARGE:6","Sonstiges"="AKTIN:DISCHARGE:OTH","Intern: Funktion"="AKTIN:TRANSFER:1","Extern: Funktion"="AKTIN:TRANSFER:2","Intern: Ueberwachung"="AKTIN:TRANSFER:3","Extern: Ueberwachung"="AKTIN:TRANSFER:4","Intern: Normalstation"="AKTIN:TRANSFER:5","Extern: Normalstation"="AKTIN:TRANSFER:6")
levels(df$discharge) <- list("Tod"="DISCHARGE:1","Gegen aerztl. Rat"="DISCHARGE:2","Abbruch durch Pat."="DISCHARGE:3","Nach Hause"="DISCHARGE:4","Zu weiterbehandl. Arzt"="DISCHARGE:5","Kein Arztkontakt"="DISCHARGE:6","Sonstiges"="DISCHARGE:OTH","Intern: Funktion"="TRANSFER:1","Extern: Funktion"="TRANSFER:2","Intern: Ueberwachung"="TRANSFER:3","Extern: Ueberwachung"="TRANSFER:4","Intern: Normalstation"="TRANSFER:5","Extern: Normalstation"="TRANSFER:6")

#Isolation Codes
df$isolation = factor(x=enc$isolation)
levels(df$isolation) <- list("Isolation"="ISO","Keine Isolation"="ISO:NEG","Umkehrisolation"="RISO")
df$isolation_grund = factor(x=enc$isolation_grund)
levels(df$isolation_grund) <- list("Multiresistenter Keim"="U80","Gastroenteritis"="A09.9","Tuberkulose"="A16.9","Meningitis"="G03.9","Andere"="OTH")

#CEDIS Codes
#df$cedis <- factor(x=enc$cedis,t(cedis[1]))
df$cedis <- factor(enc$cedis)

#Multiresistente Erreger
df$keime <- factor(enc$keime)
df$keime_mrsa <- factor(enc$keime_mrsa)
df$keime_3mrgn <- factor(enc$keime_3mrgn)
df$keime_4mrgn <- factor(enc$keime_4mrgn)
df$keime_vre <- factor(enc$keime_vre)
df$keime_andere <- factor(enc$keime_andere)

df$plz <- enc$postleitzahl
df$iknr <- enc$versicherung_iknr
df$vers_txt <- enc$versicherung_txt


#Diagnoses
df_diag$fuehrend = diag$diagnose_fuehrend
df_diag$zusatz = diag$diagnose_zusatz



# XHTML tables
source('xhtml-table.R')
xml.dir <- ''

# Graphics & Plots
library(lattice)

gfx.dir <- ''
gfx.ext <- '.svg'
gfx.dev <- 'svg'



#addrow <- function(insert_var) {
#  prettyOut <- format(round(as.numeric(num), digits), nsmall=digits, big.mark=".")
#  (prettyOut)
#}

encounter_num <- length(df$encounter)

#Definition of reported items
var_list <- c("Alter",
              "Geschlecht",
              "Postleitzahl",
              "Versicherung IK-Nummer",
              "Versicherung Name",
              "Zuweisung",
              "Transportmittel",
              "CEDIS",
              "Symptomdauer",
              "Triage",
              "Atemfrequenz",
              
              "Sauerstoffsaettigung",
              "Herzfrequenz",
              "Koerperkerntemperatur",
              "Schmerzskala",
              "Glasgow Coma Scale",
              "GCS Augen",
              "GCS verbal",
              "GCS motorisch",
              "Pupillenweite rechts",
              "Pupillenweite links",
              
              "Pupillenreaktion rechts",
              "Pupillenreaktion links",
              "Rankin",
              "Schwangerschaft",
              "Tetanusschutz",
              "Allergien",
              "Kontrastmittelallergie",
              "Antibiotikaallergie",
              "Sonstige Allergie",
              "Isolation",
              
              "Isolationsgrund",
              "Multiresistente Keime",
              #Diagnostik
              "Führende Diagnose",
              "Verlegund/Entlassung"
              #Zeitpunkte
              #Module
              )
var_count <- c(sum(!is.na(df$age)),
               sum(!is.na(df$sex)),
               sum(!is.na(df$plz)),
               sum(!is.na(df$iknr)),
               sum(!is.na(df$vers_txt)),
               sum(!is.na(df$referral)),
               sum(!is.na(df$transport)),
               sum(!is.na(df$cedis)),
               0, #Symptomdauer
               sum(!is.na(df$triage.result)),
               sum(!is.na(enc$atemfrequenz)),
               
               0, #SpO2
               0, #HF
               0, #KT
               0, #Schmerz
               0, #GCS
               0, #GCS
               0, #GCS
               0, #GCS
               0, #Pupillen
               0, #Pupillen
              
               0, #Pupillen
               0, #Pupillen
               0, #Rankin
               0, #Schwangerschaft
               0, #Tetanus
               0, #Allergien
               0, #Allergien
               0, #Allergien
               0, #Allergien
               sum(!is.na(df$isolation)),
               
               sum(!is.na(df$isolation_grund)),
               sum(!(is.na(df$keime) & is.na(df$keime_vre) & is.na(df$keime_mrsa) & is.na(df$keime_3mrgn) & is.na(df$keime_4mrgn) & is.na(df$keime_andere))),
               sum(!is.na(df_diag$zusatz)),
               sum(!is.na(df$discharge))
               )
                
#,Missing=encounter_num-var_count

compl_table <- data.frame(Variable=var_list,Anteil=gformat(var_count/encounter_num*100,digits = 2))
compl_table[,2] <- paste(compl_table[,2],'%')
xhtml.table(compl_table, file=paste0(xml.dir,'complete.xml'),align=c('left','right'),widths=c(35,15))

# Weitere Tabelle:
#"Abschlussdiagnosen",
#

