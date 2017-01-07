package spreadsheet.mapper.w2o.processor.listener;

import spreadsheet.mapper.model.core.Sheet;
import spreadsheet.mapper.model.meta.SheetMeta;

import java.util.List;

/**
 * listener of sheet to objects processor
 * <p>
 * Created by hanwen on 15-12-16.
 */
public interface SheetProcessListener<T> {

  /**
   * before model created
   *
   * @param sheet     sheet
   * @param sheetMeta sheet meta
   */
  void before(Sheet sheet, SheetMeta sheetMeta);

  /**
   * after all object value set
   *
   * @param sheet     sheet
   * @param sheetMeta sheet meta
   * @param objects   value set object list
   */
  void after(Sheet sheet, SheetMeta sheetMeta, List<T> objects);
}