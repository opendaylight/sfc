package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface Exporter {
    ObjectMapper mapper = new ObjectMapper();

    String exportJson(DataObject dataObject);
    String exportJsonNameOnly(DataObject dataObject);
}
