/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.json;

public class SffExporterFactory implements ExporterFactory {

    public static final String SERVICE_FUNCTION_FORWARDER = SffExporter.SERVICE_FUNCTION_FORWARDER;
    public static final String NAME = SffExporter.NAME;
    public static final String REST_URI = SffExporter.REST_URI;

    @Override
    public Exporter getExporter() {
        return new SffExporter();
    }
}
