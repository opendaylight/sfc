/*
 * Copyright (c) 2016 Ericsson Spain and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.provider.listeners;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Abstract class providing some common functionality to the rest of the
 * specific listeners. This abstract listener is subscribed to changes in the
 * data tree, and depending on the type of modification, it invokes the
 * appropriate method on the derived classes.
 *
 * @author David Suárez (david.suarez.fuentes@ericsson.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
public abstract class AbstractDataTreeChangeListener<T extends DataObject>
        implements DataTreeChangeListener<T>, AutoCloseable {

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<T>> collection) {
        for (final DataTreeModification<T> dataTreeModification : collection) {
            final DataObjectModification<T> dataObjectModification = dataTreeModification.getRootNode();
            switch (dataObjectModification.getModificationType()) {
            case SUBTREE_MODIFIED:
                update(dataObjectModification.getDataBefore(), dataObjectModification.getDataAfter());
                break;
            case DELETE:
                remove(dataObjectModification.getDataBefore());
                break;
            case WRITE:
                if (dataObjectModification.getDataBefore() == null) {
                    add(dataObjectModification.getDataAfter());
                } else {
                    update(dataObjectModification.getDataBefore(), dataObjectModification.getDataAfter());
                }
                break;
            default:
                break;
            }
        }
    }

    /**
     * Method add to be implemented by the specific listener.
     *
     * @param newDataObject
     *            newly added object
     */
    protected abstract void add(T newDataObject);

    /**
     * Method remove to be implemented by the specific listener.
     *
     * @param removedDataObject
     *            existing object being removed
     */
    protected abstract void remove(T removedDataObject);

    /**
     * Method update to be implemented by the specific listener.
     *
     * @param originalDataObject
     *            existing object being modified
     * @param updatedDataObject
     *            modified data object
     */
    protected abstract void update(T originalDataObject, T updatedDataObject);
}
