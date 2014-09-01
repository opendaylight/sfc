/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ofsfc.provider.logback;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author malauko
 */
public class SfcProviderLogbackLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SfcProviderLogbackLoader.class);

    private static final Path LOGBACK_XML_PATH = Paths.get("configuration/sfc/logback.xml");

    public static void loadSfcLogbackConfiguration() {
        if (Files.exists(LOGBACK_XML_PATH)) {
            File logbackxmlFile = LOGBACK_XML_PATH.toFile();
            load(false, logbackxmlFile);
        }
    }

    /**
     * load given logback configurations in given order, reset existing
     * configuration before applying first one
     *
     * @param purgeBefore require reset before loading first config
     * @param args
     */
    private static void load(boolean purgeBefore, Object... args) {
        try {
            if (purgeBefore) {
                resetExistingConfiguration();
            }
            for (Object logbackConfig : args) {
                load(logbackConfig);
            }
        } catch (IllegalStateException e) {
            LOG.warn("loading of multiple logback configurations failed", e);
        }
    }

    /**
     * purge existing logback configuration
     */
    private static void resetExistingConfiguration() {
        LOG.trace("resetting existing logback configuration");
        LoggerContext context = getLoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();
    }

    /**
     * @return logback context
     */
    private static LoggerContext getLoggerContext() {
        ILoggerFactory context = LoggerFactory.getILoggerFactory();
        if (context != null && context instanceof LoggerContext) {
            // now SLF4J is bound to logback in the current environment
            return (LoggerContext) context;
        }
        throw new IllegalStateException("current logger factory is not supported: " + context);
    }

    /**
     * @param logbackConfig
     * @param reset true if previous configuration needs to get purged
     */
    private static void load(Object logbackConfig) {
        LOG.trace("BEFORE logback reconfig");
        try {
            LoggerContext context = getLoggerContext();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            if (logbackConfig instanceof String) {
                configurator.doConfigure((String) logbackConfig);
            } else if (logbackConfig instanceof URL) {
                configurator.doConfigure((URL) logbackConfig);
            } else if (logbackConfig instanceof File) {
                configurator.doConfigure((File) logbackConfig);
            }

            LOG.trace("applied {}", logbackConfig);

            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        } catch (IllegalStateException | JoranException je) {
            LOG.warn("Logback configuration loading failed: {}", logbackConfig);
        }
        LOG.trace("AFTER logback reconfig");
    }

}
