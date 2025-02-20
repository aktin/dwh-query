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
#########################

# phys.d.box
# phys.d.hist
try(
  {
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
        geom_boxplot(fill = "#00427e", width = 0.5) +
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
          axis.text.y = element_text(color = "#000000", size = 12),
          axis.title.x = element_blank(),
          axis.ticks.x = element_blank(),
          axis.text.x = element_blank()
        ) +
        scale_y_continuous(breaks = seq(0, 200, 20), expand = c(0, 0.1))
    }
    report_svg(graph, "phys.d.box")

    rm(graph)


    delay_times_intervals <- data.frame(
      Time = cut(
        valid_times_raw,
        breaks = seq(0, 180, by = 15),
        include.lowest = TRUE,
        right = FALSE
      )
    )

    if (nrow(delay_times_intervals) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(delay_times_intervals, aes(x = Time)) +
        geom_bar(
          aes(y = 100 * after_stat(count) / sum(after_stat(count))),
          color = "black",
          fill = "#00427e",
          stat = "count"
        ) +
        labs(
          x = "Zeit von Aufnahme bis Arztkontakt [Minuten]",
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
          legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
        ) +
        scale_y_continuous(expand = c(0, 0.1)) +
        scale_x_discrete(labels = function(x) paste0(x))
    }
    report_svg(graph, "phys.d.hist")
    rm(graph, delay_times_intervals)
  },
  silent = FALSE
)

# phys.d.xml
try(
  {
    summary_table <- create_delay_time_report(
      delay_times,
      num_missing_times,
      num_positive_outliers,
      num_negative_outliers,
      factors$phys_txt
    )

    report_table(
      summary_table,
      name = "phys.d.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = translations
    )
    rm(summary_table, delay_times)
  },
  silent = FALSE
)

# Time to triage
# triage.d.box
# triage.d.hist
try(
  {
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
        geom_boxplot(fill = "#00427e", width = 0.5) +
        geom_hline(aes(yintercept = 10, linetype = "Ersteinschätzung innerhalb 10 Minuten"), color = "#e3000f", linewidth = 1) +
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
          axis.text.y = element_text(color = "#000000", size = 12),
          axis.title.x = element_blank(),
          axis.ticks.x = element_blank(),
          axis.text.x = element_blank(),
          legend.position = "bottom", legend.title = element_blank(),
          legend.text = element_text(size = 12)
        ) +
        coord_cartesian(ylim = c(0, 60)) +
        scale_y_continuous(expand = c(0, 0.1))
    }
    report_svg(graph, "triage.d.box")
    rm(graph)

    delay_times_intervals <- data.frame(
      Time = cut(
        valid_times_raw,
        breaks = seq(0, 60, by = 5),
        include.lowest = TRUE,
        right = FALSE
      )
    )

    if (nrow(delay_times_intervals) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(delay_times_intervals, aes(x = Time)) +
        geom_bar(
          aes(y = 100 * after_stat(count) / sum(after_stat(count))),
          color = "black",
          fill = "#00427e",
          stat = "count"
        ) +
        labs(
          x = "Zeit von Aufnahme bis Triage [Minuten]",
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
          legend.position = "bottom",
          legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
        ) +
        scale_y_continuous(expand = c(0, 0.1)) +
        scale_x_discrete(labels = function(x) paste0(x))
    }

    report_svg(graph, "triage.d.hist")
    rm(graph, delay_times_intervals)
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
      factors$triage_txt
    )

    report_table(
      summary,
      name = "triage.d.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = translations
    )
    rm(summary, delay_times)
  },
  silent = FALSE
)

# triage.phys.d.avg
try(
  {
    outliers <- sum(df$phys.d >= 181, na.rm = TRUE)
    triage_by_phys <- data.frame(
      Time = na.omit(df$phys.d[df$phys.d < 181]),
      Triage = df$triage.result[df$phys.d < 181 & !is.na(df$phys.d)]
    )

    if (nrow(triage_by_phys) == 0) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = triage_by_phys, aes(x = Triage, y = Time, fill = Triage)) +
        geom_boxplot(show.legend = FALSE) +
        labs(
          x = "Triage-Gruppe",
          y = "Durchschn. Zeit bis Arztkontakt [Min.]",
          caption = paste(
            "Werte über 180 Minuten (unberücksichtigt): ", outliers,
            " ; Fehlender Zeitstempel Arztkontakt:", sum(is.na(df$phys.d))
          )
        ) +
        scale_fill_manual(
          values = aktin_colors
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12),
          axis.text.y = element_text(color = "#000000", size = 12)
        ) +
        scale_y_continuous(breaks = seq(0, max(triage_by_phys$Time), 10), expand = c(0, 0.1))
    }
    report_svg(graph, "triage.phys.d.avg")
    rm(graph)
  },
  silent = FALSE
)

# triage.phy.d.avg.xml
try(
  {
    metrics_funs <- list(
      Mean = function(x) round(mean(x, na.rm = TRUE), 1),
      Median = function(x) round(median(x, na.rm = TRUE), 0),
      Min = function(x) min(x, na.rm = TRUE),
      Max = function(x) max(x, na.rm = TRUE)
    )

    compute_metrics <- function(funs, data, group_by_col, value_col) {
      lapply(funs, function(fun) {
        result <- tapply(data[[value_col]], data[[group_by_col]], FUN = fun)
        result[is.na(result)] <- 0
        return(result)
      })
    }

    metrics_list <- compute_metrics(
      metrics_funs,
      triage_by_phys,
      group_by_col = "Triage",
      value_col = "Time"
    )

    metrics_list$Count <- tapply(triage_by_phys$Time, triage_by_phys$Triage, FUN = length)
    metrics_list$Count[is.na(metrics_list$Count)] <- 0

    metrics_list$Category <- c("Red", "Orange", "Yellow", "Green", "Blue", "Ohne")

    metrics_frame <- as.data.frame(metrics_list)
    metrics_frame <- metrics_frame[, c("Category", setdiff(names(metrics_frame), "Category"))]

    report_table(
      metrics_frame,
      name = "triage.phys.d.xml",
      align = c("left", "right", "right", "right", "right", "right"),
      width = 15,
      translations = translations
    )
  },
  silent = FALSE
)
