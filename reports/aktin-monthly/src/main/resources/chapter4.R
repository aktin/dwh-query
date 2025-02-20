# Table 4.1
try(
  {
    if (length(df$triage.result) == 0) {
      data_frame <- data.frame(
        Category = "-",
        Count = "-",
        Percentage = "-"
      )

      report_table(
        data_frame,
        "triage.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      triage_res_summary <- table(df$triage.result, useNA = "ifany")

      data_frame <- data.frame(
        Category = factors$Triage[!is.na(factors$Triage)],
        Count = as.numeric(triage_res_summary),
        Percentage = as.numeric(triage_res_summary) / sum(triage_res_summary) * 100
      )

      data_frame <- rbind(
        data_frame,
        data.frame(
          Category = "sum",
          Count = sum(triage_res_summary),
          Percentage = sum(data_frame$Percentage)
        )
      )

      data_frame$Count <- format_number(data_frame$Count)
      data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

      report_table(
        data_frame,
        "triage.xml",
        align = c("left", "right", "right"),
        widths = c(20, 15, 15),
        translations = translations
      )
    }

    if (nrow(data_frame) == 0 || all(data_frame$Count == 0)) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = data.frame(triage_res_summary), aes(x = Var1, y = Freq, fill = Var1)) +
        geom_bar(stat = "identity", position = "dodge", colour = "black", show.legend = FALSE) +
        labs(x = "Ersteinschätzung", y = "Anzahl Patienten") +
        scale_fill_manual(values = aktin_colors) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12),
          axis.text.y = element_text(color = "#000000", size = 12)
        ) +
        scale_y_continuous(expand = c(0, 0.01))
    }
    report_svg(graph, "triage")
    rm(triage_res_summary, data_frame)
  },
  silent = FALSE
)
