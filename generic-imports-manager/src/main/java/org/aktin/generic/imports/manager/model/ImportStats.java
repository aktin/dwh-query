package org.aktin.generic.imports.manager.model;

import org.aktin.generic.imports.manager.enums.CSVFile;

public class ImportStats {
  private final int year;
  private final CSVFile source;
  private final int count;

  public ImportStats(int year, CSVFile source, int count) {
    this.year = year;
    this.source = source;
    this.count = count;
  }

  public int getYear() {
    return year;
  }

  public CSVFile getSource() {
    return source;
  }

  public int getCount() {
    return count;
  }

  @Override
  public String toString() {
    return String.format("Year: %d | %s: %d", year, source, count);
  }
}
