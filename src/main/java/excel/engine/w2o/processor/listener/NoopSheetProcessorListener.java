package excel.engine.w2o.processor.listener;

import excel.engine.model.core.Sheet;
import excel.engine.model.meta.SheetMeta;

import java.util.List;

/**
 * do nothing listener
 * <p>
 * Created by hanwen on 2016/12/28.
 */
public final class NoopSheetProcessorListener implements SheetProcessorListener {

  @Override
  public void before(Sheet sheet, SheetMeta sheetMeta) {
    // nothing
  }

  @Override
  public void after(Sheet sheet, SheetMeta sheetMeta, List<Object> objects) {
    // nothing
  }
}
