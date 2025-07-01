package org.aktin.generic.imports.manager.strategy;

public class FabImportStrategy extends AbstractDbImportStrategy {

  public FabImportStrategy() {
    super("FAB");
  }

  @Override
  protected String buildCondition() {
    return "of.provider_id = 'P21' AND of.concept_cd LIKE 'P21:DEP%'";
  }
}
