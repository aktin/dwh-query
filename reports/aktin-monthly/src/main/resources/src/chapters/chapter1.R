# Patient Sex
try(
  {
    # 1. Create data.frame
    sex_summary <- data.frame(table(df$sex, useNA = "no"))
    names(sex_summary) <- c("Category", "Count")
    total_count <- sum(sex_summary$Count)
    sex_summary$Percentage <- (sex_summary$Count / total_count) * 100

    # 2. Metrics
    sex_summary_report <- rbind(sex_summary, data.frame(
      Category = "Summe",
      Count = total_count,
      Percentage = sum(sex_summary$Percentage)
    ))

    # 3. Formatting
    sex_summary_report$Count <- format_number(sex_summary_report$Count)
    sex_summary_report$Percentage <- paste(format_number(sex_summary_report$Percentage, digits = 1), "%")

    # 4. Localisation and print report
    report_table(
      sex_summary_report,
      name = "sex.xml",
      align = c("left", "right", "right"),
      widths = c(25, 15, 15),
      translations = column_name_translations
    )
    rm(sex_summary, sex_summary_report)
  },
  silent = FALSE
)

# Patient Age
try(
  {
    df$age[df$age > 110] <- 110
    df$age[df$age < 0] <- NA

    age_statistics <- c(
      round(mean(df$age, na.rm = TRUE)),
      round(median(df$age, na.rm = TRUE)),
      round(sd(df$age, na.rm = TRUE)),
      min(df$age, na.rm = TRUE),
      max(df$age, na.rm = TRUE)
    )

    age_report <- data.frame(
      Metrics = c("Mittelwert", "Median", "Standardabweichung", "Minimum", "Maximum"),
      Age = paste(age_statistics, "Jahre")
    )
    report_table(age_report, name = "age_test.xml", align = c("left", "right"), widths = c(30, 15))
    rm(age_report)
  },
  silent = FALSE
)


try(
  {
    age <- na.omit(df$age)

    graph <- ggplot(data = data.frame(age), aes(x = age)) +
      geom_histogram(
        aes(y = after_stat(count)),
        color = "black", fill = "#046C9A",
        binwidth = 5
      ) +
      labs(
        x = "Alter [Jahre]", y = "Anzahl Patienten",
        caption = paste("n =", length(age), ", Werte größer 110 werden als 110 gewertet")
      ) +
      theme(
        plot.caption = element_text(hjust = 0.5, size = 12),
        panel.background = element_rect(fill = "white"),
        axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
        axis.text.x = element_text(face = "bold", color = "#000000", size = 12),
        axis.text.y = element_text(face = "bold", color = "#000000", size = 12)
      ) +
      scale_x_continuous(
        breaks = seq(0, 110, 5)
      ) +
      scale_y_continuous(
        expand = c(0, 0.3)
      ) +
      geom_vline(aes(xintercept = mean(age)),
        color = "#e3000b", linetype = "dashed", size = 1
      )

    report_svg(graph, "age") ### Change scala!!!! -> Scala is not correct in the histogram.
    rm(age)
  },
  silent = FALSE
)
