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
  directory <- "output/"
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
  xml_dir <- "output/"

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

#' @title
#' @description
#'
#' @param
#' @return
