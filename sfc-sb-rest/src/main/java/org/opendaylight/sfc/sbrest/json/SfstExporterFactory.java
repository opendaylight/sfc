/*
 * Copyright (c) 2015, 2017 Intel .Ltd, and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

public class SfstExporterFactory implements ExporterFactory {
    public static final String SERVICE_FUNCTION_SCHEDULE_TYPE = SfstExporter.SERVICE_FUNCTION_SCHEDULE_TYPE;
    public static final String NAME = SfstExporter.NAME;

    @Override
    public Exporter getExporter() {
        return new SfstExporter();
    }
}
