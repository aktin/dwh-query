# Table 1.1
try(
  {
    # 1. Create data.frame
    sex_summary <- data.frame(table(df$sex, useNA = "always"))
    if (nrow(sex_summary) == 0) {
      # Backup data.frame if no data are available
      sex_summary_report <- data.frame(
        Category = "-",
        Count = "-",
        Percentage = "-"
      )

      report_table(
        sex_summary_report,
        name = "sex.xml",
        align = c("center", "center", "center"),
        translations = translations
      )
    } else {
      names(sex_summary) <- c("Category", "Count")
      sex_summary$Category <- as.character(sex_summary$Category)
      sex_summary$Category[is.na(sex_summary$Category)] <- "NA"
      total_count <- sum(sex_summary$Count)
      sex_summary$Percentage <- (sex_summary$Count / total_count) * 100

      # 2. Metrics
      sex_summary_report <- rbind(sex_summary, data.frame(
        Category = "sum",
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
        translations = translations
      )
    }
    rm(sex_summary, sex_summary_report)
  },
  silent = FALSE
)

# Table 1.2
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


    age_statistics[is.na(age_statistics)] <- "-"
    age_statistics[age_statistics == "-Inf" | age_statistics == "Inf"] <- "-"

    age_report <- data.frame(
      Metrics = c("Mittelwert", "Median", "Standardabweichung", "Minimum", "Maximum"),
      Age = paste(age_statistics, "Jahre")
    )
    report_table(
      age_report,
      name = "age.xml",
      align = c("left", "right"),
      widths = c(30, 15),
      translations = translations
    )
    rm(age_report)
  },
  silent = FALSE
)

# Figure 1.1
try(
  {
    age <- na.omit(df$age)

    if (length(age) == 0) {
      graph <- create_no_data_figure()
    } else {
      age_groups <- cut(
        age,
        breaks = seq(0, 110, by = 5),
        include.lowest = TRUE,
        right = FALSE
      )
      age_df <- data.frame(age_groups = age_groups)

      counts <- table(age_groups)
      max_count <- max(counts)

      graph <- ggplot(data = age_df, aes(x = age_groups)) +
        geom_bar(
          color = "black", fill = "#00427e",
          stat = "count"
        ) +
        geom_vline(
          aes(xintercept = mean(as.numeric(age_groups)), linetype = "Durchschnittsalter"),
          color = "#e3000f", linewidth = 1
        ) +
        theme(
          plot.caption = element_text(size = 12, hjust = 0.5),
          panel.background = element_rect(fill = "white"),
          axis.title = element_text(size = 12),
          panel.border = element_blank(),
          axis.line = element_line(color = "black"),
          axis.text.x = element_text(color = "#000000", size = 12, hjust = 1, angle = 45),
          axis.text.y = element_text(color = "#000000", size = 12),
          legend.title = element_blank(),
          legend.position = "bottom",
          legend.justification = "center",
          legend.direction = "horizontal",
          legend.box = "horizontal",
          legend.text = element_text(size = 12)
        ) +
        labs(
          x = "Alter [Jahre]", y = "Anzahl Patienten",
          caption = paste("n =", length(age), ", Werte größer 110 werden als 110 gewertet.")
        ) +
        scale_x_discrete(
          labels = function(x) paste0(x)
        ) +
        scale_y_continuous(expand = c(0,0))
    }

    report_svg(graph, "age")
    rm(age)
  },
  silent = FALSE
)
