package org.opendaylight.sfc.sbrest.json;

import org.opendaylight.yangtools.yang.binding.DataObject;

public interface Exporter {
    String exportJson(DataObject dataObject);
    String exportJsonNameOnly(DataObject dataObject);
}
