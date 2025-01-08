# TOP20 CEDIS
std_cols5 <- c("firebrick3", "orange", "yellow", "forestgreen", "dodgerblue", "aliceblue") ### chapter8

try(
  {
    df_cedis <- data.frame(df$cedis)
    df_cedis[is.na(df_cedis)] <- "999"
    df_cedis_na <- df_cedis[df_cedis$df.cedis == "999", ]
    num_missing_values <- length(df_cedis_na)

    level_counts <- as.data.frame(table(df_cedis$df.cedis))
    names(level_counts) <- c("cedis_codes", "count")
    top_counts <- level_counts[order(-level_counts$count), ][1:20, ]
    top_counts[top_counts$count == 0] <- NA


    if (nrow(top_counts) > 0) {
      graph <- ggplot(data = top_counts, aes(reorder(cedis_codes, count), count)) +
        geom_bar(stat = "identity", fill = "#046C9A", width = 0.5) +
        labs(y = "Anzahl Patienten", x = "CEDIS") +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.x = element_text(face = "bold", color = "#000000", size = 12),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12)
        ) +
        coord_flip()
    } else {
      graph <- create_no_data_figure()
    }
    report_svg(graph, "cedis_top_test")
    rm(graph)

    ## Very similar to chapter1 - Patient Sex - Function?
    cedis_summary <- data.frame(
      Code = as.character(top_counts$cedis_codes),
      Category = factor(top_counts$cedis_codes, levels = cedis[[1]], labels = cedis[[3]]),
      Count = as.numeric(top_counts$count),
      Percentage = (top_counts$count / length(df$encounter)) * 100
    )

    cedis_summary <- rbind(
      cedis_summary,
      data.frame(
        Code = "---",
        Category = "Summe TOP20",
        Count = sum(top_counts$count),
        Percentage = sum(cedis_summary$Percentage, na.rm = TRUE)
      )
    )

    cedis_summary <- rbind(
      cedis_summary,
      data.frame(
        Code = "999",
        Category = "Unbekannt",
        Count = num_missing_values,
        Percentage = (num_missing_values / length(df$encounter)) * 100
      )
    )

    cedis_summary$Count <- format_number(cedis_summary$Count)
    cedis_summary$Percentage <- paste(format_number(cedis_summary$Percentage, digits = 1), "%")

    report_table(
      cedis_summary,
      name = "cedis_test.xml",
      align = c("left", "left", "right", "right"),
      widths = c(8, 60, 15, 15),
      translations = column_name_translations
    )
    rm(cedis_summary)
  },
  silent = FALSE
)

# CEDIS Groups
try(
  {
    cedis_cat_top <- factor(enc$cedis, levels = t(cedis[1]), labels = t(cedis[2]))

    # Rename levels
    levels(cedis_cat_top) <- c(
      "CV" = "Kardiovaskulär", "HNE" = "HNO (Ohren)",
      "HNM" = "HNO (Mund, Rachen, Hals)", "HNN" = "HNO (Nase)",
      "EV" = "Umweltbedingt", "GI" = "Gastrointestinal",
      "GU" = "Urogenital", "MH" = "Psychische Verfassung",
      "NC" = "Neurologisch", "GY" = "Geburtshilfe/Gynäkologie",
      "EC" = "Augenheilkunde", "OC" = "Orthopädisch/Unfall-chirurgisch",
      "RC" = "Respiratorisch", "SK" = "Haut",
      "SA" = "Substanzmissbrauch",
      "MC" = "Allgemeine und sonstige Beschwerden",
      "998" = "Patient vor Ersteinschätzung wieder gegangen",
      "999" = "Unbekannt"
    )

    cedis_counts <- table(cedis_cat_top, useNA = "always")
    names(cedis_counts)[length(cedis_counts)] <- "Vorstellungsgrund nicht dokumentiert"

    cedis_data_frame <- as.data.frame(cedis_counts, stringsAsFactors = FALSE)
    colnames(cedis_data_frame) <- c("Category", "Frequency")
    cedis_data_frame <- cedis_data_frame[order(-cedis_data_frame$Frequency), ]

    graph <- ggplot(data = cedis_data_frame, aes(reorder(Category, Frequency), Frequency)) +
      geom_bar(stat = "identity", fill = "#046C9A", width = 0.5) +
      labs(y = "Anzahl Patienten", x = "") +
      theme(
        plot.caption = element_text(hjust = 0.5, size = 12),
        panel.background = element_rect(fill = "white"),
        axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
        axis.text.x = element_text(face = "bold", color = "#000000", size = 12),
        axis.text.y = element_text(face = "bold", color = "#000000", size = 10)
      ) +
      coord_flip()

    report_svg(graph, "cedis_groups_test")
  },
  silent = FALSE
)

# # TOP20 ICD
# try(
#   {
#     ## Old simple Version (still necessary, table is based only on "F" data)
#     f_diag <- df_diag$diagnosis[df_diag$fuehrend == "F" & !is.na(df_diag$fuehrend)]
#     t <- table(f_diag, useNA = "no") # frequencies
#     x <- sort(t, decreasing = TRUE)
#     names(x)[is.na(names(x))] <- "NA"

#     # calculate table for stacked barchart based on Zusatzkennzeichen for all diagnoses with "F"
#     icd_stacked <- data.frame(mod_F = df_diag$diagnosis, mod_G = df_diag$diagnosis, mod_V = df_diag$diagnosis, mod_Z = df_diag$diagnosis, mod_A = df_diag$diagnosis)
#     # remove all diagnoses that are not 'F'
#     icd_stacked$mod_F[!df_diag$fuehrend == "F" | is.na(df_diag$fuehrend)] <- NA
#     icd_stacked$mod_G[!df_diag$fuehrend == "F" | is.na(df_diag$fuehrend)] <- NA
#     icd_stacked$mod_V[!df_diag$fuehrend == "F" | is.na(df_diag$fuehrend)] <- NA
#     icd_stacked$mod_Z[!df_diag$fuehrend == "F" | is.na(df_diag$fuehrend)] <- NA
#     icd_stacked$mod_A[!df_diag$fuehrend == "F" | is.na(df_diag$fuehrend)] <- NA
#     # remove all diagnoses that have the wrong modifier
#     icd_stacked$mod_G[(!df_diag$zusatz == "G") | is.na(df_diag$zusatz)] <- NA
#     icd_stacked$mod_V[(!df_diag$zusatz == "V") | is.na(df_diag$zusatz)] <- NA
#     icd_stacked$mod_Z[(!df_diag$zusatz == "Z") | is.na(df_diag$zusatz)] <- NA
#     icd_stacked$mod_A[(!df_diag$zusatz == "A") | is.na(df_diag$zusatz)] <- NA
#     # remove all diagnoses from 'mod_F' that have a modifier
#     icd_stacked$mod_F[df_diag$zusatz == "G"] <- NA
#     icd_stacked$mod_F[df_diag$zusatz == "V"] <- NA
#     icd_stacked$mod_F[df_diag$zusatz == "Z"] <- NA
#     icd_stacked$mod_F[df_diag$zusatz == "A"] <- NA
#     # lots of silly transformations to get a matrix that is plotable as a stacked barchart
#     stacktable <- data.frame(diag = names(table(icd_stacked$mod_F)), F = as.vector(table(icd_stacked$mod_F)), G = as.vector(table(icd_stacked$mod_G)), V = as.vector(table(icd_stacked$mod_V)), Z = as.vector(table(icd_stacked$mod_Z)), A = as.vector(table(icd_stacked$mod_A)))
#     diag_order <- order(table(f_diag, useNA = "always"), decreasing = TRUE)
#     stacktable <- t(stacktable[diag_order[1:20], ])
#     colnames(stacktable) <- stacktable[1, ]
#     if (!is.null(names(table(icd_stacked$mod_F)))) {
#       stacktable <- stacktable[2:6, ]
#     }
#     stacktable[is.na(stacktable)] <- 0
#     stacktable <- apply(stacktable, 2, as.numeric)
#     rownames(stacktable) <- c("F", "G", "V", "Z", "A")
#     stacktable <- t(stacktable)
#     stacktable <- stacktable[complete.cases(stacktable), ] # remove rows
#     graph <- barchart(stacktable[dim(stacktable)[1]:1, 1:5], xlab = "Anzahl Patienten", sub = "blau=Ohne Zusatzkennzeichen, grün=Gesichert, gelb=Verdacht, orange=Z.n., rot=Ausschluss", col = std_cols5[5:1], origin = 0)
#     # graph <- barchart( x [20:1], xlab="Anzahl Patienten",col=std_cols[1],origin=0)
#     report_svg(graph, "icd_top")

#     a <- t
#     a <- sort(a, decreasing = TRUE)
#     a <- a[1:20]
#     codes <- names(a)
#     # names(a) <- factor(names(a),t(icd[1]),labels=strtrim(t(icd[2]),60))
#     names(a) <- factor(names(a), t(icd[1]), labels = t(icd[2]), 61)
#     a <- a[complete.cases(a)]
#     if (length(a) == 0) {
#       codes <- a # do not output ICD-codes with no occurence in the table => make the table shorter
#     }
#     # kat <- paste(codes,": ",names(a),sep = '')
#     # b <- data.frame(Kategorie=kat, Anzahl=format_number(a), Anteil=format_number((a / sum(t))*100,digits = 1))
#     b <- data.frame(Code = codes, Kategorie = names(a), Anzahl = format_number(a), Anteil = format_number((a / length(df$encounter)) * 100, digits = 1))
#     ges <- sum(a)
#     c <- rbind(b, data.frame(Code = "---", Kategorie = "Summe TOP20", Anzahl = format_number(ges), Anteil = format_number((ges / length(df$encounter) * 100), digits = 1)))
#     d <- rbind(c, data.frame(Code = "", Kategorie = "Nicht dokumentiert", Anzahl = format_number(length(df$encounter) - length(f_diag)), Anteil = format_number(((length(df$encounter) - length(f_diag)) / length(df$encounter)) * 100, digits = 1))) # f_diag is 1 or 0 per encounter, df$enc is the number of enc. Counting NA is not enough since there may be multiple or no diagnoses per encounter
#     d[, 4] <- paste(d[, 4], "%")
#     report_table(d, name = "icd.xml", align = c("left", "left", "right", "right"), widths = c(8, 62, 15, 15))
#   },
#   silent = FALSE
# )
