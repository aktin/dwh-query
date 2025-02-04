# TOP20 CEDIS
try(
  {
    df_cedis <- data.frame(df$cedis)
    df_cedis[is.na(df_cedis)] <- "999"

    level_counts <- as.data.frame(table(df_cedis$df.cedis))
    names(level_counts) <- c("Cedis_codes", "Count")
    top_counts <- level_counts[order(-level_counts$Count), ][1:20, ]
    top_counts[top_counts$Count == 0, ] <- NA


    if (nrow(top_counts) == 0 || all(is.na(top_counts$Count))) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = top_counts, aes(reorder(Cedis_codes, Count), Count)) +
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
    }
    report_svg(graph, "cedis_top")
    rm(graph)


    xml_top_counts <- level_counts[order(-level_counts$Count), ]
    missing_cedis <- xml_top_counts[xml_top_counts$Cedis_codes == "999", , drop = FALSE]

    xml_top_counts <- xml_top_counts[xml_top_counts$Cedis_codes != "999", , drop = FALSE]
    xml_top_counts <- xml_top_counts[1:20, ]
    xml_top_counts[xml_top_counts$Count == 0, ] <- NA

    if (all(is.na(xml_top_counts$Count))) {
      cedis_summary <- data.frame(
        Code = "-",
        Category = "-",
        Count = "-",
        Percentage = "-"
      )

      report_table(
        cedis_summary,
        name = "cedis.xml",
        align = c("center", "center", "center", "center"),
        translations = column_name_translations
      )
    } else {
      cedis_summary <- data.frame(
        Code = as.character(xml_top_counts$Cedis_codes),
        Category = factor(xml_top_counts$Cedis_codes, levels = cedis[[1]], labels = cedis[[3]]),
        Count = as.numeric(xml_top_counts$Count),
        Percentage = (xml_top_counts$Count / length(df$encounter)) * 100
      )

      cedis_summary <- rbind(
        cedis_summary,
        data.frame(
          Code = "---",
          Category = "Summe TOP20",
          Count = sum(xml_top_counts$Count),
          Percentage = sum(cedis_summary$Percentage, na.rm = TRUE)
        )
      )

      cedis_summary <- rbind(
        cedis_summary,
        data.frame(
          Code = "999",
          Category = "Unbekannt",
          Count = missing_cedis$Count,
          Percentage = (missing_cedis$Count / length(df$encounter)) * 100
        )
      )

      cedis_summary$Count <- format_number(cedis_summary$Count)
      cedis_summary$Percentage <- paste(format_number(cedis_summary$Percentage, digits = 1), "%")

      report_table(
        cedis_summary,
        name = "cedis.xml",
        align = c("left", "left", "right", "right"),
        widths = c(8, 60, 15, 15),
        translations = column_name_translations
      )
    }
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

    if (nrow(cedis_data_frame) == 0 || all(cedis_data_frame$Frequency == 0)) {
      graph <- create_no_data_figure()
    } else {
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
    }

    report_svg(graph, "cedis_groups")
    rm(graph)
  },
  silent = FALSE
)

# TOP20 ICD
try(
  {
    ## Data Prep
    # Isolate all diagnoses with the fuehrend "F" and not NA.
    # ATTENTION: In this part df_diag$diagnosis is needed as factor.
    diag_factor <- df_diag$diagnosis[df_diag$fuehrend == "F" & !is.na(df_diag$fuehrend)]
    numeric_diag <- as.numeric(diag_factor)
    lookup_table <- levels(diag_factor)
    top_diag <- sort(table(numeric_diag, useNA = "no"), decreasing = TRUE)[1:20]

    # Define modifiers (replacing NA with "Ohne")
    modifiers <- unique(na.omit(c("Ohne", df_diag$zusatz)))
    if (all(modifiers == "Ohne")) {
      graph <- create_no_data_figure()
    } else {
      # Initialize an empty data matrix to store results
      data_matrix <- data.frame(matrix(0, nrow = length(top_diag), ncol = length(modifiers)))
      colnames(data_matrix) <- modifiers
      rownames(data_matrix) <- lookup_table[as.numeric(names(top_diag))]

      # Filter the df_diag to include just F and no NA.
      # ATTENTION: In this part df_diag$diagnosis is needed as data frame.
      # The frame itself is modified!
      df_diag[df_diag$fuehrend == "F" & !is.na(df_diag$fuehrend), ]
      # Filter rows that have the top20 diagnoses as entry.
      rest_diag <- df_diag[as.numeric(df_diag$diagnosis) %in% as.numeric(names(top_diag)), ]
      # Fill matrix with values
      for (current_diag in as.numeric(names(top_diag))) {
        frame <- rest_diag[as.numeric(rest_diag$diagnosis) == current_diag, ]
        diag_name <- lookup_table[current_diag]

        for (modifier in modifiers) {
          if (modifier == "Ohne") {
            count <- sum(is.na(frame$zusatz) | frame$zusatz != modifier)
          } else {
            count <- sum(frame$zusatz == modifier, na.rm = TRUE)
          }
          data_matrix[diag_name, modifier] <- count
        }
      }

      data_matrix <- data_matrix[rev(rownames(data_matrix)), ]

      ## Graph
      modifier_colors <- c(
        "Ohne" = "dodgerblue",
        "G" = "forestgreen",
        "V" = "yellow",
        "Z.n." = "orange",
        "A" = "firebrick3"
      )

      graph <- barchart(
        as.matrix(data_matrix),
        xlab = "Anzahl Patienten",
        sub = "blau=Ohne Zusatzkennzeichen, grün=Gesichert, gelb=Verdacht, orange=Z.n., rot=Ausschluss",
        col = modifier_colors[colnames(data_matrix)],
        origin = 0
      )
    }

    report_svg(graph, "icd_top")
    rm(graph)


    ## XML table

    # Get the top diagnoses as codes
    codes <- lookup_table[as.numeric(names(top_diag))]

    if (length(codes) == 0) {
      diag_summary <- data.frame(
        Code = "-",
        Category = "-",
        Count = "-",
        Percentage = "-"
      )

      report_table(
        diag_summary,
        name = "icd.xml",
        align = c("center", "center", "center", "center"),
        translations = column_name_translations
      )
    } else {
      diag_summary <- data.frame(
        Code = codes,
        Category = icd$V2[match(codes, icd$V1)], # Map codes to categories from generate_report.R.
        Count = as.numeric(top_diag),
        Percentage = as.numeric(top_diag) / length(df$encounter) * 100
      )

      diag_summary <- rbind(
        diag_summary,
        data.frame(
          Code = "---",
          Category = "Summe TOP20",
          Count = sum(top_diag),
          Percentage = sum(as.numeric(top_diag)) / length(df$encounter) * 100
        ),
        data.frame(
          Code = "",
          Category = "Nicht dokumentiert",
          Count = length(df$encounter) - length(diag_factor),
          Percentage = (length(df$encounter) - length(diag_factor)) / length(df$encounter) * 100
        )
      )

      diag_summary$Count <- format_number(diag_summary$Count)
      diag_summary$Percentage <- paste(format_number(diag_summary$Percentage, digits = 1), "%")

      report_table(
        diag_summary,
        name = "icd.xml",
        align = c("left", "left", "right", "right"),
        widths = c(8, 62, 15, 15),
        translations = column_name_translations
      )
    }
    rm(diag_summary)
  },
  silent = FALSE
)
