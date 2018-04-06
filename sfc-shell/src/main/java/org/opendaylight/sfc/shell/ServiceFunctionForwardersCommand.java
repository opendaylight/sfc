/*
 * Copyright (c) 2017, 2018 Ericsson S.A. and others.  All rights reserved.
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
import org.opendaylight.sfc.provider.api.SfcProviderServiceForwarderAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.ServiceFunctionForwarders;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf CLI command to show the provisioned Service Function Forwarders.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 */
@Service
@Command(scope = "sfc", name = "sff-list", description = "Show the provisioned Service Function Forwarders")
public class ServiceFunctionForwardersCommand extends AbstractCommand {

    private static final String LOGICAL_SFF = "(Logical)";

    @Option(name = "-name", aliases = {"--name"}, description = "Name of the Service Function")
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionForwardersCommand.class);

    public ServiceFunctionForwardersCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
        table.column(new Col("Mgmt. Addr."));
        table.column(new Col("REST Uri"));
        table.column(new Col("SFF DPL"));
    }

    @Override
    public Object execute() {
        LOG.debug("Service Function Forwarder name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServiceForwarderAPI.readServiceFunctionForwarder(new SffName(name)));
        } else {
            LOG.debug("Getting the list of Service Function Forwarders");
            ServiceFunctionForwarders allServiceFunctionForwarders = SfcProviderServiceForwarderAPI
                    .readAllServiceFunctionForwarders();
            if (allServiceFunctionForwarders != null) {
                List<ServiceFunctionForwarder> serviceFunctionForwarders = allServiceFunctionForwarders
                        .getServiceFunctionForwarder();
                serviceFunctionForwarders.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceFunctionForwarder serviceFunctionForwarder) {
        if (serviceFunctionForwarder != null) {
            LOG.debug("Service Function Forwarder data: {}", serviceFunctionForwarder);
            table.addRow().addContent(serviceFunctionForwarder.getName().getValue(),
                                      serviceFunctionForwarder.getIpMgmtAddress()
                                              == null ? LOGICAL_SFF : serviceFunctionForwarder.getIpMgmtAddress()
                                              .getValue(), serviceFunctionForwarder.getRestUri()
                                              == null ? LOGICAL_SFF : serviceFunctionForwarder.getRestUri().getValue(),
                                      serviceFunctionForwarder.getSffDataPlaneLocator() == null
                                              || serviceFunctionForwarder.getSffDataPlaneLocator().get(0)
                                              == null ? LOGICAL_SFF : serviceFunctionForwarder.getSffDataPlaneLocator()
                                              .get(0).getName());
        }
    }
}
