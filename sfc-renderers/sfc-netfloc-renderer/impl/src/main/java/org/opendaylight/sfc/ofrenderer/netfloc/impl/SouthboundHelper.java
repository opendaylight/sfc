/*
 * Copyright (c) ZHAW and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.ofrenderer.netfloc.impl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IBridgeOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IPortOperator;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IHostPort;
import org.opendaylight.sfc.ofrenderer.netfloc.iface.IInternalPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeInternal;
import java.util.List;
import java.util.LinkedList;

public class SouthboundHelper {

  public static IInternalPort maybeCreateInternalPort(IBridgeOperator bo, TerminationPoint tp, OvsdbTerminationPointAugmentation tpa) {
    java.lang.Class<? extends InterfaceTypeBase> type = tpa.getInterfaceType();
    if (type == InterfaceTypeInternal.class) {
        IPortOperator po = new InternalPort(bo, tp, tpa);
    }
    return null;
  }

	public static String getInterfaceExternalIdsValue(
    OvsdbTerminationPointAugmentation tpa, String key) {
      String value = null;
      List<InterfaceExternalIds> pairs = tpa.getInterfaceExternalIds();
      if (pairs != null && !pairs.isEmpty()) {
        for (InterfaceExternalIds pair : pairs) {
          if (pair.getExternalIdKey().equals(key)) {
              value = pair.getExternalIdValue();
              break;
          }
        }
      }
      return value;
    }
}
