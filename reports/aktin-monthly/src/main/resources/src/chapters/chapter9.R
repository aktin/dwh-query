# Isolated patients
try(
  {
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

    ## Very similar to chapter1 - Patient Sex - Function?
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
      translations = column_name_translations
    )
    rm(isoreason_summary, iso_table)
  },
  silent = FALSE
)

# Multidrug-resistant organisms
try(
  {
    mrsa_patient <- cbind(df$keime, df$keime_mrsa, df$keime_3mrgn, df$keime_4mrgn, df$keime_vre)
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
      name = "multiresistant_test.xml",
      align = c("left", "right", "right"),
      widths = c(30, 15, 15),
      translations = column_name_translations
    )
  },
  silent = FALSE
)

# # Patienten in der NA
# # Überfüllung
# try(
#   {
#     a <- data.frame(
#       admit.ts = as.POSIXct(df$admit.ts),
#       discharge.ts = as.POSIXct(df$discharge.ts)
#     )
#     a <- na.omit(a)
#     a <- a[order(as.Date(a$admit.ts)), ]

#     a$in_na <- sapply(a$admit.ts, function(enter) {
#       sum(a$admit.ts <= enter & a$discharge.ts > enter)
#     })

#     init_last_month <- as.Date(a$admit.ts[1])
#     end_last_month <- as.Date(a$admit.ts[nrow(a)])

#     a$Uhrzeit <- as.numeric(format(a$admit.ts, "%H")) * 3600 +
#       as.numeric(format(a$admit.ts, "%M")) * 60
#     a$tag <- as.Date(a$admit.ts)
#     a$tag_g <- format(a$admit.ts, format = "%a") # Weekday
#     a$year <- as.numeric(format(a$admit.ts, "%Y"))
#     a$day <- as.numeric(format(a$admit.ts, "%d"))

#     a$woche <- findInterval(a$tag, seq(init_last_month, end_last_month, by = "week"))

#     time_labels <- sprintf("%02d:00", seq(0, 24, by = 2))

#     fehlend <- data.frame(
#       tag = rep(seq(init_last_month, end_last_month, by = "day"), each = 2),
#       Uhrzeit = c(0, 86400)
#     )
#     a <- merge(a, fehlend, by = c("tag", "Uhrzeit"), all = TRUE)

#     na_locf <- function(vec) {
#       for (i in seq_along(vec)) {
#         if (is.na(vec[i]) && i > 1) {
#           vec[i] <- vec[i - 1]
#         }
#       }
#       return(vec)
#     }

#     for (col in c("tag_g", "day", "woche")) {
#       a[[col]] <- na_locf(a[[col]])
#     }

#     a <- a[-nrow(a), ]
#     a$tag <- format(as.Date(a$tag), "%m-%d")

#     weekly_titles <- sapply(1:5, function(i) {
#       start <- init_last_month + (i - 1) * 7
#       end <- pmin(init_last_month + i * 7 - 1, end_last_month)
#       paste0("Woche ", i, " ", start, " - ", end)
#     })

#     plot_week <- function(week_num, title) {
#       a_week <- subset(a, woche == week_num)
#       a_week <- subset(a_week, !is.na(in_na))

#       ggplot(data = a_week, aes(x = Uhrzeit, y = in_na, fill = tag, group = 1)) +
#         geom_line() +
#         facet_grid(tag + tag_g ~ .) +
#         theme_bw() +
#         xlab("Uhrzeit") +
#         ylab("Anzahl Patienten") +
#         scale_x_continuous(
#           breaks = seq(0, 86400, by = 7200),
#           expand = c(0, 0),
#           labels = time_labels
#         ) +
#         coord_cartesian(xlim = c(0, 86400)) +
#         ggtitle(title)
#     }

#     for (i in 1:5) {
#       graph <- plot_week(i, weekly_titles[i])
#       report_svg(graph, paste0("stayweek", i))
#     }
#   },
#   silent = FALSE
# )
