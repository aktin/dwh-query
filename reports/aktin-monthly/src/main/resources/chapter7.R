# Length of stay (los)
# los.xml
try(
  {
    df$los <- difftime(df$discharge.ts, df$admit.ts, units = "mins")
    valid_los <- na.omit(df$los[df$los > 0 & df$los <= 24 * 60])

    los_times <- data.frame(
      Time = valid_los,
      Description = rep("Zeit", length(valid_los))
    )

    summary <- create_delay_time_report(
      delay_times = los_times,
      num_missing_times = sum(is.na(df$los)),
      num_negative_outliers = sum(df$los < 0, na.rm = TRUE),
      num_positive_outliers = sum(df$los > 24 * 60, na.rm = TRUE),
      factors = factors$los_txt
    )

    report_table(
      summary,
      name = "los.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = translations
    )
    rm(summary, los_times)
  },
  silent = FALSE
)

# Time to discharge
# discharge.d.box.svg
# discharge.d.hist.svg
try(
  {
    valid_discharge <- na.omit(df$discharge.d[df$discharge.d < 600])
    num_times_above_threshold <- sum(df$discharge.d >= 600, na.rm = TRUE)
    num_positive_outliers <- sum(df$discharge.d >= 24 * 60, na.rm = TRUE)
    num_negative_outliers <- sum(df$discharge.d <= 0, na.rm = TRUE)
    num_missing_times <- sum(is.na(df$discharge.d))

    discharge_times <- data.frame(
      Time = as.numeric(valid_discharge),
      Description = rep("Zeit", length(valid_discharge))
    )

    if (nrow(discharge_times) == 0 || all(discharge_times$Time == 0)) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = discharge_times, aes(x = Description, y = Time)) +
        geom_boxplot(fill = "#00427e", width = 0.5) +
        labs(
          y = "Zeit von Aufnahme bis zur Entlassung/ \n Verlegung [Minuten]",
          caption = paste(
            "Fehlende Werte: ", num_missing_times,
            "; Werte < 0h: ", num_negative_outliers,
            "; Werte > 24h:", num_positive_outliers,
            "; Werte über 600 Minuten: ", num_times_above_threshold
          )
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(color = "#000000", size = 12),
          axis.title.x = element_blank(),
          axis.ticks.x = element_blank(),
          axis.text.x = element_blank(),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(size = 12)
        ) +
        scale_y_continuous(breaks = seq(0, 600, 40), expand = c(0, 0.1)) +
        geom_hline(aes(yintercept = mean(Time), linetype = "Ersteinschätzung innerhalb 10 Minuten"), color = "#e3000f", linewidth = 1)
    }
    report_svg(graph, "discharge.d.box")
    rm(graph)

    discharge_times <- data.frame(
      Time = cut(
        as.numeric(valid_discharge),
        breaks = seq(0, 600, by = 60),
        include.lowest = TRUE,
        right = FALSE
      )
    )

    if (nrow(discharge_times) == 0 || all(is.na(discharge_times$Time))) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(discharge_times, aes(x = Time)) +
        geom_bar(
          aes(y = 100 * after_stat(count) / sum(after_stat(count))),
          color = "black",
          fill = "#00427e",
          stat = "count"
        ) +
        labs(
          x = "Zeit von Aufnahme bis zur Entlassung/Verlegung [Minuten]",
          y = "Relative Häufigkeit [%]"
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(color = "#000000", size = 12),
          axis.text.x = element_text(color = "#000000", size = 12, angle = 45, hjust = 1),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(color = "#e3000f", size = 12, face = "bold")
        ) +
        scale_y_continuous(expand = c(0, 0.1)) +
        scale_x_discrete(labels = function(x) paste0(x))
    }
    report_svg(graph, "discharge.d.hist")
    rm(graph)
  },
  silent = FALSE
)
