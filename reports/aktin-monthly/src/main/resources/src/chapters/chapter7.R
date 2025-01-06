# Length of stay (los)
try(
  {
    df$los <- difftime(df$discharge.ts, df$admit.ts, units = "mins")
    valid_los <- na.omit(df$los[df$los > 0 & df$los <= 24 * 60])

    los_times <- data.frame(
      Time = valid_los,
      Description = rep("Zeit", length(valid_los))
    )

    summary <- create_delay_time_report(
      delay_times = los_times,
      num_missing_times = sum(is.na(df$los)),
      num_negative_outliers = sum(df$los < 0, na.rm = TRUE),
      num_positive_outliers = sum(df$los > 24 * 60, na.rm = TRUE),
      factors = factors$los_txt
    )

    report_table(
      summary,
      name = "los.xml",
      align = c("left", "right"),
      widths = c(45, 15),
      translations = column_name_translations
    )
    rm(summary, los_times)
  },
  silent = FALSE
)

# # Time to discharge
# try(
#   {
#     a <- df$discharge.d[df$discharge.d < 600]
#     outofbounds <- length(df$discharge.d) - length(a)
#     isNA <- length(df$discharge.d[is.na(df$discharge.d)])
#     b <- a[!is.na(a)]
#     b <- data.frame(b)
#     b$b <- as.numeric(b$b)
#     if (nrow(b) == 0) {
#       text <- paste("\n   Keine Daten \n")
#       graph <- ggplot() +
#         annotate("text", x = 4, y = 25, size = 8, label = text) +
#         theme_void()
#       graph2 <- ggplot() +
#         annotate("text", x = 4, y = 25, size = 8, label = text) +
#         theme_void()
#       b <- data.frame(b)
#       b$b <- as.numeric(b$b)
#       b <- b %>% filter(b > -1 & b < 61)
#       # b$x<-"Zeit"
#       z <- b %>% filter(b < 11)
#       z <- length(z$b)
#     } else {
#       b$x <- "Zeit"
#       graph <- ggplot(data = b, aes(x = x, y = b)) +
#         geom_boxplot(fill = "#046C9A", width = 0.5) +
#         # geom_jitter(width = 0.05,alpha=0.2)+
#         labs(
#           y = "Zeit von Aufnahme bis zur Entlassung/ \n Verlegung [Minuten]",
#           caption = paste("Fehlende Werte: ", los_NA, "; Werte < 0h: ", lt_zero, "; Werte > 24h:", gt_day, "; Werte über 600 Minuten: ", outofbounds)
#         ) +
#         theme(
#           plot.caption = element_text(hjust = 0.5, size = 12),
#           panel.background = element_rect(fill = "white"),
#           axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
#           axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
#           axis.title.x = element_blank(),
#           axis.ticks.x = element_blank(),
#           axis.text.x = element_blank(),
#           legend.title = element_blank(),
#           legend.position = "bottom",
#           legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
#         ) +
#         scale_y_continuous(breaks = seq(0, 600, 40)) +
#         geom_hline(aes(yintercept = mean(b), color = "Mittelwert"), size = 0.9)
#       graph2 <- ggplot(b, aes(x = b)) +
#         geom_histogram(aes(y = 100 * (..count..) / sum(..count..)), bins = 11, color = "black", fill = "#046C9A", boundary = 0) +
#         scale_x_continuous(breaks = seq(0, 600, length = 11)) +
#         labs(y = "Relative Häufigkeit [%]") +
#         theme(
#           plot.caption = element_text(hjust = 0.5, size = 12),
#           panel.background = element_rect(fill = "white"),
#           axis.title = element_text(size = 12), panel.border = element_blank(), axis.line = element_line(color = "black"),
#           axis.text.y = element_text(face = "bold", color = "#000000", size = 12),
#           axis.title.x = element_blank(),
#           # axis.ticks.x=element_blank(),
#           # axis.text.x=element_blank(),
#           legend.title = element_blank(),
#           legend.position = "bottom",
#           legend.text = element_text(color = "#e3000b", size = 12, face = "bold")
#         )
#     }

#     report_svg(graph, "discharge.d.box")
#     report_svg(graph2, "discharge.d.hist")
#   },
#   silent = FALSE
# )
