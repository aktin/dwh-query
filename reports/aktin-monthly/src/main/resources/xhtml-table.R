#!/usr/bin/Rscript
#
# Description: Create XHTML for tabular data
# Author: R.W.Majeed
# Last-Modified: 2024-01-02
# Modified from: M. Streicher

#' Escape Special Characters for XML
#'
#' Replaces special characters (`&`, `<`, `>`) in a string with their corresponding
#' XML-safe entities (`&amp;`, `&lt;`, `&gt;`). This ensures that the text is safe
#' to include within an XML document and does not break XML syntax.
#'
#' @param data A character vector containing text that may include special characters.
#'
#' @return A character vector with the special characters replaced by their XML-safe entities.
#'
#' @details
#' The function is used to sanitize text for inclusion in XML or XHTML documents.
#' It performs the following replacements:
#' - `&` → `&amp;`
#' - `<` → `&lt;`
#' - `>` → `&gt;`
#'
#' These transformations prevent XML parsing errors or unexpected behavior caused by unescaped characters.
xml_escape <- function(data) {
  data <- gsub(pattern = "&", replacement = "&amp;", x = data)
  data <- gsub(pattern = "<", replacement = "&lt;", x = data)
  data <- gsub(pattern = ">", replacement = "&gt;", x = data)
  return(data)
}

#' Create XHTML for Tabular Data
#'
#' @description Converts a data frame or matrix into an XHTML table and writes it to a file.
#' This function allows for customizable column widths, alignments, and NA value substitutions.
#'
#' @param data A data frame or matrix containing the tabular data to be converted into XHTML format.
#'             The columns will be treated as table columns, and rows as table rows.
#' @param file_name A character string specifying the file name where the XHTML table should be written.
#'                  Defaults to "default_table.xml". If the file already exists, the function warns and skips creation.
#' @param widths A numeric vector specifying the widths of the table columns in percentages.
#'               If NULL (default), column widths are equally distributed. If a single value is provided,
#'               it is applied to all columns.
#' @param align A character vector specifying the alignment of the columns. Options include "left", "right", "center",
#'              or "auto". The default is "auto", which automatically aligns numeric columns to "right"
#'              and others to "left".
#' @param translations A list representation of the english column names with their german translation.
xhtml_table <- function(data, file_name = "default_table.xml", widths = NULL, align = "auto", translations = NULL) {
  if (!is.data.frame(data) && !is.matrix(data)) {
    stop("ERROR: 'data' must be a data frame or matrix.")
  }

  #### Currently disabled due tue debugging
  # if (file.exists(file_name)) {
  #   warning(sprintf("File '%s' already exists. Skipping file creation.", file_name))
  #   return(invisible(NULL))
  # }

  num_columns <- length(data)
  if (is.null(widths)) {
    widths <- rep(round(100 / num_columns), num_columns)
  } else if (length(widths) == 1) {
    widths <- rep(widths, num_columns)
  } else if (length(widths) != num_columns) {
    stop("ERROR: Length of 'widths' does not match the number of columns in 'data'.")
  }

  if (length(align) == 1) {
    if (align == "auto") {
      align <- ifelse(sapply(data, is.numeric), "right", "left")
    } else {
      align <- rep(align, num_columns)
    }
  } else if (length(align) != num_columns) {
    stop("ERROR: Length of 'align' does not match the number of columns in 'data'.")
  }

  file <- file(file_name, open = "wt", encoding = "UTF-8")
  on.exit(close(file), add = TRUE)

  cat('<?xml version="1.0" encoding="UTF-8"?>\n<table xmlns="http://www.w3.org/1999/xhtml">\n', file = file)

  for (i in seq_along(data)) {
    cat(sprintf('\t<col align="%s" width="%d%%"/>\n', align[i], widths[i]), file = file)
  }

  cat("\t<thead>\n\t\t<tr>\n", file = file)
  for (name in names(data)) {
    translated_name <- if (!is.null(translations) && name %in% names(translations)) {
      translations[[name]]
    } else {
      name
    }
    cat(sprintf("\t\t\t<th>%s</th>\n", xml_escape(translated_name)), file = file)
  }
  cat("\t\t</tr>\n\t</thead>\n", file = file)

  cat("\t<tbody>\n", file = file)
  for (row in seq_len(nrow(data))) {
    cat("\t\t<tr>\n", file = file)
    for (cell in data[row, , drop = FALSE]) {
      value <- if (is.na(cell) || is.nan(cell) || is.infinite(cell)) "" else xml_escape(cell)
      cat(sprintf("\t\t\t<td>%s</td>\n", value), file = file)
    }
    cat("\t\t</tr>\n", file = file)
  }
  cat("\t</tbody>\n</table>\n", file = file)
}
