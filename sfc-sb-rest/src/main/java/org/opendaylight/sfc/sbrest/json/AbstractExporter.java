/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.sbrest.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Abstract exporter
 *
 * <p>
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since       2015-12-02
 */
public abstract class AbstractExporter {

    protected ObjectMapper mapper;

    AbstractExporter() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
    }

}
