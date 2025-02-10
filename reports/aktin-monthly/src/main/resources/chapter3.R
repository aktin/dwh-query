# Table 3.1 and Table 3.2 - Referral and transport
try(
  {
    if (length(df$transport) == 0) {
      data_frame <-
        data.frame(
          Category = "_",
          Count = "_",
          Percentage = "_"
        )

      report_table(
        data_frame,
        name = "transport.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      transport_summary <- table(df$transport, useNA = "always")
      names(transport_summary)[is.na(names(transport_summary))] <- "Keine Daten"

      data_frame <- data.frame(
        Category = names(transport_summary),
        Count = as.numeric(transport_summary),
        Percentage = as.numeric(transport_summary) / sum(transport_summary) * 100
      )

      data_frame <- rbind(
        data_frame,
        data.frame(
          Category = "Summe",
          Count = sum(transport_summary),
          Percentage = sum(data_frame$Percentage)
        )
      )

      data_frame$Count <- format_number(data_frame$Count)
      data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

      report_table(
        data_frame,
        name = "transport.xml",
        align = c("left", "right", "right"),
        widths = c(25, 15, 15),
        translations = translations
      )
    }

    rm(transport_summary, data_frame)

    if (length(df$referral) == 0) {
      data_frame <- data.frame(
        Category = "-",
        Count = "-",
        Percentage = "-"
      )


      report_table(
        data_frame,
        name = "referral.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      referral_summary <- table(df$referral, useNA = "always")
      names(referral_summary)[is.na(names(referral_summary))] <- "Keine Daten"

      data_frame <- data.frame(
        Category = names(referral_summary),
        Count = as.numeric(referral_summary),
        Percentage = as.numeric(referral_summary) / sum(referral_summary) * 100
      )

      data_frame <- rbind(
        data_frame,
        data.frame(
          Category = "Summe",
          Count = sum(referral_summary),
          Percentage = sum(data_frame$Percentage)
        )
      )

      data_frame$Count <- format_number(data_frame$Count)
      data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

      report_table(
        data_frame,
        name = "referral.xml",
        align = c("left", "right", "right"),
        widths = c(60, 15, 15),
        translations = translations
      )
    }
    rm(referral_summary, data_frame)
  },
  silent = FALSE
)
