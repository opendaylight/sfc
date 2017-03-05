/*
 * Copyright (c) 2015, 2017 Intel Corp. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

public class SfstateExporterFactory implements ExporterFactory {

    public static final String SERVICE_FUNCTION_STATE = SfstateExporter.SERVICE_FUNCTION_STATE;
    public static final String NAME = SfstateExporter.NAME;

    @Override
    public Exporter getExporter() {
        return new SfstateExporter();
    }
}
