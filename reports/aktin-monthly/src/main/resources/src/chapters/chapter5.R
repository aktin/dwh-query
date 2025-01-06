# File overview:
#
# phys.d.box
# phys.d.hist
# phys.d.xml
#
# triage.d.box
# triage.d.hist
# triage.d.xml
#
# triage.phys.d.avg
# triage.phy.d.avg.xml

# Notes:
# - Empty graph option for all graphs?
# - Combine graph functions of similar type across the whole project into one function?
# - How can the localisation be solved within the graphs?
# - What is done for data prep in parse_derive and what in the chapters?
#########################

# phys.d.box
# phys.d.hist
try(
  {
    df$phys.d <- as.numeric(df$phys.d) ### Here or better in Parse_derive???

    num_positive_outliers <- as.character(sum(df$phys.d >= 181, na.rm = TRUE))
    num_negative_outliers <- as.character(sum(df$phys.d <= 0, na.rm = TRUE))
    num_missing_times <- as.character(length(df$phys.d[is.na(df$phys.d)]))

    valid_times_raw <- na.omit(df$phys.d[df$phys.d < 181 & df$phys.d >= 0])
    delay_times <- data.frame(
      Time = valid_times_raw,
      Description = rep("Zeit", length(valid_times_raw))
    )

    if (nrow(delay_times) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = delay_times, aes(x = Description, y = Time)) +
        geom_boxplot(fill = "#046C9A", width = 0.5) +
        labs(
          y = "Zeit von Aufnahme bis Arztkontakt [Minuten]",
          caption = paste(
            "Fehlende Werte: ", num_missing_times,
            "; Werte > 180 Minuten: ", num_positive_outliers,
            "; Werte < 0 Minuten: ", num_negative_outliers
          )
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
          axis.title.x = element_blank(),
          axis.ticks.x = element_blank(),
          axis.text.x = element_blank()
        ) +
        scale_y_continuous(breaks = seq(0, 200, 20))
    }
    report_svg(graph, "phys.d.box")
    rm(graph)

    if (nrow(delay_times) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(delay_times, aes(x = Time)) +
        geom_histogram(
          aes(y = 100 * after_stat(count) / sum(after_stat(count))),
          bins = 12,
          color = "black",
          fill = "#046C9A", boundary = 0
        ) +
        scale_x_continuous(breaks = seq(0, 180, length = 7)) +
        labs(y = "Relative Häufigkeit [%]") +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
          axis.title.x = element_blank(),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
        )
    }
    report_svg(graph, "phys.d.hist")
    rm(graph)
  },
  silent = FALSE
)

# phys.d.xml
try(
  {
    summary <- create_delay_time_report(
      delay_times,
      num_missing_times,
      num_positive_outliers,
      num_negative_outliers,
      factors
    )

    report_table(
      summary,
      name = "phys.d.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = column_name_translations
    )
    rm(summary, delay_times)
  },
  silent = FALSE
)

# Time to triage
# triage.d.box
# triage.d.hist
try(
  {
    df$triage.d <- as.numeric(df$triage.d) ### Here or better in Parse_derive???

    num_positive_outliers <- as.character(sum(df$triage.d >= 61, na.rm = TRUE))
    num_negative_outliers <- as.character(sum(df$triage.d <= 0, na.rm = TRUE))
    num_missing_times <- as.character(length(df$triage.d[is.na(df$triage.d)]))

    valid_times_raw <- na.omit(df$triage.d[df$triage.d < 61 & df$triage.d >= 0])

    delay_times <- data.frame(
      Time = valid_times_raw,
      Description = rep("Zeit", length(valid_times_raw))
    )

    if (nrow(delay_times) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = delay_times, aes(x = Description, y = Time)) +
        geom_boxplot(fill = "#046C9A", width = 0.5) +
        geom_hline(aes(yintercept = 10, linetype = "Ersteinschätzung innerhalb 10 Minuten"), color = "red", size = 1) +
        labs(
          y = "Zeit von Aufnahme bis Triage [Minuten]",
          caption = paste(
            "Fehlende Werte: ", num_missing_times,
            "; Werte > 60 Minuten: ", num_positive_outliers,
            "; Werte < 0 Minuten: ", num_negative_outliers,
            "; Werte innerhalb 10 min: ", sum(delay_times$Time < 11)
          )
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
          axis.title.x = element_blank(),
          axis.ticks.x = element_blank(),
          axis.text.x = element_blank(),
          legend.position = "bottom", legend.title = element_blank()
        ) +
        coord_cartesian(ylim = c(0, 60))
    }
    report_svg(graph, "triage.d.box")
    rm(graph)

    if (nrow(delay_times) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(delay_times, aes(x = Time)) +
        geom_histogram(
          aes(y = 100 * after_stat(count) / sum(after_stat(count))),
          bins = 12, color = "black",
          fill = "#046C9A",
          boundary = 0
        ) +
        scale_x_continuous(breaks = seq(0, 60, length = 7)) +
        labs(y = "Relative Häufigkeit [%]") +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
          axis.title.x = element_blank(),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
        )
    }

    report_svg(graph, "triage.d.hist")
    rm(graph)
  },
  silent = FALSE
)

# triage.d.xml
try(
  {
    summary <- create_delay_time_report(
      delay_times,
      num_missing_times,
      num_positive_outliers,
      num_negative_outliers,
      factors
    )

    report_table(
      summary,
      name = "triage.d.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = column_name_translations
    )
    rm(summary, delay_times)
  },
  silent = FALSE
)

# # Time to physician mean grouped by triage result
# try(
#   {
#     df$phys.d <- as.numeric(df$phys.d)
#     a <- df$phys.d[df$phys.d < 181]
#     c <- table(a == 0)
#     c <- data.frame(c)
#     c <- c %>% filter(Var1 == TRUE)
#     c <- c$Freq
#     outofbounds <- length(df$phys.d) - length(a)
#     b <- df$triage.result[df$phys.d < 181]
#     y <- data.frame(time = a, triage = b)
#     y$time <- as.numeric(y$time)
#     y <- y %>% filter(!is.na(time))
#     graph <- ggplot(data = y, aes(x = triage, y = time, fill = triage)) +
#       geom_boxplot(show.legend = FALSE) +
#       labs(x = "Triage-Gruppe", y = "Durchschn. Zeit bis Arztkontakt [Min.]", caption = paste("Werte über 180 Minuten (unberücksichtigt): ", outofbounds, " ; Fehlender Zeitstempel Arztkontakt:", sum(is.na(df$phys.d)))) +
#       scale_fill_manual(values = c("Rot" = "red", "Orange" = "orange", "Gelb" = "yellow2", "Grün" = "green4", "Blau" = "blue", "Ohne" = "grey48")) +
#       theme(
#         plot.caption = element_text(hjust = 0.5, size = 12),
#         panel.background = element_rect(fill = "white"),
#         axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
#         axis.text.x = element_text(face = "bold", color = "#000000", size = 12),
#         axis.text.y = element_text(face = "bold", color = "#000000", size = 12)
#       ) +
#       scale_y_continuous(breaks = seq(0, max(y$time), 10), expand = c(0, 0.3))
#     report_svg(graph, "triage.phys.d.avg")
#   },
#   silent = FALSE
# )


# try(
#   {
#     y2 <- data.frame(triage = df$triage.result, time = df$phys.d)
#     y2 <- y2 %>% dplyr::filter(time < 181)
#     agg.funs <- list(Mittelwert = mean, Median = median, Minimum = min, Maximum = max)
#     agg.list <- lapply(agg.funs, function(fun) {
#       with(y2, tapply(time, list(triage), FUN = fun, na.rm = TRUE))
#     })

#     agg.list$Mittelwert <- round(agg.list$Mittelwert, 1)
#     agg.list$Median <- round(agg.list$Median, 0)
#     agg.list$Anzahl <- with(y2, tapply(time, list(triage), FUN = length))
#     agg.list$Anzahl[is.na(agg.list$Anzahl)] <- 0
#     kat <- c("Rot", "Orange", "Gelb", "Grün", "Blau", "Ohne")

#     x <- data.frame(Kategorie = kat, agg.list)
#     rm(agg.funs, agg.list)
#     report_table(x, "triage.phys.d.xml", align = c("left", "right", "right", "right", "right", "right"), width = 15)
#   },
#   silent = FALSE
# )
