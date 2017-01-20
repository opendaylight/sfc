/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package ch.icclab.netfloc.impl;
import ch.icclab.netfloc.iface.ofhandlers.ILinkHandler;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import ch.icclab.netfloc.impl.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.topology.discovery.rev130819.LinkDiscovered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkDataChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(LinkDataChangeListener.class);
    private static final String DEFAULT_TOPOLOGY_ID = "flow:1";
    private DataBroker dataBroker = null;
    private ListenerRegistration<DataChangeListener> registration;
    private ILinkHandler linkHandler;

    public LinkDataChangeListener (DataBroker dataBroker, ILinkHandler linkHandler) {
      this.linkHandler = linkHandler;
      this.dataBroker = dataBroker;
      this.start();
    }

    public void start() {
      InstanceIdentifier<Link> path = InstanceIdentifier.builder(NetworkTopology.class)
          .child(Topology.class, new TopologyKey(new TopologyId(DEFAULT_TOPOLOGY_ID)))
          .child(Link.class).build();
      registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, this,
              DataChangeScope.BASE);
      LOG.info("LinkDataChangeListener dataBroker= {}, registration= {}",
              dataBroker, registration);
    }

    @Override
    public void close () throws Exception {
      registration.close();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
      LOG.trace("LinkDataChangeListener onDataChanged: changes {}", changes);
      processLinkCreation(changes);
      processLinkDeletion(changes);
      processLinkUpdate(changes);
    }

    private void processLinkCreation(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
      for(Map.Entry<InstanceIdentifier<?>, DataObject> newLink : changes.getCreatedData().entrySet()){
        if(newLink.getKey().getTargetType().equals(Link.class)){
          LOG.info("LinkDataChangeListener processLinkCreation <{}>", newLink.getValue());
          Link link = (Link)newLink.getValue();
          this.linkHandler.handleLinkCreate(link);
        }
      }
    }

    private void processLinkDeletion(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
      for(InstanceIdentifier<?> removedLinkId : changes.getRemovedPaths()) {
        if(removedLinkId.getTargetType().equals(Link.class)){
          @SuppressWarnings("unchecked")
          Link removedLink = getDataChanges(changes.getOriginalData(),
                  (InstanceIdentifier<Link>)removedLinkId);
          LOG.info("LinkDataChangeListener processLinkDeletion <{}>", removedLink);
          this.linkHandler.handleLinkDelete(removedLink);
        }
      }
    }

    private void processLinkUpdate(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
      for(Map.Entry<InstanceIdentifier<?>, DataObject> updatedLink : changes.getCreatedData().entrySet()){
        if(updatedLink.getKey().getTargetType().equals(Link.class)){
          LOG.info("LinkDataChangeListener processLinkUpdate <{}>", updatedLink.getValue());
          Link link = (Link)updatedLink.getValue();
          this.linkHandler.handleLinkUpdate(link);
        }
      }
    }

    private <T extends DataObject> T getDataChanges(
          Map<InstanceIdentifier<?>, DataObject> changes,InstanceIdentifier<T> path){
      for(Map.Entry<InstanceIdentifier<?>,DataObject> change : changes.entrySet()){
        if(change.getKey().getTargetType().equals(path.getTargetType())){
          @SuppressWarnings("unchecked")
          T dataObject = (T) change.getValue();
          return dataObject;
        }
      }
      return null;
    }
}
