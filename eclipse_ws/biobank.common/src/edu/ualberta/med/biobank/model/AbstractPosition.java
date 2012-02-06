package edu.ualberta.med.biobank.model;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import edu.ualberta.med.biobank.common.util.RowColPos;

@MappedSuperclass
public class AbstractPosition extends AbstractBiobankModel {
    private static final long serialVersionUID = 1L;

    private Integer row;
    private Integer col;

    @Min(value = 0, message = "{edu.ualberta.med.biobank.model.AbstractPosition.row.Min}")
    @NotNull(message = "{edu.ualberta.med.biobank.model.AbstractPosition.row.NotNull}")
    @Column(name = "ROW", nullable = false)
    public Integer getRow() {
        return this.row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    @Min(value = 0, message = "{edu.ualberta.med.biobank.model.AbstractPosition.col.Min}")
    @NotNull(message = "{edu.ualberta.med.biobank.model.AbstractPosition.col.NotNull}")
    @Column(name = "COL", nullable = false)
    public Integer getCol() {
        return this.col;
    }

    public void setCol(Integer col) {
        this.col = col;
    }

    @Transient
    public RowColPos getPosition() {
        return new RowColPos(getRow(), getCol());
    }
}