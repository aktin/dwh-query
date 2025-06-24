package org.aktin.generic.imports.manager.enums;

import org.jetbrains.annotations.NotNull;

public enum CSVFile {
  FALL("FALL"),
  FAB("FAB"),
  ICD("ICD"),
  OPS("OPS");

  public final String filename;

  CSVFile(String filename) {
    this.filename = filename;
  }

  public static @NotNull CSVFile fromString(String source) {
    for (CSVFile csvFile : CSVFile.values()) {
      if (csvFile.filename.equals(source)) {
        return csvFile;
      }
    }
    throw new IllegalArgumentException("CSV file " + source + " not found");
  }

  @Override
  public String toString() {
    return filename;
  }

}
