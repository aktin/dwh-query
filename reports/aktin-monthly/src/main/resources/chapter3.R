# Transport and referral
try(
  {
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
      translations = column_name_translations
    )
    rm(transport_summary, data_frame)


    refferal_summary <- table(df$referral, useNA = "always")
    names(refferal_summary)[is.na(names(refferal_summary))] <- "Keine Daten"

    data_frame <- data.frame(
      Category = names(refferal_summary),
      Count = as.numeric(refferal_summary),
      Percentage = as.numeric(refferal_summary) / sum(refferal_summary) * 100
    )

    data_frame <- rbind(
      data_frame,
      data.frame(
        Category = "Summe",
        Count = sum(refferal_summary),
        Percentage = sum(data_frame$Percentage)
      )
    )

    data_frame$Count <- format_number(data_frame$Count)
    data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

    report_table(
      data_frame,
      name = "refferal.xml",
      align = c("left", "right", "right"),
      widths = c(25, 15, 15),
      translations = column_name_translations
    )
    rm(refferal_summary, data_frame)
  },
  silent = FALSE
)
