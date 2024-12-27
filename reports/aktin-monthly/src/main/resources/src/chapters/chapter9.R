# Isolierte Patienten
try(
  {
    df$combined_iso <- factor(enc$isolation_grund) # Code ISO is redundant with Reason; CAVE: if Reverse and Reason are given, only Reverse is used
    levels(df$combined_iso) <- list("Keine Isolation" = "ISO:NEG", "Multiresistenter Keim" = "U80", "Gastroenteritis" = "A09.9", "Tuberkulose" = "A16.9", "Meningitis" = "G03.9", "Umkehrisolation" = "RISO", "Andere" = "OTH")
    df$combined_iso[df$isolation == "Keine Isolation"] <- "Keine Isolation"
    df$combined_iso[df$isolation == "Umkehrisolation"] <- "Umkehrisolation"

    isoreason_table <- table(df$combined_iso, useNA = "always")
    names(isoreason_table)[length(names(isoreason_table))] <- "Keine Daten"
    b <- data.frame(Kategorie = names(isoreason_table), Anzahl = format_number(isoreason_table), Anteil = format_number((isoreason_table / sum(isoreason_table)) * 100, digits = 1))
    c <- rbind(b, data.frame(Kategorie = "Summe", Anzahl = format_number(sum(isoreason_table)), Anteil = format_number(100, digits = 1)))
    c[, 3] <- paste(c[, 3], "%")
    report_table(c, name = "isoreason.xml", align = c("left", "right", "right"), widths = c(30, 15, 15))
  },
  silent = FALSE
)

# Multiresistente Erreger
try(
  {
    mrsa_patient <- cbind(df$keime, df$keime_mrsa, df$keime_3mrgn, df$keime_4mrgn, df$keime_vre)
    mrsa_patient <- rowSums(mrsa_patient, na.rm = TRUE)
    mrsa_table <- data.frame(Kategorie = "MRSA", Bekannt = sum(df$keime_mrsa == "MRSA", na.rm = TRUE), Verdacht = sum(df$keime_mrsa == "MRSA:SUSP", na.rm = TRUE))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "3-MRGN", Bekannt = sum(df$keime_3mrgn == "3MRGN", na.rm = TRUE), Verdacht = sum(df$keime_3mrgn == "3MRGN:SUSP", na.rm = TRUE)))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "4-MRGN", Bekannt = sum(df$keime_4mrgn == "4MRGN", na.rm = TRUE), Verdacht = sum(df$keime_4mrgn == "4MRGN:SUSP", na.rm = TRUE)))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "VRE", Bekannt = sum(df$keime_vre == "VRE", na.rm = TRUE), Verdacht = sum(df$keime_vre == "VRE:SUSP", na.rm = TRUE)))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "Andere", Bekannt = sum(df$keime_andere == "OTH", na.rm = TRUE), Verdacht = sum(df$keime_andere == "OTH:SUSP", na.rm = TRUE)))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "Keine Keime", Bekannt = sum(df$keime == "AMRO:NEG", na.rm = TRUE), Verdacht = "-"))
    mrsa_table <- rbind(mrsa_table, data.frame(Kategorie = "Keine Angabe", Bekannt = sum(mrsa_patient < 1), Verdacht = "-"))
    report_table(mrsa_table, name = "multiresistant.xml", align = c("left", "right", "right"), widths = c(30, 15, 15))
  },
  silent = FALSE
)

# Patienten in der NA
# Überfüllung
try(
  {
    a <- data.frame(
      admit.ts = as.POSIXct(df$admit.ts),
      discharge.ts = as.POSIXct(df$discharge.ts)
    )
    a <- na.omit(a)
    a <- a[order(as.Date(a$admit.ts)), ]

    a$in_na <- sapply(a$admit.ts, function(enter) {
      sum(a$admit.ts <= enter & a$discharge.ts > enter)
    })

    init_last_month <- as.Date(a$admit.ts[1])
    end_last_month <- as.Date(a$admit.ts[nrow(a)])

    a$Uhrzeit <- as.numeric(format(a$admit.ts, "%H")) * 3600 +
      as.numeric(format(a$admit.ts, "%M")) * 60
    a$tag <- as.Date(a$admit.ts)
    a$tag_g <- format(a$admit.ts, format = "%a") # Weekday
    a$year <- as.numeric(format(a$admit.ts, "%Y"))
    a$day <- as.numeric(format(a$admit.ts, "%d"))

    a$woche <- findInterval(a$tag, seq(init_last_month, end_last_month, by = "week"))

    time_labels <- sprintf("%02d:00", seq(0, 24, by = 2))

    fehlend <- data.frame(
      tag = rep(seq(init_last_month, end_last_month, by = "day"), each = 2),
      Uhrzeit = c(0, 86400)
    )
    a <- merge(a, fehlend, by = c("tag", "Uhrzeit"), all = TRUE)

    na_locf <- function(vec) {
      for (i in seq_along(vec)) {
        if (is.na(vec[i]) && i > 1) {
          vec[i] <- vec[i - 1]
        }
      }
      return(vec)
    }

    for (col in c("tag_g", "day", "woche")) {
      a[[col]] <- na_locf(a[[col]])
    }

    a <- a[-nrow(a), ]
    a$tag <- format(as.Date(a$tag), "%m-%d")

    weekly_titles <- sapply(1:5, function(i) {
      start <- init_last_month + (i - 1) * 7
      end <- pmin(init_last_month + i * 7 - 1, end_last_month)
      paste0("Woche ", i, " ", start, " - ", end)
    })

    plot_week <- function(week_num, title) {
      a_week <- subset(a, woche == week_num)
      a_week <- subset(a_week, !is.na(in_na))

      ggplot(data = a_week, aes(x = Uhrzeit, y = in_na, fill = tag, group = 1)) +
        geom_line() +
        facet_grid(tag + tag_g ~ .) +
        theme_bw() +
        xlab("Uhrzeit") +
        ylab("Anzahl Patienten") +
        scale_x_continuous(
          breaks = seq(0, 86400, by = 7200),
          expand = c(0, 0),
          labels = time_labels
        ) +
        coord_cartesian(xlim = c(0, 86400)) +
        ggtitle(title)
    }

    for (i in 1:5) {
      graph <- plot_week(i, weekly_titles[i])
      report_svg(graph, paste0("stayweek", i))
    }
  },
  silent = FALSE
)
