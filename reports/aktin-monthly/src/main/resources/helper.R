#' @title Append Generated File Name to Log
#' @description Logs the name of a generated file by appending it to the "r-generated-files.txt" file.
#'
#' @param name A character string representing the name of the file to log.
report_generated_file <- function(name) {
  cat(paste(name, "\n", sep = ""),
    file = "r-generated-files.txt",
    append = TRUE
  )
}

#' @title Check and Prepare Directory
#' @description Ensures that the specified directory exists and clears its contents if it already exists.
#'
#' @param dir A character string specifying the directory path.
check_dir <- function(dir) {
  if (dir.exists(dir)) {
    unlink(file.path(dir, "*", recursive = TRUE))
  } else {
    dir.create(dir, recursive = TRUE)
  }
}

#' @title Save Graph as SVG
#' @description Saves a graph or plot to an SVG file and logs the file name.
#'
#' @param graph The graph object to be saved.
#' @param name A character string for the output file name (without extension).
#' @param width Numeric, the width of the SVG in inches. Default is 8.
#' @param height Numeric, the height of the SVG in inches. Default is 4.
report_svg <- function(graph, name, width = 8, height = 4) {
  extension <- ".svg"
  device <- "svg"

  trellis.device(device, file = paste0(directory, name, extension), width = width, height = height)
  print(graph)
  dev.off()

  report_generated_file(paste0(name, extension))
}

#' @title Save Data as XHTML Table
#' @description Converts a data frame into an XHTML table and logs the generated file name.
#'
#' @param data A data frame to be converted into an XHTML table.
#' @param name A character string for the output file name.
#' @param widths Numeric vector specifying column widths. Default is NULL.
#' @param align A character vector or "auto" for alignment of columns. Default is "auto".
#' @param align_default A character string for default column alignment. Default is "left".
#' @param na_subst A character string to replace NA values in the table. Default is an empty string.
report_table <- function(data, name, widths = NULL, align = "auto", translations = NULL) {

  xhtml_table(data,
    file = paste0(xml_dir, name),
    widths = widths,
    translations = translations
  )

  report_generated_file(name)
}

#' @title Format a Number
#' @description Formats a number with specified precision, thousands separators, and ensures pretty output.
#'
#' @param num A numeric value to format.
#' @param digits An integer specifying the number of decimal places. Default is 0.
#' @return A formatted number as a character string.
format_number <- function(num, digits = 0) {
  pretty <- format(round(as.numeric(num), digits), nsmall = digits, big.mark = ".")
  return(pretty)
}

#' @title Calculate Standard Deviation
#' @description Computes the standard deviation of a numeric vector using the formula for a population.
#'
#' @param data A numeric vector of data.
#' @return The standard deviation of the input vector.
stdabw <- function(data) {
  len <- length(data)
  sqrt(var(data) * (len - 1) / len)
}

#' @title Left Join Base Function
#' @description Performs a left join between two data frames, similar to `dplyr::left_join`,
#' but uses base R's `merge` function. The output is ordered by the first join column.
#'
#' @param x A data frame. The left data frame to be joined.
#' @param y A data frame. The right data frame to be joined.
#' @param by A character vector of column names to join by. These columns must exist in both `x` and `y`.
#' @param suffix A character vector of length two, specifying suffixes to be added to non-join duplicate column names
#' in `x` and `y`. Default is `c(".x", ".y")`.
#'
#' @return A data frame resulting from the left join of `x` and `y`.
#' Rows from `x` are preserved, and missing matches in `y` are filled with `NA`.
#' The output is ordered by the first column in `by`.
left_join_base <- function(x, y, by, suffix = c(".x", ".y")) {
  if (!all(by %in% colnames(x))) stop("Join column(s) not found in 'x'")
  if (!all(by %in% colnames(y))) stop("Join column(s) not found in 'y'")

  merged_data <- merge(x, y, by = by, all.x = TRUE, suffixes = suffix)

  merged_data <- merged_data[order(as.numeric(merged_data[[by[1]]])), ]
  rownames(merged_data) <- NULL

  return(merged_data)
}

#' @title Create a "No Data" Figure
#' @description Generates a simple ggplot2 figure displaying a "No Data" message.
#'
#' @param None This function does not take any arguments.
#' @return A ggplot object containing a blank plot with a "No Data" message.
create_no_data_figure <- function() {
  text <- paste("\n   Keine Daten \n")
  graph <- ggplot() +
    annotate("text", x = 4, y = 25, size = 8, label = text) +
    theme_void()

  return(graph)
}

#' @title Create Delay Time Report
#' @description Generates a summary report for delay times, including metrics such as count,
#' missing times, positive and negative outliers, and statistical summaries (mean, median,
#' standard deviation, minimum, and maximum).
#'
#' @param delay_times A data frame containing delay times with at least one column named `Time`.
#' @param num_missing_times An integer representing the number of missing time values.
#' @param num_positive_outliers An integer representing the number of positive outliers.
#' @param num_negative_outliers An integer representing the number of negative outliers.
#' @param factors A data frame or list containing a `triage_txt` field, which provides metric
#' names or descriptions to be used in the report.
#'
#' @return A data frame with two columns:
#'   - `Metrics`: Metric names or descriptions (e.g., "Count", "Missing", "Positive Outliers").
#'   - `Time`: Corresponding values for each metric, including statistical summaries or "k.A" (keine Angabe)
#' if no data is available.
create_delay_time_report <- function(
    delay_times,
    num_missing_times,
    num_positive_outliers,
    num_negative_outliers,
    factors) {
  if (nrow(delay_times) == 0) {
    summary_values <- c("k.A", "k.A", "k.A", "k.A", "k.A")
  } else {
    summary_values <- c(
      length(delay_times$Time),
      num_missing_times,
      num_positive_outliers,
      num_negative_outliers,
      sprintf(
        fmt = "%.0f Min",
        c(
          round(mean(delay_times$Time), 1),
          median(delay_times$Time),
          round(stdabw(delay_times$Time), 1),
          min(delay_times$Time),
          max(delay_times$Time)
        )
      )
    )
  }

  summary_df <- data.frame(
    Metrics = factors[!is.na(factors)],
    Time = summary_values
  )

  return(summary_df)
}

#' @title Fill Missing Values with Last Observation Carried Forward
#' @description This function replaces `NA` values in a vector with the most recent non-`NA` value
#' found earlier in the vector. This is often referred to as "Last Observation Carried Forward" (LOCF).
#'
#' @param vec A numeric or character vector containing `NA` values to be replaced.
#' @return A vector of the same type and length as the input, with `NA` values replaced by the previous non-`NA` value.
#' If the first element of the vector is `NA`, it remains `NA`.
fill_na_locf <- function(vec) {
  for (index in seq_along(vec)) {
    if (is.na(vec[index]) && index > 1) {
      vec[index] <- vec[index - 1]
    }
  }
  return(vec)
}

#' @title
#' @description
#'
#' @param
#' @return
