/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

public class SfgExporterFactory implements ExporterFactory {

    public static final String SERVICE_FUNCTION_GROUP = SfgExporter.SERVICE_FUNCTION_GROUP;
    public static final String NAME = SfgExporter.NAME;
    public static final String REST_URI = SfgExporter.REST_URI;

    @Override
    public Exporter getExporter() {
        return new SfgExporter();
    }
}
