# Figure 2.3
# Admission day: Average number of patients per hour for the current month
try(
  {
    frequency_table <- data.frame(table(df$admit.h) / length(levels(df$admit.day)))

    if (nrow(frequency_table) == 0 || all(is.na(frequency_table$Freq))) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = frequency_table, aes(x = Var1, y = Freq)) +
        geom_bar(
          stat = "identity",
          fill = "#00427e",
          width = 0.5
        ) +
        labs(
          x = paste(
            "Uhrzeit [Stunde]; n =",
            sum(!is.na(df$admit.h))
          ),
          y = "Durchschnittliche Anzahl Patienten"
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12),
          axis.text.y = element_text(color = "#000000", size = 12)
        ) +
        scale_y_continuous(expand = c(0, 0.01))
    }

    report_svg(graph, "admit.h")
    rm(frequency_table)
  },
  silent = FALSE
)

# Table 2.2
# Admission day: Daily percentage of patients for the current month
# (Split into the first and and second half of the month.)
try(
  {
    total_days <- length(levels(df$admit.day))

    table_formatted <- format(
      round(table(df$admit.h)[1:12] / total_days, 1),
      nsmall = 1, big.mark = "."
    )
    table_formatted[table_formatted == "NaN"] <- "-"

    report_table(data.frame(as.list(table_formatted), check.names = FALSE), name = "admit.h.xml", align = "center")

    table_formatted_2 <- format(
      round(table(df$admit.h)[13:24] / total_days, 1),
      nsmall = 1, big.mark = "."
    )
    table_formatted_2[table_formatted_2 == "NaN"] <- "-"

    report_table(data.frame(as.list(table_formatted_2), check.names = FALSE), name = "admit2.h.xml", align = "center")
  },
  silent = FALSE
)

# Figure 2.1
# Admission day: Average number of patients admitted per weekday
try(
  {
    temp_df <- df[c("admit.day", "admit.wd")]
    temp_df <- temp_df[!is.na(temp_df$admit.wd), ]
    temp_df <- temp_df[!duplicated(temp_df), ]

    week_day_counts <- table(temp_df$admit.wd)
    rm(temp_df)

    frequency_table <- round(table(df$admit.wd) / week_day_counts, digits = 1)
    frequency_table[is.na(frequency_table)] <- 0
    frequency_table <- data.frame(frequency_table)

    if (nrow(frequency_table) == 0 || all(frequency_table$Freq == 0)) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot(data = frequency_table, aes(x = Var1, y = Freq)) +
        geom_bar(
          stat = "identity",
          fill = "#00427e", width = 0.5
        ) +
        labs(
          x = paste(
            "Wochentag; n =",
            sum(!is.na(df$admit.h))
          ),
          y = "Durchschnittliche Anzahl Patienten"
        ) +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12),
          axis.text.y = element_text(color = "#000000", size = 12)
        ) +
        scale_y_continuous(expand = c(0, 0.1))
    }
    report_svg(graph, "admit.wd")
    rm(frequency_table)
  },
  silent = FALSE
)

# Figure 2.3
# Admission day: Average number of patients per hour, week vs. weekend
try(
  {
    weekday <- rep(0, 24)
    weekend <- rep(0, 24)

    for (i in 1:24) {
      weekday[i] <- sum(admit_hwd[1:5, i])
      weekend[i] <- sum(admit_hwd[6:7, i])
    }

    days_weekday <- sum(week_day_counts[1:5])
    days_weekend <- sum(week_day_counts[6:7])
    hours <- 0:23

    avg_weekday <- weekday / days_weekday
    avg_weekend <- weekend / days_weekend

    data_weekday <- data.frame(
      hours, avg_weekday
    )
    data_weekend <- data.frame(
      hours, avg_weekend
    )

    if (nrow(data_weekend) == 0 || all(data_weekend$avg_weekend == "NaN") && all(data_weekend$avg_weekday == "NaN")) {
      graph <- create_no_data_figure()
    } else {
      graph <- ggplot() +
        geom_line(data = data_weekday, aes(x = hours, y = avg_weekday), color = "#890700") +
        geom_line(data = data_weekend, aes(x = hours, y = avg_weekend), color = "#FA9B06") +
        xlab("Uhrzeit [Stunde]") +
        ylab("Durchschnittliche Fallzahl") +
        theme(
          plot.caption = element_text(hjust = 0.5, size = 12),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12),
          axis.text.y = element_text(color = "#000000", size = 12)
        ) +
        scale_x_continuous(expand = c(0, 1), breaks = seq(0, 23, 2)) +
        geom_point(
          data = data_weekday,
          aes(x = hours, y = avg_weekday), color = "#6c1023",
          fill = "#6c1023", shape = 22, size = 3
        ) +
        geom_point(
          data = data_weekend,
          aes(x = hours, y = avg_weekend), color = "#e26800",
          fill = "#e26800", shape = 24, size = 3
        )
    }
    report_svg(graph, "admit.hwd.weekend")
    rm(weekday, days_weekday, avg_weekday, data_weekday)
    rm(weekend, days_weekend, avg_weekend, data_weekend)
  },
  silent = FALSE
)

# Table 2.1
# Admit day: Date, Weekday, Count
try(
  {
    admission_days <- subset(df, !is.na(admit.day))
    formatted_dates <- format(admission_days$admit.day, format = "%d.%m.%Y", tz = "GMT")
    unique_dates <- names(table(formatted_dates))

    if (all(is.na(unique_dates))) {
      admissions_summary <- data.frame(
        Date = "-",
        Weekday = "-",
        Count = "-"
      )

      report_table(
        admissions_summary,
        name = "admit.d.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      weekdays_iso <- as.Date(unique_dates)
      weekdays_numbers <- format(weekdays_iso, format = "%u", tz = "GMT")

      weekdays_labeled <- factor(weekdays_numbers)
      levels(weekdays_labeled) <- list(
        "Montag" = "1", "Dienstag" = "2", "Mittwoch" = "3", "Donnerstag" = "4",
        "Freitag" = "5", "Samstag" = "6", "Sonntag" = "7"
      )
      admission_counts <- as.vector(table(formatted_dates))
      admissions_summary <- data.frame(
        Date = unique_dates,
        Weekday = weekdays_labeled,
        Count = admission_counts
      )
      admissions_summary <- admissions_summary[1:31, ]

      report_table(
        admissions_summary,
        name = "admit.d.xml",
        align = c("left", "right", "right"),
        widths = c(25, 15, 13),
        translations = translations
      )
    }
  },
  silent = FALSE
)
