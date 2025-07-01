package org.aktin.generic.imports.manager.model;

public class ImportStats {

  private final int year;
  private final String source;
  private final int count;

  public ImportStats(int year, String source, int count) {
    this.year = year;
    this.source = source;
    this.count = count;
  }

  public int getYear() {
    return year;
  }

  public String getSource() {
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
