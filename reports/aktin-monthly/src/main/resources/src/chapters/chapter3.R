# Transport and referral
try(
  {
    ## Very similar to chapter1 - Patient Sex - Function?
    table_formatted <- table(df$transport, useNA = "always")
    names(table_formatted)[is.na(names(table_formatted))] <- "Keine Daten"

    data_frame <- data.frame(
      Category = names(table_formatted),
      Count = as.numeric(table_formatted),
      Percentage = as.numeric(table_formatted) / sum(table_formatted) * 100
    )

    data_frame <- rbind(
      data_frame,
      data.frame(
        Category = "Summe",
        Count = sum(table_formatted),
        Percentage = sum(data_frame$Percentage)
      )
    )

    data_frame$Count <- format_number(data_frame$Count)
    data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

    report_table(
      data_frame,
      name = "transport_test.xml",
      align = c("left", "right", "right"),
      widths = c(25, 15, 15),
      translations = column_name_translations
    )
    rm(table_formatted, data_frame)

    ## Very similar to chapter1 - Patient Sex - Function?
    table_formatted <- table(df$referral, useNA = "always")
    names(table_formatted)[is.na(names(table_formatted))] <- "Keine Daten"

    data_frame <- data.frame(
      Category = names(table_formatted),
      Count = as.numeric(table_formatted),
      Percentage = as.numeric(table_formatted) / sum(table_formatted) * 100
    )

    data_frame <- rbind(
      data_frame,
      data.frame(
        Category = "Summe",
        Count = sum(table_formatted),
        Percentage = sum(data_frame$Percentage)
      )
    )

    data_frame$Count <- format_number(data_frame$Count)
    data_frame$Percentage <- paste(format_number(data_frame$Percentage, digits = 1), "%")

    report_table(
      data_frame,
      name = "refferal_test.xml",
      align = c("left", "right", "right"),
      widths = c(25, 15, 15),
      translations = column_name_translations
    )
    rm(table_formatted, data_frame)
  },
  silent = FALSE
)
