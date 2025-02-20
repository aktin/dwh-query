# Encounters data
enc$aufnahme_ts <- strptime(enc$aufnahme_ts, "%FT%T")
enc$arztkontakt_ts <- strptime(enc$arztkontakt_ts, "%FT%T")
enc$therapiebeginn_ts <- strptime(enc$therapiebeginn_ts, "%FT%T")
enc$entlassung_ts <- strptime(enc$entlassung_ts, "%FT%T")
enc$triage_ts <- strptime(enc$triage_ts, "%FT%T")

# Patients data
colnames(pat) <- c("patient_id", "dob", "sex")

# Merge of encounters and patients data into one data frame
df <- data.frame(patient = enc$patient_id, encounter = enc$encounter_id)
colnames(df) <- c("patient_id", "encounter")


df <- left_join_base(df, pat, by = "patient_id")
df$dob <- strptime(df$dob, format = "%Y-%m-%d", tz = "GMT")
df$triage.ts <- strptime(enc$triage_ts, format = "%Y-%m-%d %H:%M", tz = "GMT")
df$admit.ts <- strptime(enc$aufnahme_ts, format = "%Y-%m-%d %H:%M", tz = "GMT")
df$admit.day <- strptime(enc$aufnahme_ts, tz = "GMT", format = "%Y-%m-%d")
df$phys.ts <- strptime(enc$arztkontakt_ts, format = "%Y-%m-%d %H:%M", tz = "GMT")
df$therapy.ts <- strptime(enc$therapiebeginn_ts, format = "%Y-%m-%d %H:%M", tz = "GMT")
df$discharge.ts <- strptime(enc$entlassung_ts, format = "%Y-%m-%d %H:%M", tz = "GMT")
df$age <- (df$admit.day$year - df$dob$year) - 1 * (df$admit.day$yday < df$dob$yday)
df$triage.result <- as.factor(enc$triage)
gender_categories <- c("female", "male", "indetermined")
df$sex <- factor(df$sex, levels = gender_categories)
levels(df$triage.result) <- list("Rot" = "1", "Orange" = "2", "Gelb" = "3", "Grün" = "4", "Blau" = "5", "Ohne" = "NA")
df$triage.result[is.na(df$triage.result)] <- "Ohne"

# Referral Codes
df$referral <- factor(x = enc$zuweisung)
levels(df$referral) <- list(
  "Vertragsarzt" = "VAP",
  "KV-Notfallpraxis am Krankenhaus" = "KVNPIK",
  "KV-Notdienst ausserhalb des Krankenhauses" = "KVNDAK",
  "Rettungsdienst" = "RD", "Notarzt" = "NA",
  "Klinik/Verlegung" = "KLINV",
  "Ohne/Zuweisung nicht durch Arzt" = "NPHYS", "Andere" = "OTH"
)

# Transport Codes
df$transport <- factor(x = enc$transportmittel)
levels(df$transport) <- list(
  "Ohne" = "NA",
  "KTW" = "1",
  "RTW" = "2",
  "NAW/NEF/ITW" = "3",
  "RTH/ITH" = "4",
  "Anderes" = "OTH"
)

# Discharge Codes
df$discharge <- factor(x = enc$entlassung)
levels(df$discharge) <- list(
  "Tod" = "DISCHARGE:1",
  "Gegen ärztl. Rat" = "DISCHARGE:2",
  "Abbruch durch Pat." = "DISCHARGE:3",
  "Nach Hause" = "DISCHARGE:4",
  "Zu weiterbehandl. Arzt" = "DISCHARGE:5",
  "Kein Arztkontakt" = "DISCHARGE:6",
  "Sonstiges" = "DISCHARGE:OTH",
  "Intern: Funktion" = "TRANSFER:1",
  "Extern: Funktion" = "TRANSFER:2",
  "Intern: Überwachung" = "TRANSFER:3",
  "Extern: Überwachung" = "TRANSFER:4",
  "Intern: Normalstation" = "TRANSFER:5",
  "Extern: Normalstation" = "TRANSFER:6"
)

# Isolation Codes
df$isolation <- factor(x = enc$isolation)
levels(df$isolation) <- list("Isolation" = "ISO", "Keine Isolation" = "ISO:NEG", "Umkehrisolation" = "RISO")
df$isolation_grund <- factor(x = enc$isolation_grund)
levels(df$isolation_grund) <- list(
  "Multiresistenter Keim" = "U80",
  "Gastroenteritis" = "A09.9",
  "Tuberkulose" = "A16.9",
  "Meningitis" = "G03.9",
  "Andere" = "OTH"
)

# CEDIS Codes
df$cedis <- factor(x = enc$cedis, t(cedis[1]))

# Multiresistente Erreger
df$keime <- factor(enc$keime)
df$keime_mrsa <- factor(enc$keime_mrsa)
df$keime_3mrgn <- factor(enc$keime_3mrgn)
df$keime_4mrgn <- factor(enc$keime_4mrgn)
df$keime_vre <- factor(enc$keime_vre)
df$keime_andere <- factor(enc$keime_andere)

# Diagnoses
df_diag <- data.frame(diagnosis = as.factor(substring(diag$diagnose_code, first = 1, last = 3))) ### Neccessary?
df_diag$fuehrend <- diag$diagnose_fuehrend
df_diag$zusatz <- diag$diagnose_zusatz


# Generate derived information
weekday_levels <- c("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
# Weekday Index
admit_wdi <- as.integer(strftime(df$admit.ts, "%u"))
# Weekday of admission
df$admit.wd <- factor(x = weekday_levels[admit_wdi], levels = weekday_levels, ordered = TRUE)

# Day of admission
# (Extract Admit.ts again with lower precision)
df$admit.day <- factor(x = strftime(
  strptime(enc$aufnahme_ts, tz = "GMT", format = "%F"),
  tz = "GMT", format = "%F"
), ordered = TRUE)

# Day of discharge
df$discharge.day <- factor(x = strftime(df$discharge.ts, tz = "GMT", format = "%F"), ordered = TRUE)

# Hour of admission
hour_levels <- sprintf("%02i", 0:23)
df$admit.h <- factor(x = strftime(df$admit.ts, tz = "GMT", format = "%H"), levels = hour_levels, ordered = TRUE)

# Hour of admission per weekday
admit_hwd <- matrix(NA, 7, 24)
for (i in seq_along(weekday_levels)) {
  admit_hwd[i, ] <- as.vector(
    table(
      factor(x = strftime(
        df$admit.ts[df$admit.wd == weekday_levels[i]],
        tz = "GMT", format = "%H"
      ), levels = hour_levels, ordered = TRUE)
    )
  )
}

# Hour of discharge
df$discharge.h <- factor(x = strftime(df$discharge.ts, tz = "GMT", format = "%H"), levels = hour_levels, ordered = TRUE)

# Time to triage
df$triage.d <- difftime(df$triage.ts, df$admit.ts, units = "mins")
# Values out of bounds (<0h or >24h) => NA
df$triage.d[df$triage.d < 0] <- NA
df$triage.d[df$triage.d > 24 * 60] <- NA
df$triage.d <- as.numeric(df$triage.d)

# Time to physician
df$phys.d <- difftime(df$phys.ts, df$admit.ts, units = "mins")
# Values out of bounds (<0h or >24h) => NA
df$phys.d[df$phys.d < 0] <- NA
df$phys.d[df$phys.d > 24 * 60] <- NA
df$phys.d <- as.numeric(df$phys.d)

# Time phys to therapy
df$therapy.d <- difftime(df$therapy.ts, df$phys.ts, units = "mins")
# Values out of bounds (<0h or >24h) => NA
df$therapy.d[df$therapy.d < 0] <- NA
df$therapy.d[df$therapy.d > 24 * 60] <- NA

# Time to discharge
df$discharge.d <- difftime(df$discharge.ts, df$admit.ts, units = "mins")
# Values out of bounds (<0h or >24h) => NA
df$discharge.d[df$discharge.d < 0] <- NA
df$discharge.d[df$discharge.d > 24 * 60] <- NA
