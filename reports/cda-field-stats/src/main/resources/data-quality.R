#!/usr/bin/Rscript

options(OutDec= ",")

gformat <- function(num,digits=0) {
  prettyOut <- format(round(as.numeric(num), digits), nsmall=digits, big.mark=".")
  (prettyOut)
}

# load data into data frame and keep all data as strings
# this will prevent R from creating factors out of dates and prefixed values
pat <- read.table(file='patients.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
enc <- read.table(file='encounters.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='')
diag <- read.table(file='diagnoses.txt',header=TRUE, sep='\t', as.is=TRUE, na.strings='',stringsAsFactors=FALSE, colClasses = c("character"))

# XHTML tables
source('xhtml-table.R')
xml.dir <- ''

# Graphics & Plots
#library(lattice)

#gfx.dir <- ''
#gfx.ext <- '.svg'
#gfx.dev <- 'svg'


#Definition of reported items
var_list <- c("Alter",
              "Geschlecht",
              "Postleitzahl",
              "IK-Nummer der Versicherung",
              "Name der Versicherung (Freitext)",
              "Zuweisung",
              "Transportmittel",
              "Vorstellungsgrund (CEDIS)",
              "Beschwerden bei Vorstellung (Freitext)",
              "Symptomdauer",
              "Triage",
              "Verwendetes Ersteinschätzungssystem",
              "Atemfrequenz",

              "Sauerstoffsättigung",
              "Herzfrequenz",
              "Körperkerntemperatur",
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
              "Allergie Spezifizierung (Freitext)",
              "Isolation",
              
              "Isolationsgrund",
              "Multiresistente Keime",
              "Verlegung/Entlassung",
              "Aufnahmedatum und -zeitpunkt",
              "Zeitpunkt der Ersteinschätzung",
              "Erster Arztkontakt",
              "Beginn der Therapie",
              "Zeitpunkt der Verlegung/Entlassung",
              #Module z.Zt. nicht relevant
              
              "Labor durchgeführt",
              "Labor Ergebnis",
              "Labor Zeitpunkt",
              "Blutgase durchgeführt",
              "Blutgase Ergebnis",
              "Blutgase Zeitpunkt",
              "Urinschnelltest durchgeführt",
              "Urinschnelltest Ergebnis",
              "Urinschnelltest Zeitpunkt",
              "EKG durchgeführt",
              "EKG Ergebnis",
              "EKG Zeitpunkt",
              "Sonographie durchgeführt",
              "Sonographie Ergebnis",
              "Sonographie Zeitpunkt",
              "Echokardiographie durchgeführt",
              "Echokardiographie Ergebnis",
              "Echokardiographie Zeitpunkt",
              "CCT durchgeführt",
              "CCT Ergebnis",
              "CCT Zeitpunkt",
              "CT durchgeführt",
              "CT Ergebnis",
              "CT Zeitpunkt",
              "Traumascan durchgeführt",
              "Traumascan Ergebnis",
              "Traumascan Zeitpunkt",
              "Röntgen Wirbelsäule durchgeführt",
              "Röntgen Wirbelsäule Ergebnis",
              "Röntgen Wirbelsäule Zeitpunkt",
              "Röntgen Thorax durchgeführt",
              "Röntgen Thorax Ergebnis",
              "Röntgen Thorax Zeitpunkt",
              "Röntgen Becken durchgeführt",
              "Röntgen Becken Ergebnis",
              "Röntgen Becken Zeitpunkt",
              "Röntgen Extremitäten durchgeführt",
              "Röntgen Extremitäten Ergebnis",
              "Röntgen Extremitäten Zeitpunkt",
              "Röntgen Sonstiges durchgeführt",
              "Röntgen Sonstiges Ergebnis",
              "Röntgen Sonstiges Zeitpunkt",
              "MRT durchgeführt",
              "MRT Ergebnis",
              "MRT Zeitpunkt"
              )
var_count <- c(sum(!is.na(pat$geburtsdatum_ts)),
               sum(!is.na(pat$geschlecht)),
               sum(!is.na(enc$postleitzahl)),
               sum(!is.na(enc$versicherung_iknr)),
               sum(!is.na(enc$versicherung_txt)),
               sum(!is.na(enc$zuweisung)),
               sum(!is.na(enc$transportmittel)),
               sum(!is.na(enc$cedis)),
               sum(!is.na(enc$beschwerden_txt)),
               sum(!is.na(enc$symptomdauer)),
               sum(!is.na(enc$triage)),
               sum(!is.na(enc$triage_system)),
               sum(!is.na(enc$atemfrequenz)),
               
               sum(!is.na(enc$saettigung)),
               sum(!is.na(enc$herzfrequenz)),
               sum(!is.na(enc$kerntemperatur)),
               sum(!is.na(enc$schmerzskala)),
               sum(!is.na(enc$gcs_summe)),
               sum(!is.na(enc$gcs_augen)),
               sum(!is.na(enc$gcs_verbal)),
               sum(!is.na(enc$gcs_motorisch)),
               sum(!is.na(enc$pupillenweite_rechts)),
               sum(!is.na(enc$pupillenweite_links)),
              
               sum(!is.na(enc$pupillenreaktion_rechts)),
               sum(!is.na(enc$pupillenreaktion_links)),
               sum(!is.na(enc$rankin)),
               sum(!is.na(enc$schwangerschaft)),
               sum(!is.na(enc$tetanusschutz)),
               sum(!is.na(enc$allergie)),
               sum(!is.na(enc$allergie_kontrastmittel)),
               sum(!is.na(enc$allergie_antibiotika)),
               sum(!is.na(enc$allergie_sonstige)),
               sum(!is.na(enc$allergie_txt)),
               sum(!is.na(enc$isolation)),
               
               sum(!is.na(enc$isolation_grund)),
               sum(!(is.na(enc$keime) & is.na(enc$keime_vre) & is.na(enc$keime_mrsa) & is.na(enc$keime_3mrgn) & is.na(enc$keime_4mrgn) & is.na(enc$keime_andere))),
               sum(!is.na(enc$entlassung)),
               sum(!is.na(enc$aufnahme_ts)),
               sum(!is.na(enc$triage_ts)),
               sum(!is.na(enc$arztkontakt_ts)),
               sum(!is.na(enc$therapiebeginn_ts)),
               sum(!is.na(enc$entlassung_ts)),
               
               sum(!is.na(enc$diagnostik_labor)),
               sum(!is.na(enc$diagnostik_labor_ergebnis)),
               sum(!is.na(enc$diagnostik_labor_ts)),
               sum(!is.na(enc$diagnostik_blutgase)),
               sum(!is.na(enc$diagnostik_blutgase_ergebnis)),
               sum(!is.na(enc$diagnostik_blutgase_ts)),
               sum(!is.na(enc$diagnostik_urinschnelltest)),
               sum(!is.na(enc$diagnostik_urinschnelltest_ergebnis)),
               sum(!is.na(enc$diagnostik_urinschnelltest_ts)),
               sum(!is.na(enc$diagnostik_ekg)),
               sum(!is.na(enc$diagnostik_ekg_ergebnis)),
               sum(!is.na(enc$diagnostik_ekg_ts)),
               sum(!is.na(enc$diagnostik_sonographie)),
               sum(!is.na(enc$diagnostik_sonographie_ergebnis)),
               sum(!is.na(enc$diagnostik_sonographie_ts)),
               sum(!is.na(enc$diagnostik_echokardiographie)),
               sum(!is.na(enc$diagnostik_echokardiographie_ergebnis)),
               sum(!is.na(enc$diagnostik_echokardiographie_ts)),
               sum(!is.na(enc$diagnostik_ct_kopf)),
               sum(!is.na(enc$diagnostik_ct_kopf_ergebnis)),
               sum(!is.na(enc$diagnostik_ct_kopf_ts)),
               sum(!is.na(enc$diagnostik_ct)),
               sum(!is.na(enc$diagnostik_ct_ergebnis)),
               sum(!is.na(enc$diagnostik_ct_ts)),
               sum(!is.na(enc$diagnostik_ct_trauma)),
               sum(!is.na(enc$diagnostik_ct_trauma_ergebnis)),
               sum(!is.na(enc$diagnostik_ct_trauma_ts)),
               sum(!is.na(enc$diagnostik_roentgen_wirbelsaeule)),
               sum(!is.na(enc$diagnostik_roentgen_wirbelsaeule_ergebnis)),
               sum(!is.na(enc$diagnostik_roentgen_wirbelsaeule_ts)),
               sum(!is.na(enc$diagnostik_roentgen_thorax)),
               sum(!is.na(enc$diagnostik_roentgen_thorax_ergebnis)),
               sum(!is.na(enc$diagnostik_roentgen_thorax_ts)),
               sum(!is.na(enc$diagnostik_roentgen_becken)),
               sum(!is.na(enc$diagnostik_roentgen_becken_ergebnis)),
               sum(!is.na(enc$diagnostik_roentgen_becken_ts)),
               sum(!is.na(enc$diagnostik_roentgen_extremitaeten)),
               sum(!is.na(enc$diagnostik_roentgen_extremitaeten_ergebnis)),
               sum(!is.na(enc$diagnostik_roentgen_extremitaeten_ts)),
               sum(!is.na(enc$diagnostik_roentgen_sonstiges)),
               sum(!is.na(enc$diagnostik_roentgen_sonstiges_ergebnis)),
               sum(!is.na(enc$diagnostik_roentgen_sonstiges_ts)),
               sum(!is.na(enc$diagnostik_mrt)),
               sum(!is.na(enc$diagnostik_mrt_ergebnis)),
               sum(!is.na(enc$diagnostik_mrt_ts))
               )

encounter_num <- length(enc$encounter_id)
                
#,Missing=encounter_num-var_count

enc_table <- data.frame(Variable=var_list,Anteil=gformat(var_count/encounter_num*100,digits = 2))
enc_table[,2] <- paste(enc_table[,2],'%')
xhtml.table(enc_table, file=paste0(xml.dir,'complete.xml'),align=c('left','right'),widths=c(50,15))



#Abschlussdiagnosen
diag$diagnose_code_count <- 0
diag$diagnose_code_count[!is.na(diag$diagnose_code)] <- 1
diagnose_count <- aggregate(diag$diagnose_code_count, by=list(Encounter=diag$encounter_id), FUN=sum)

diag$ausschluss_count <- 0
diag$ausschluss_count[diag$diagnose_zusatz=="A"] <- 1
ausschluss_count <- aggregate(diag$ausschluss_count, by=list(Encounter=diag$encounter_id), FUN=sum)
#sum(ausschluss_count$x,na.rm = TRUE)

diag$zustand_count <- 0
diag$zustand_count[diag$diagnose_zusatz=="Z"] <- 1
zustand_count <- aggregate(diag$zustand_count, by=list(Encounter=diag$encounter_id), FUN=sum)

diag$gesichert_count <- 0
diag$gesichert_count[diag$diagnose_zusatz=="G"] <- 1
gesichert_count <- aggregate(diag$gesichert_count, by=list(Encounter=diag$encounter_id), FUN=sum)

diag$verdacht_count <- 0
diag$verdacht_count[diag$diagnose_zusatz=="V"] <- 1
verdacht_count <- aggregate(diag$verdacht_count, by=list(Encounter=diag$encounter_id), FUN=sum)


diag_table <- data.frame(Variable="Fälle mit mindestens einer Diagnose",Anteil=gformat(length(diagnose_count$x[diagnose_count$x > 0])/encounter_num*100,digits = 2))
diag_table <- rbind(diag_table,data.frame(Variable="Fälle mit (genau) einer führenden Diagnose",Anteil=gformat(sum(!is.na(diag$diagnose_fuehrend))/encounter_num*100,digits = 2)))
diag_table <- rbind(diag_table,data.frame(Variable="Fälle mit mindestens einer gesicherten Diagnose",Anteil=gformat(length(gesichert_count$x[gesichert_count$x > 0])/encounter_num*100,digits = 2)))
diag_table <- rbind(diag_table,data.frame(Variable="Fälle mit mindestens einer Verdachtsdiagnose",Anteil=gformat(length(verdacht_count$x[verdacht_count$x > 0])/encounter_num*100,digits = 2)))
diag_table <- rbind(diag_table,data.frame(Variable="Fälle mit mindestens einer 'Zustand nach'-Diagnose",Anteil=gformat(length(zustand_count$x[zustand_count$x > 0])/encounter_num*100,digits = 2)))
diag_table <- rbind(diag_table,data.frame(Variable="Fälle mit mindestens einer Ausschlussdiagnose",Anteil=gformat(length(ausschluss_count$x[ausschluss_count$x > 0])/encounter_num*100,digits = 2)))
diag_table[,2] <- paste(diag_table[,2],'%')
xhtml.table(diag_table, file=paste0(xml.dir,'diagnoses.xml'),align=c('left','right'),widths=c(65,15))