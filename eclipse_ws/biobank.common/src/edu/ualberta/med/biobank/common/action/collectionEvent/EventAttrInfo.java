package edu.ualberta.med.biobank.common.action.collectionEvent;

import java.io.Serializable;

import edu.ualberta.med.biobank.common.action.eventattr.EventAttrTypeEnum;
import edu.ualberta.med.biobank.common.util.NotAProxy;
import edu.ualberta.med.biobank.model.EventAttr;

public class EventAttrInfo implements Serializable, NotAProxy {
    private static final long serialVersionUID = 1L;

    public EventAttr attr;
    public EventAttrTypeEnum type;

}
