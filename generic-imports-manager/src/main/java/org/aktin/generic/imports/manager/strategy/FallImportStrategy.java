package org.aktin.generic.imports.manager.strategy;

public class FallImportStrategy extends AbstractDbImportStrategy {

  public FallImportStrategy() {
    super("FALL");
  }

  @Override
  protected String buildCondition() {
    return "of.concept_cd LIKE 'P21:ADMC%'";
  }
}
