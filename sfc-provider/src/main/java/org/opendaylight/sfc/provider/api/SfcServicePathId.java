package org.opendaylight.sfc.provider.api;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.PathIdState;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.ServicePathIds;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.service.path.id.rev150804.service.path.ids.ServicePathIdKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Random;

/**
 * This class has the APIs to operate on the Service PathIds.
 * <p>
 *
 * @author Reinaldo Penno (rapenno@gmail.com)
 * @version 0.1
 * <p>
 * @since 2015-08-09
 */
public class SfcServicePathId {


    /* Initialization value */

    /* We only use half of the path-id space in order to use algorithmic symmetric
     * path-id generation
     */
    private final static int MAX_PATH_ID = 2^12 - 1;
    private final static int MIN_PATH_ID = 0;
    private static final Random randomGenerator = new Random();
    private static int next_pathid = (randomGenerator.nextInt() % (MAX_PATH_ID + 1));

    /* Determines the trade-off */
    private final static int N = 200;

    /**
     * Algorithm to randomize the generation of pathIds. Provides
     * security by making path-id less predictable. Adapted
     * from NAT port allocation algorithm
     *
     * <p>
     * @return Pathid or error if none available
     */
    public static int randomIncrements() {

        int num_pathid = MAX_PATH_ID -  MIN_PATH_ID + 1;
        int pathid;

        int count = num_pathid;

        do {
            next_pathid = next_pathid + (randomGenerator.nextInt() % N) + 1;
            pathid = MIN_PATH_ID + (next_pathid % num_pathid);

            if (check_suitable_pathid(pathid))
                return pathid;

            count--;
        } while (count > 0);

        return -1;
    }

    /**
     * Check if Pathid is available
     *
     * <p>
     * @param  pathid Candidate Path Id
     * @return True if available, otherwise false.
     */
    public static boolean check_suitable_pathid(long pathid) {
        ServicePathIdKey servicePathIdKey = new
                ServicePathIdKey(pathid);
        InstanceIdentifier<ServicePathId> spIID;
        spIID = InstanceIdentifier.builder(ServicePathIds.class)
                .child(ServicePathId.class, servicePathIdKey)
                .build();
        ServicePathId servicePathId = SfcDataStoreAPI.
                readTransactionAPI(spIID, LogicalDatastoreType.OPERATIONAL);

        if ((servicePathId != null) || (servicePathId.getState().compareTo(PathIdState.Available)) != 0) {
            return true;
        } else {
            return false;
        }
    }
}
