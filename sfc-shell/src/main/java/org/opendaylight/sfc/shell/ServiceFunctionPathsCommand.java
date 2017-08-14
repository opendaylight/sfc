/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.shell;

import java.util.List;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.opendaylight.sfc.provider.api.SfcProviderServicePathAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfpName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.ServiceFunctionPaths;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sfp.rev140701.service.function.paths.ServiceFunctionPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf command to show the Service Function Paths provisioned.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 */
@Service
@Command(scope = "sfc", name = "sfp-list", description = "Show the provisioned Service Function Paths")
public class ServiceFunctionPathsCommand extends AbstractCommand {

    @Option(name = "-name", aliases = {
            "--name" }, description = "Name of the Service Function Paths", required = false, multiValued = false)
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionPathsCommand.class);

    public ServiceFunctionPathsCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
        table.column(new Col("Path id"));
        table.column(new Col("Symmetric"));
        table.column(new Col("Chain name"));
        table.column(new Col("Transport type"));
        table.column(new Col("Encapsulation"));
    }

    @Override
    public Object execute() throws Exception {
        LOG.debug("Service Function Path name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServicePathAPI.readServiceFunctionPath(new SfpName(name)));
        } else {
            LOG.debug("Getting the list of Service Function Paths");
            ServiceFunctionPaths sfps = SfcProviderServicePathAPI.readAllServiceFunctionPaths();
            if (sfps != null) {
                List<ServiceFunctionPath> serviceFunctionPaths = sfps.getServiceFunctionPath();
                serviceFunctionPaths.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceFunctionPath serviceFunctionPath) {
        if (serviceFunctionPath != null) {
            LOG.debug("Service Function Path data: {}", serviceFunctionPath);
            table.addRow().addContent(serviceFunctionPath.getName().getValue(), serviceFunctionPath.getPathId(),
                    serviceFunctionPath.isSymmetric(), serviceFunctionPath.getServiceChainName().getValue(),
                    serviceFunctionPath.getTransportType() == null ? NO_VALUE
                            : serviceFunctionPath.getTransportType().getSimpleName(),
                    serviceFunctionPath.getSfcEncapsulation() == null ? NO_VALUE
                            : serviceFunctionPath.getSfcEncapsulation().getSimpleName());
        }
    }
}
