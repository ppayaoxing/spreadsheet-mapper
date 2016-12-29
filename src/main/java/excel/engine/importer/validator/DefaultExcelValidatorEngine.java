package excel.engine.importer.validator;

import excel.engine.exception.ExcelReadException;
import excel.engine.exception.ExcelValidateException;
import excel.engine.importer.validator.cell.CellValidator;
import excel.engine.importer.validator.row.RowValidator;
import excel.engine.importer.validator.sheet.SheetValidator;
import excel.engine.importer.validator.workbook.WorkbookValidator;
import excel.engine.model.excel.ExcelMeta;
import excel.engine.model.excel.Row;
import excel.engine.model.excel.Sheet;
import excel.engine.model.excel.Workbook;
import excel.engine.model.message.DataValidateMessage;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * Created by hanwen on 15-12-16.
 */
public class DefaultExcelValidatorEngine implements ExcelValidatorEngine {

  /*===============
    validators
   ================*/
  private List<WorkbookValidator> workbookValidators = new ArrayList<>();
  private List<SheetValidator> sheetValidators = new ArrayList<>();
  private Map<Integer, Map<String, List<RowValidator>>> key2rowValidators = new HashMap<>();
  private Map<Integer, Map<String, List<CellValidator>>> key2cellValidators = new HashMap<>();

  /*==============
    error messages TODO
   ===============*/
  private List<DataValidateMessage> errorMessages = new ArrayList<>();

  private Workbook workbook;

  public DefaultExcelValidatorEngine(Workbook workbook) {
    this.workbook = workbook;
  }

  @Override
  public void addWorkbookValidator(WorkbookValidator... validators) {
    if (validators == null) {
      return;
    }
    Collections.addAll(this.workbookValidators, validators);
  }

  @Override
  public void addSheetValidator(SheetValidator... validators) {
    if (validators == null) {
      return;
    }
    Collections.addAll(this.sheetValidators, validators);
  }

  @Override
  public void addRowValidator(RowValidator... validators) {
    if (validators == null) {
      return;
    }

    for (RowValidator validator : validators) {
      String key = validator.getKey();
      int sheetIndex = validator.getSheetIndex();

      if (!key2rowValidators.containsKey(sheetIndex)) {
        key2rowValidators.put(sheetIndex, new HashMap<String, List<RowValidator>>());
      }

      Map<String, List<RowValidator>> validatorsOfSheet = key2rowValidators.get(sheetIndex);
      if (!validatorsOfSheet.containsKey(key)) {
        validatorsOfSheet.put(key, new ArrayList<RowValidator>());
      }
      validatorsOfSheet.get(key).add(validator);
    }
  }

  @Override
  public void addCellValidator(CellValidator... validators) {
    if (validators == null) {
      return;
    }
    for (CellValidator validator : validators) {
      String key = validator.getKey();
      int sheetIndex = validator.getSheetIndex();

      if (!key2cellValidators.containsKey(sheetIndex)) {
        key2cellValidators.put(sheetIndex, new HashMap<String, List<CellValidator>>());
      }

      Map<String, List<CellValidator>> validatorsOfSheet = key2cellValidators.get(sheetIndex);
      if (!validatorsOfSheet.containsKey(key)) {
        validatorsOfSheet.put(key, new ArrayList<CellValidator>());
      }
      validatorsOfSheet.get(key).add(validator);
    }
  }

  @Override
  public boolean valid() {
    if (workbook == null) {
      throw new ExcelReadException("workbook is null");
    }

    // valid data
    validWorkbook(workbook);
    if (CollectionUtils.isNotEmpty(errorMessages)) {
      return false;
    }

    for (Sheet sheet : workbook.getSheets()) {

      // check dependency of this sheet
      checkValidatorKeyDependencyOfSheet(sheet.getIndex());

      validSheet(sheet);

      if (CollectionUtils.isNotEmpty(errorMessages)) {
        return false;
      }

      for (Row row : sheet.getRows()) {

        validRowCells(row, sheet.getIndex());
      }
    }

    return CollectionUtils.isEmpty(errorMessages);
  }

  /**
   * check if dependency correct
   */
  private void checkValidatorKeyDependencyOfSheet(int sheetIndex) {

    Map<String, Set<String>> dependsOnHierarchy = new HashMap<>();

    dependsOnHierarchy.putAll(buildDependsOnHierarchy(key2rowValidators.get(sheetIndex)));
    dependsOnHierarchy.putAll(buildDependsOnHierarchy(key2cellValidators.get(sheetIndex)));

    Set<String> satisfiedKeys = new HashSet<>();
    for (String key : dependsOnHierarchy.keySet()) {
      Set<String> dependencyKeys = new HashSet<>();
      checkValidatorKeyDependencyHierarchy(dependsOnHierarchy, satisfiedKeys, dependencyKeys, key);
      satisfiedKeys.addAll(dependencyKeys);
    }
  }

  private void checkValidatorKeyDependencyHierarchy(
      Map<String, Set<String>> dependsOnHierarchy,
      Set<String> satisfiedKeys,
      Set<String> dependencyKeys,
      String key
  ) {

    if (satisfiedKeys.contains(key)) {
      return;
    }

    dependencyKeys.add(key);
    for (String dependsOn : dependsOnHierarchy.get(key)) {

      if (!dependsOnHierarchy.containsKey(dependsOn)) {
        throw new ExcelValidateException("dependency missing key [" + dependsOn + "]");
      }

      if (dependencyKeys.contains(dependsOn)) {
        throw new ExcelValidateException("dependency cycling on [" + key + "] and [" + dependsOn + "]");
      }

      checkValidatorKeyDependencyHierarchy(dependsOnHierarchy, satisfiedKeys, dependencyKeys, dependsOn);
    }
  }

  /*=========================
   below is internal valid
   ==========================*/
  private void validWorkbook(Workbook workbook) {

    for (WorkbookValidator validator : workbookValidators) {

      if (!validator.valid(workbook)) {

      }
    }

  }

  private void validSheet(Sheet sheet) {

    for (SheetValidator validator : sheetValidators) {

      if (!validator.valid(sheet)) {

      }
    }

  }

  private void validRowCells(Row row, int sheetIndex) {

    Map<String, Set<String>> dependsOnHierarchy = new HashMap<>();

    dependsOnHierarchy.putAll(buildDependsOnHierarchy(key2rowValidators.get(sheetIndex)));
    dependsOnHierarchy.putAll(buildDependsOnHierarchy(key2cellValidators.get(sheetIndex)));

    // one key corresponding multi validators
    Map<String, List<? extends RelationValidator>> validatorMap = new HashMap<>();

    Map<String, List<RowValidator>> rowValidatorsOfSheet = key2rowValidators.get(sheetIndex);
    validatorMap.putAll(rowValidatorsOfSheet);
    Map<String, List<CellValidator>> cellValidatorsOfSheet = key2cellValidators.get(sheetIndex);
    validatorMap.putAll(cellValidatorsOfSheet);

    Map<String, Set<Boolean>> allResult = new HashMap<>();

    for (List<RowValidator> validators : rowValidatorsOfSheet.values()) {
      for (RowValidator validator : validators) {
        allResult.putAll(validRowCellsHierarchy(validatorMap, allResult, dependsOnHierarchy, row, validator.getKey()));
      }
    }
    for (List<CellValidator> validators : cellValidatorsOfSheet.values()) {
      for (CellValidator validator : validators) {
        allResult.putAll(validRowCellsHierarchy(validatorMap, allResult, dependsOnHierarchy, row, validator.getKey()));
      }
    }

  }

  private Map<String, Set<Boolean>> validRowCellsHierarchy(
      Map<String, List<? extends RelationValidator>> validatorMap,
      Map<String, Set<Boolean>> allResult,
      Map<String, Set<String>> dependsOnHierarchy,
      Row row,
      String key
  ) {

    Map<String, Set<Boolean>> result = new HashMap<>();

    if (allResult.containsKey(key)) {
      result.put(key, allResult.get(key));
      return result;
    }

    Set<String> dependsOns = dependsOnHierarchy.get(key);

    if (CollectionUtils.isNotEmpty(dependsOns)) {

      for (String dependsOn : dependsOns) {

        result.putAll(validRowCellsHierarchy(validatorMap, result, dependsOnHierarchy, row, dependsOn));
      }

    }

    if (ifSkip(result)) {
      result.put(key, Collections.singleton((Boolean) null));
      return result;
    }

    Set<Boolean> vrs = new HashSet<>();
    for (RelationValidator relationValidator : validatorMap.get(key)) {
      vrs.add(doRowCellsValid(relationValidator, row));
    }
    result.put(key, vrs);
    return result;
  }

  /**
   * if result is not empty and result set not only has true skip the valid
   *
   * @param result dependsOn result
   * @return true if skip
   */
  private boolean ifSkip(Map<String, Set<Boolean>> result) {
    Set<Boolean> dependsOnVrs = new HashSet<>();
    for (Set<Boolean> vr : result.values()) {
      dependsOnVrs.addAll(vr);
    }

    return !dependsOnVrs.isEmpty() && (dependsOnVrs.size() > 1 || !dependsOnVrs.iterator().next());
  }

  private boolean doRowCellsValid(RelationValidator relationValidator, Row row) {
    if (relationValidator instanceof RowValidator) {

      return ((RowValidator) relationValidator).valid(row);
    } else {

      CellValidator cellValidator = (CellValidator) relationValidator;
      return cellValidator.valid(row.getCell(cellValidator.getMatchField()));
    }
  }

  private <META extends ExcelMeta, VALIDATOR extends RelationValidator<META>> Map<String, Set<String>> buildDependsOnHierarchy(Map<String, List<VALIDATOR>> key2dataValidator) {
    Map<String, Set<String>> dependsOnHierarchy = new HashMap<>();

    for (Map.Entry<String, List<VALIDATOR>> entry : key2dataValidator.entrySet()) {
      String key = entry.getKey();
      dependsOnHierarchy.put(key, new HashSet<String>());

      for (VALIDATOR dataValidator : entry.getValue()) {

        Set<String> dependsOn = dataValidator.getDependsOn();
        if (CollectionUtils.isNotEmpty(dependsOn)) {

          dependsOnHierarchy.get(key).addAll(dependsOn);
        }
      }
    }

    return dependsOnHierarchy;
  }
}
