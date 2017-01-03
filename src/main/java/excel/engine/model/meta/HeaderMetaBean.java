package excel.engine.model.meta;

import java.util.Objects;

/**
 * Created by hanwen on 2016/12/29.
 */
public class HeaderMetaBean implements HeaderMeta {

  private int rowIndex;

  private String value;

  private FieldMeta fieldMeta;

  public HeaderMetaBean(int rowIndex, String value) {
    this.rowIndex = rowIndex;
    this.value = value;
  }

  @Override
  public int getRowIndex() {
    return rowIndex;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public FieldMeta getFieldMeta() {
    return fieldMeta;
  }

  void setFieldMeta(FieldMeta fieldMeta) {
    this.fieldMeta = fieldMeta;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HeaderMetaBean that = (HeaderMetaBean) o;
    return rowIndex == that.rowIndex &&
        Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rowIndex, value);
  }
}
