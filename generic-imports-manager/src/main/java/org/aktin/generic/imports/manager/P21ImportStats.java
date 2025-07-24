package org.aktin.generic.imports.manager;

public class P21ImportStats {

  private final int year;
  private final String source;
  private final int count;

  /**
   * Constructs import statistics with p21 encounter count for a given year and data source
   *
   * @param year   the year the data applies to
   * @param source the csv source file of the data (mostly FALL, FAB, ICD or OPS)
   * @param count  the number of found encounters in database
   */
  public P21ImportStats(int year, String source, int count) {
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
