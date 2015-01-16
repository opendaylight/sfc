package org.opendaylight.sfc.sbrest.json;

import org.opendaylight.yangtools.yang.binding.DataObject;

public interface ExporterFactory {
    Exporter getExporter();
}

