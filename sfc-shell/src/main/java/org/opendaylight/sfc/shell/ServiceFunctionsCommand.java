/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 * <p>
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
import org.opendaylight.sfc.provider.api.SfcProviderServiceFunctionAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.ServiceFunctions;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sf.rev140701.service.functions.ServiceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf CLI command to show the provisioned Service Functions.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 */
@Service
@Command(scope = "sfc", name = "sf-list", description = "Show the provisioned Service Functions")
public class ServiceFunctionsCommand extends AbstractCommand {

    @Option(name = "-name", aliases = {"--name"}, description = "Name of the Service Function", required = false,
            multiValued = false)
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFunctionsCommand.class);

    public ServiceFunctionsCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
        table.column(new Col("Type"));
        table.column(new Col("Mgmt. Addr."));
        table.column(new Col("REST Uri"));
        table.column(new Col("DPL Type"));
    }

    @Override
    public Object execute() throws Exception {
        LOG.debug("Service Function name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServiceFunctionAPI.readServiceFunction(new SfName(name)));
        } else {
            LOG.debug("Getting the list of Service Functions");
            ServiceFunctions allServiceFunctions = SfcProviderServiceFunctionAPI.readAllServiceFunctions();
            if (allServiceFunctions != null) {
                List<ServiceFunction> serviceFunctions = allServiceFunctions.getServiceFunction();
                serviceFunctions.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceFunction serviceFunction) {
        if (serviceFunction != null) {
            LOG.debug("Service Function data: {}", serviceFunction);
            table.addRow().addContent(serviceFunction.getName().getValue(),
                                      serviceFunction.getType() == null ? NO_VALUE : serviceFunction.getType()
                                              .getValue(),
                                      serviceFunction.getIpMgmtAddress() == null ? NO_VALUE : serviceFunction
                                              .getIpMgmtAddress().getIpv4Address().getValue(),
                                      serviceFunction.getRestUri() == null ? NO_VALUE : serviceFunction.getRestUri()
                                              .getValue(), serviceFunction.getSfDataPlaneLocator() == null
                                              || serviceFunction.getSfDataPlaneLocator().get(0)
                            == null ? NO_VALUE : serviceFunction.getSfDataPlaneLocator().get(0).getLocatorType());
        }
    }
}
