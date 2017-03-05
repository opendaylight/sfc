/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

public class AclExporterFactory implements ExporterFactory {
    public static final String ACL = AclExporter.ACL;
    public static final String ACL_NAME = AclExporter.ACL_NAME;

    @Override
    public Exporter getExporter() {
        return new AclExporter();
    }
}
