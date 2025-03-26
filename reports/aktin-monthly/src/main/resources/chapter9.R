# Isolated patients
try(
  {
    if (length(enc$isolation_grund) == 0) {
      iso_table <- data.frame(
        Category = "-",
        Count = "-",
        Percentage = "-"
      )

      report_table(
        iso_table,
        name = "isoreason.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      df$combined_iso <- factor(enc$isolation_grund)
      levels(df$combined_iso) <- list(
        "Keine Isolation" = "ISO:NEG",
        "Multiresistenter Keim" = "U80",
        "Gastroenteritis" = "A09.9",
        "Tuberkulose" = "A16.9",
        "Meningitis" = "G03.9",
        "Umkehrisolation" = "RISO",
        "Andere" = "OTH"
      )

      df$combined_iso[df$isolation == "Keine Isolation"] <- "Keine Isolation"
      df$combined_iso[df$isolation == "Umkehrisolation"] <- "Umkehrisolation"

      isoreason_summary <- table(df$combined_iso, useNA = "always")
      names(isoreason_summary)[is.na(names(isoreason_summary))] <- "Keine Daten"


      iso_table <- data.frame(
        Category = names(isoreason_summary),
        Count = as.numeric(isoreason_summary),
        Percentage = as.numeric(isoreason_summary) / sum(isoreason_summary) * 100
      )

      iso_table <- rbind(
        iso_table,
        data.frame(
          Category = "Summe",
          Count = sum(isoreason_summary),
          Percentage = 100
        )
      )

      iso_table$Count <- format_number(iso_table$Count)
      iso_table$Percentage <- paste(format_number(iso_table$Percentage, digits = 1), "%")

      report_table(
        iso_table,
        name = "isoreason.xml",
        align = c("left", "right", "right"),
        widths = c(30, 15, 15),
        translations = translations
      )
    }
    rm(isoreason_summary, iso_table)
  },
  silent = FALSE
)

# Multidrug-resistant organisms
try(
  {
    mrsa_patient <- cbind(df$keime, df$keime_mrsa, df$keime_3mrgn, df$keime_4mrgn, df$keime_vre)

    if (nrow(mrsa_patient) == 0) {
      mrsa_table <- data.frame(
        Category = "-",
        Known = "-",
        Suspected = "-"
      )

      report_table(
        mrsa_table,
        name = "multiresistant.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      mrsa_patient <- rowSums(mrsa_patient, na.rm = TRUE)

      mrsa_table <- data.frame(
        Category = "MRSA",
        Known = sum(df$keime_mrsa == "MRSA", na.rm = TRUE),
        Suspected = sum(df$keime_mrsa == "MRSA:SUSP", na.rm = TRUE)
      )

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "3-MRGN",
        Known = sum(df$keime_3mrgn == "3MRGN", na.rm = TRUE),
        Suspected = sum(df$keime_3mrgn == "3MRGN:SUSP", na.rm = TRUE)
      ))

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "4-MRGN",
        Known = sum(df$keime_4mrgn == "4MRGN", na.rm = TRUE),
        Suspected = sum(df$keime_4mrgn == "4MRGN:SUSP", na.rm = TRUE)
      ))

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "VRE",
        Known = sum(df$keime_vre == "VRE", na.rm = TRUE),
        Suspected = sum(df$keime_vre == "VRE:SUSP", na.rm = TRUE)
      ))

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "Andere",
        Known = sum(df$keime_andere == "OTH", na.rm = TRUE),
        Suspected = sum(df$keime_andere == "OTH:SUSP", na.rm = TRUE)
      ))

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "Keine Keime",
        Known = sum(df$keime == "AMRO:NEG", na.rm = TRUE),
        Suspected = "-"
      ))

      mrsa_table <- rbind(mrsa_table, data.frame(
        Category = "Keine Angabe",
        Known = sum(mrsa_patient < 1),
        Suspected = "-"
      ))

      report_table(
        mrsa_table,
        name = "multiresistant.xml",
        align = c("left", "right", "right"),
        widths = c(30, 15, 15),
        translations = translations
      )
    }
  },
  silent = FALSE
)

# Patients in emergency room over time
try(
  {
    data_frame <- data.frame(
      admit.ts = as.POSIXct(df$admit.ts),
      discharge.ts = as.POSIXct(df$discharge.ts)
    )

    data_frame <- na.omit(data_frame)
    data_frame <- data_frame[order(as.Date(data_frame$admit.ts)), ]

    data_frame$Patients_in_emergency_room <- sapply(data_frame$admit.ts, function(enter) {
      sum(data_frame$admit.ts <= enter & data_frame$discharge.ts > enter)
    })

    init_last_month <- as.Date(data_frame$admit.ts[1])
    end_last_month <- as.Date(data_frame$admit.ts[nrow(data_frame)])

    data_frame$Day_complete <- as.Date(data_frame$admit.ts)
    data_frame$Time <- as.numeric(format(data_frame$admit.ts, "%H")) * 3600 +
      as.numeric(format(data_frame$admit.ts, "%M")) * 60

    if (!is.na(init_last_month) && !is.na(end_last_month)) {
      fill_time <- data.frame(
        Day_complete = rep(seq(init_last_month, end_last_month, by = "day"), each = 2),
        Time = c(0, 86400)
      )
      data_frame <- merge(data_frame, fill_time, by = c("Day_complete", "Time"), all = TRUE)

      data_frame$Week_day <- format(data_frame$Day_complete, format = "%a")
      data_frame$Week <- findInterval(
        as.Date(data_frame$Day_complete),
        seq(init_last_month,
          end_last_month,
          by = "week"
        )
      )
      for (col in c("Patients_in_emergency_room", "Week_day")) {
        data_frame[col] <- fill_na_locf(data_frame[[col]])
      }
      data_frame <- data_frame[!is.na(data_frame$Patients_in_emergency_room), ]
    } else {
      data_frame$Week_day
      data_frame$Week
    }


    weekly_titles <- sapply(1:5, function(i) {
      start <- init_last_month + (i - 1) * 7
      end <- pmin(init_last_month + i * 7 - 1, end_last_month)
      paste0("Woche ", i, " ", start, " - ", end)
    })


    plot_week <- function(week_num, title, data_frame) {
      if (nrow(data_frame) == 0) {
        graph <- create_no_data_figure()
        return(graph)
      }
      data_frame_week <- subset(data_frame, Week == week_num)
      data_frame_week$Day_complete <- format(as.Date(data_frame_week$Day_complete), "%m-%d")

      graph <- ggplot(data = data_frame_week, aes(x = Time, y = Patients_in_emergency_room, group = Day_complete)) +
        geom_line() +
        facet_grid(Day_complete + Week_day ~ .) +
        theme_bw() +
        xlab("Uhrzeit") +
        ylab("Anzahl Patienten") +
        scale_x_continuous(
          breaks = seq(0, 86400, by = 7200),
          expand = c(0, 0),
          labels = sprintf("%02d:00", seq(0, 24, by = 2))
        ) +
        scale_y_continuous(
          breaks = scales::breaks_extended(n = 4)
        ) +
        coord_cartesian(xlim = c(0, 86400)) +
        ggtitle(title) +
        theme(
          text = element_text(size = 10),
          axis.title = element_text(size = 10),
          axis.text = element_text(size = 10),
          strip.text = element_text(size = 10),
          legend.text = element_text(size = 12),
          legend.title = element_text(size = 12),
          plot.title = element_text(size = 12)
        )

      return(graph)
    }

    file_names <- c("stayone", "staytwo", "staythree", "stayfour", "stayfive")

    for (i in 1:5) {
      graph <- plot_week(i, weekly_titles[i], data_frame)
      report_svg(graph, file_names[i])
    }
  },
  silent = FALSE
)
