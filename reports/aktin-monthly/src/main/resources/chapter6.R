# Discharge
try(
  {
    discharge_table <- table(df$discharge, useNA = "always")[c(14, 7, 1, 2, 3, 6, 5, 4, 11, 9, 13, 10, 8, 12)]
    names(discharge_table)[1] <- "Keine Daten"
    discharge_table <- data.frame(discharge_table)

    if (nrow(discharge_table) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = discharge_table, aes(x = Var1, y = Freq)) +
        geom_bar(stat = "identity", fill = "#046C9A", width = 0.5) +
        labs(y = "Anzahl Patienten", x = "") +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
          axis.text.x = element_text(face = "bold", color = "#000000", size = 12)
        ) +
        coord_flip()
    }
    report_svg(graph, "discharge")
  },
  silent = FALSE
)

## Very similar to chapter1 - Patient Sex - Function?
try(
  {
    discharge_summary <- table(df$discharge, useNA = "always")[c(12, 8, 10, 13, 9, 11, 4, 5, 6, 3, 2, 1, 7, 14)]
    names(discharge_summary)[is.na(names(discharge_summary))] <- "Keine Daten"

    data_frame <- data.frame(
      Category = names(discharge_summary),
      Count = as.numeric(discharge_summary),
      Percentage = as.numeric(discharge_summary) / sum(discharge_summary) * 100
    )

    data_frame <- rbind(
      data_frame,
      data.frame(
        Category = "Summe",
        Count = sum(discharge_summary),
        Percentage = sum(data_frame$Percentage)
      )
    )

    data_frame$Count <- format_number(data_frame$Count)
    data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

    report_table(
      data_frame,
      name = "discharge.xml",
      align = c("left", "right", "right"),
      widths = c(25, 15, 15),
      translations = column_name_translations
    )
    rm(discharge_summary, data_frame)
  },
  silent = FALSE
)
