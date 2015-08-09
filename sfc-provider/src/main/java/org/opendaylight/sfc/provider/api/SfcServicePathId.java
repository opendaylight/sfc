package org.opendaylight.sfc.provider.api;

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

    private final static int MAX_PATH_ID = 2^12 - 1;
    private final static int MIN_PATH_ID = 0;
    private static final Random randomGenerator = new Random();
    private static int next_pathid = (randomGenerator.nextInt() % (MAX_PATH_ID + 1));

    /* Determines the trade-off */
    private final static int N = 200;

    /**
     * Algorithm to randomize the generation of pathIds.
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

            //if (check_suitable_pathid(pathid))
            //    return pathid;

            count--;
        } while (count > 0);

        return -1;
    }
}
