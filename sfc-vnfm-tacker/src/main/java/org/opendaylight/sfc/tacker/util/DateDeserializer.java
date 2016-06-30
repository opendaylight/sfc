/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.tacker.util;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class DateDeserializer implements JsonDeserializer<Date> {

    public static final String DATE_FORMAT_STANDARD = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DATE_FORMAT_ZULU = "yyyy-MM-dd'T'HH:mm:ssZ";

    @Override
    public Date deserialize(JsonElement jsonElement, Type typeOF, JsonDeserializationContext context)
            throws JsonParseException {

        if (jsonElement.getAsString().endsWith("Z")) {
            try {
                return new SimpleDateFormat(DATE_FORMAT_ZULU, Locale.US)
                    .parse(jsonElement.getAsString().replace("Z", "+0000"));
            } catch (ParseException ignored) {
            }
        } else {
            try {
                // cutting off the microsecond precision
                return new SimpleDateFormat(DATE_FORMAT_STANDARD, Locale.US)
                    .parse(jsonElement.getAsString().substring(0, jsonElement.getAsString().length() - 3) + "+0000");
            } catch (ParseException ignored) {
            }
        }

        throw new JsonParseException("Unparseable date: \"" + jsonElement.getAsString() + "\".");
    }
}
