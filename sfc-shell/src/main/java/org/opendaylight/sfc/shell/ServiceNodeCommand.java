/**
 * Copyright (c) 2017 Ericsson S.A. and others.  All rights reserved.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.shell;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;
import org.opendaylight.sfc.provider.api.SfcProviderServiceNodeAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SfName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SffName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.common.rev151017.SnName;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.ServiceNodes;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sn.rev140701.service.nodes.ServiceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf CLI command to show the provisioned Service Nodes.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
@Service
@Command(scope = "sfc", name = "sn-list", description = "Show the provisioned Service Nodes")
public class ServiceNodeCommand extends AbstractCommand {
    @Option(name = "-name", aliases = {"--name"}, description = "Name of the Service Node", required = false,
            multiValued = false)
    private String name;

    private final ShellTable table;

    private static final Logger LOG = LoggerFactory.getLogger(ServiceNodeCommand.class);

    public ServiceNodeCommand() {
        table = new ShellTable();
        table.column(new Col("Name"));
        table.column(new Col("Mgmt. Addr."));
        table.column(new Col("SFs"));
        table.column(new Col("SFFs"));
    }

    @Override
    public Object execute() throws Exception {
        LOG.debug("Service Node name: {}", name);

        if (name != null) {
            renderContent(SfcProviderServiceNodeAPI.readServiceNodeByName(new SnName(name)));
        } else {
            LOG.debug("Getting the list of Service Nodes");
            ServiceNodes allServiceNodes = SfcProviderServiceNodeAPI.readAllServiceNodes();

            if (allServiceNodes != null) {
                List<ServiceNode> serviceNodes = allServiceNodes.getServiceNode();
                serviceNodes.forEach(this::renderContent);
            }
        }
        table.print(getConsole());
        return null;
    }

    private void renderContent(ServiceNode serviceNode) {
        if (serviceNode != null) {
            LOG.debug("Service Node data: {}", serviceNode);
            table.addRow().addContent(serviceNode.getName().getValue(),
                                      serviceNode.getIpMgmtAddress() == null ? NO_VALUE : serviceNode.getIpMgmtAddress()
                                              .getIpv4Address().getValue(),
                                      serviceNode.getServiceFunction() == null ? NO_VALUE : serviceNode
                                              .getServiceFunction().stream().map(SfName::getValue)
                                              .collect(Collectors.joining(SEPARATOR)),
                                      serviceNode.getServiceFunctionForwarder() == null ? NO_VALUE : serviceNode
                                              .getServiceFunctionForwarder().stream().map(SffName::getValue)
                                              .collect(Collectors.joining(SEPARATOR)));
        }
    }
}
