/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_verify.provider.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * This class is used for configuration generation for SFC verification.
 *
 * @author Sagar Srivastav (sagsriva@cisco.com)
 * @version 0.1
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationConfigGenerator
 * @since 2016-05-01
 */
public class SfcVerificationConfigGenerator {

    final String VERSION = "0.5";
    final int MAX_SERVICE_NODES = 100;

    private short noOfBits = 56;
    private boolean optQUIET = false;
    private int noOfServices = -1;
    private long numLimit = 0;
    public Long[] testShareSecret;
    private Long[] coeff;
    private Long[] coeffPerPack;
    private Long prime;
    private BigInteger bigPrime;
    private Short[] serviceIndices;

    public SfcVerificationConfigGenerator(int noOfServices) {
        this.noOfServices = noOfServices;
        testShareSecret = new Long[noOfServices + 1];
        serviceIndices = new Short[noOfServices + 1];
        setNoOfBits((short)56);//default, calling to set numLimit;
    }

    void debug(String str) {
        if (optQUIET)
            System.out.println(str);
    }

    long maxNum() {
        long max = Long.parseUnsignedLong("FFFFFFFFFFFFFFFF", 16);
        return max;
    }

    void setupCoeffPrime() {
        int i;
        long largest_coeff = 0;
        BigInteger largest_coeff_p;
        coeff = new Long[noOfServices];
        coeffPerPack = new Long[noOfServices];
        debug("[debug]\n Picking the following co-efficients:");
        for (i = 0; i < noOfServices; i++) {
            debug("[debug]\n" + i);
            coeff[i] = (long) 0;
            coeffPerPack[i] = (long) 0;
            Random r = new Random();
            coeff[i] = (long) (numLimit <= 1 ? Math.abs(r.nextLong()) : Math.abs(r.nextLong()) % numLimit) + 1;
            coeffPerPack[i] = (long) (numLimit <= 1 ? Math.abs(r.nextLong()) : Math.abs(r.nextLong()) % numLimit) + 1;
            if (coeff[i] > largest_coeff)
                largest_coeff = coeff[i];
            if (coeffPerPack[i] > largest_coeff)
                largest_coeff = coeffPerPack[i];
            debug("[debug] " + i + ") " + Long.toUnsignedString(coeff[i]) + "  coeff2: "
                    + Long.toUnsignedString(coeffPerPack[i]) + "\n");
        }
        debug("[debug]\n");

        largest_coeff_p = new BigInteger(Long.toUnsignedString(largest_coeff));
        bigPrime = largest_coeff_p.nextProbablePrime();
        prime = bigPrime.longValue();
        debug("[debug]\nPrime = " + Long.toUnsignedString(prime));
        debug("[debug]\nSecret = " + Long.toUnsignedString(coeff[0]));
        debug("[debug]\n Coefficients for per packet random: \n");
        for (i = 1; i < noOfServices; i++) {
            /*
             * ax^2 + bx + c then b = index 1, a = index 2
             */
            debug("[debug]For index " + i + " - " + Long.toUnsignedString(coeffPerPack[i]));
        }
    }

    BigInteger precomputeSplitShare(int x, int n, Long[] coefficients, BigInteger precompute_share) {

        int exp = 1;
        BigInteger pow_x,term;
        precompute_share = new BigInteger("0");
        pow_x = new BigInteger("1");
        debug("\n index " + x + ":");
        for (exp = 1; exp < n; exp++) {
            pow_x = pow_x.multiply(new BigInteger("" + x));
            term = pow_x.multiply(new BigInteger(Long.toUnsignedString(coefficients[exp])));
            precompute_share = precompute_share.add(term);
        }
        precompute_share = precompute_share.mod(bigPrime);
        return precompute_share;
    }

    long splitShareIndex(int index, int n, Long[] coefficients) {
        BigInteger bigShare;
        long share = 0;

        bigShare = new BigInteger("0");
        bigShare = precomputeSplitShare(index, n, coefficients, bigShare);
        bigShare = bigShare.add(new BigInteger(Long.toUnsignedString(coefficients[0])));

        bigShare = bigShare.remainder(bigPrime);
        /*
         * share = (precompute_split_share(index, n, coefficients) +
         * coefficients[0]) % prime;
         */
        share = bigShare.longValue();
        return (share);
    }

    BigInteger calculateLpc(int index, Short[] service_indices, int n, BigInteger bigLpc) {
        long lpc = 0;
        int count;
        long startPosition, nextPosition;
        BigInteger bigDenom, bigNumerator;

        bigDenom = new BigInteger("1");
        bigNumerator = new BigInteger("1");
        for (count = 1; count <= n; count++) {
            if (index == count)
                continue;
            startPosition = service_indices[index];
            nextPosition = service_indices[count];
            bigNumerator = bigNumerator.multiply(new BigInteger(Long.toString(-1 * nextPosition)));
            bigNumerator = bigNumerator.mod(bigPrime);
            bigDenom = bigDenom.multiply(new BigInteger(Long.toString(startPosition - nextPosition)));
            bigDenom = bigDenom.mod(bigPrime);

        }
        bigDenom = bigDenom.modInverse(bigPrime);
        bigLpc = bigNumerator.multiply(bigDenom);
        bigLpc = bigLpc.mod(bigPrime);

        lpc = bigLpc.longValue();
        debug("[debug] LPC for " + service_indices[index] + " = " + lpc);

        return bigLpc;
    }

    void setupServiceIndices() {
        int i, j;
        j = 0;
        System.out.println("\nUnique service indices for each service node:");

        for (i = 1; i <= noOfServices; i++) {
            serviceIndices[i] = (short) ((i) * 2);
            System.out.println(serviceIndices[i]);
        }
        Random r = new Random();
        for (i = noOfServices; i > 0; i--) {
            // Pick a random index from 1 to num_of_service
            j = r.nextInt(noOfServices) + 1;
            Short temp = serviceIndices[i];
            serviceIndices[i] = serviceIndices[j];
            serviceIndices[j] = temp;
        }
        System.out.println("\n Shuffled service indices are: ");
        for (i = 1; i <= noOfServices; i++) {
            System.out.println(serviceIndices[i]);
        }

    }

    void setupShareForSecret(Short[] serviceIndices) {
        int i;
        long share;

        System.out.println("\nSplit share for each service node:");
        for (i = 1; i <= noOfServices; i++) {
            share = splitShareIndex(serviceIndices[i], noOfServices, coeff);
            testShareSecret[i] = share;
            debug("\nIndex: " + serviceIndices[i] + " Share: " + share);
        }
        System.out.println("\n");
    }

    /* Functions to be performed at each node */
    BigInteger precomputeLpc(int index, Short[] serviceIndices, BigInteger lpci) {
        lpci = new BigInteger("0");
        lpci = calculateLpc(index, serviceIndices, noOfServices, lpci);
        return lpci;
    }

    BigInteger presplitRandom(int index, BigInteger presplit) {
        presplit = precomputeSplitShare(index, noOfServices, coeffPerPack, presplit);
        return presplit;
    }

    BigInteger updateCumulative(int index, BigInteger bigCumulative, long random, BigInteger lpc,
            BigInteger preSplit) {
        BigInteger shareRandom;

        /*
         * calculate split share for random
         */

        shareRandom = preSplit.add(new BigInteger(Long.toUnsignedString(random)));
        shareRandom = shareRandom.mod(bigPrime);

        /*
         * lpc * (share_secret + share_random)
         */
        shareRandom = shareRandom.add(new BigInteger(Long.toUnsignedString(testShareSecret[index])));
        shareRandom = shareRandom.mod(bigPrime);
        shareRandom = shareRandom.multiply(lpc);
        shareRandom = shareRandom.mod(bigPrime);

        bigCumulative = bigCumulative.add(shareRandom);
        bigCumulative = bigCumulative.mod(bigPrime);

        return bigCumulative;
    }


    void generateScvConfig() {
        int i;
        BigInteger[] lpcs = new BigInteger[noOfServices + 1];
        BigInteger[] presplitRandom = new BigInteger[noOfServices + 1];

        setupCoeffPrime();

        debug("[debug]\n");
        System.out.println("\nConfiguration:");
        System.out.println("\nPrime = " + prime);
        System.out.println("\nSecret =" + coeff[0]);

        System.out.println("\n");
        System.out.println("\nCoefficients for per packet random:");
        for (i = 1; i < noOfServices; i++) {
            /*
             * ax^2 + bx + c then b = index 1, a = index 2
             */
            System.out.println("\n " + i + ":  " + Long.toUnsignedString(coeffPerPack[i]));
        }
        System.out.println("\n");
        setupServiceIndices();
        setupShareForSecret(serviceIndices);
        System.out.println("\n");
        ArrayList<BigInteger[]> arr = testScPrecompute(lpcs, serviceIndices, presplitRandom);
        lpcs = arr.get(0);
        presplitRandom = arr.get(1);
        for (i = 1; i <= noOfServices; i++) {
            System.out.println("\n config for service node with index " + serviceIndices[i]);
            System.out.println("\n[VPP] set scv profile id 0 ");
            if (i == noOfServices) {
                System.out.println("validate-key " + Long.toUnsignedString(coeff[0]));
            }
            System.out.println("prime-number " + Long.toUnsignedString(prime) + " secret_share "
                    + Long.toUnsignedString(testShareSecret[i]));
            System.out.println("lpc " + lpcs[i]);
            System.out.println("polynomial2 " + presplitRandom[i]);
            System.out.println("bits-in-random " + noOfBits);
        }

    }


    /******** Tests ********/

    void testCombineSecret(Short[] index) {
        long cumulative = 0;
        BigInteger bigCumulative, bigLpc, temp;
        int i;

        bigCumulative = new BigInteger("0");
        bigLpc = new BigInteger("0");
        temp = new BigInteger("0");

        for (i = 1; i <= noOfServices; i++) {
            bigLpc = calculateLpc(i, index, noOfServices, bigLpc);
            bigCumulative = bigCumulative.add(bigPrime);
            temp = bigLpc.multiply(new BigInteger(Long.toString(testShareSecret[i])));
            bigCumulative = bigCumulative.add(temp);
            bigCumulative = bigCumulative.mod(bigPrime);

        }

        cumulative = Long.parseUnsignedLong("" + bigCumulative.longValue());

        System.out.println("\n Calculated secret from split shares = " + Long.toUnsignedString(cumulative));
        debug("[debug]\n Secret                              = " + Long.toUnsignedString(coeff[0]));
    }

    int fail = 0;
    int pass = 0;

    void testValidate(long cumulative, long random) {
        BigInteger magicNum=new BigInteger(Long.toUnsignedString((random + coeff[0])));
        System.out.println("\n Random + Secret = " + Long.toUnsignedString((random + coeff[0])));
        if (cumulative == magicNum.longValue()) {
            pass += 1;
            System.out.println("\n Cumulative matches the secret + random. Packet has visited all service nodes");
        } else if (cumulative == magicNum.mod(bigPrime).longValue()) {
            pass += 1;
            System.out.println(
                    "\n Cumulative matches the secret + random mod prime. Packet has visited all service nodes");
        } else {
            System.out.println("moded: "+(random + coeff[0]) % prime);

            System.out.println("\n Packet has not visited all nodes");
            fail += 1;
        }

    }

    int noOfPackets = 1;

    void testPacketFlow(BigInteger[] lpcs, Short[] index, BigInteger[] presplits) {
        int i, j;
        BigInteger cumulative;
        long[] prevCumulative = new long[noOfServices + 1];
        long random2 = 0, limit;
        int d;
        long MAX_NUM = maxNum();

        for (i = 0; i < prevCumulative.length; i++)
            prevCumulative[i] = 0;

        System.out.println("\nTesting packet flow through " + noOfServices + " services..\n");
        if (prime > 100)
            coeffPerPack[0] = prime - 100;
        System.out.println("\n Generating 2nd Random as a sequence\n");
        for (j = 0; j < noOfPackets; j++) {
            /*
             * packet no j
             */
            System.out.println("\nPacket no " + (j + 1) + " - Generated Random number" + (coeffPerPack[0] + j));
            cumulative = new BigInteger("0");
            for (i = 1; i <= noOfServices; i++) {
                cumulative = updateCumulative(i, cumulative, coeffPerPack[0] + j, lpcs[i], presplits[i]);
                System.out.println("\nService index " + index[i] + " Cumulative is: "
                        + Long.toUnsignedString(cumulative.longValue()) + "     Difference from last = "
                        + Long.toUnsignedString(cumulative.longValue() - prevCumulative[i]));
                prevCumulative[i] = cumulative.longValue();
            }
            testValidate(cumulative.longValue(), coeffPerPack[0] + j);
        }

        for (i = 0; i < prevCumulative.length; i++)
            prevCumulative[i] = 0;
        System.out.println("\n Generating 2nd Random as a pseudo random\n");

        coeffPerPack[0] = (long) 0;
        limit = prime;
        d = (int) Long.divideUnsigned(MAX_NUM, limit);

        limit *= d;
        for (j = 0; j < noOfPackets; j++) {
            /*
             * packet no j
             */
            Random r = new Random();
            random2 = ((numLimit <= 1 ? Math.abs(r.nextLong()) : Math.abs(r.nextLong()) % numLimit) + 1) % limit;
            random2 /= d;

            System.out.println("\nPacket no " + (j + 1) + " - Generated Random number " + random2 + " difference "
                    + Long.toUnsignedString((random2 - coeffPerPack[0])));
            cumulative = new BigInteger("0");
            for (i = 1; i <= noOfServices; i++) {
                cumulative = updateCumulative(i, cumulative, random2, lpcs[i], presplits[i]);
                System.out.println("\n@Service index " + index[i] + " Cumulative is: "
                        + Long.toUnsignedString(cumulative.longValue()) + " Difference from last = "
                        + Long.toUnsignedString(cumulative.longValue() - prevCumulative[i]));

                System.out.println("\n[CSV] " + j + " " + index[i] + " " + coeffPerPack[0] + " " + random2 + " "
                        + prevCumulative[i] + " " + cumulative + " " + (random2 - coeffPerPack[0]) + " "
                        + (cumulative.longValue() - prevCumulative[i]));

                prevCumulative[i] = cumulative.longValue();

            }
            coeffPerPack[0] = random2;
            testValidate(cumulative.longValue(), random2);
        }
    }

    void testPacketFlowSkip(BigInteger[] lpcs, Short[] index, BigInteger[] presplits) {
        int i;
        BigInteger cumulative;
        System.out.println("\nTesting packet flow through " + noOfServices + " services..skipping\n");
        System.out.println("\nGenerated Random number " + Long.toUnsignedString(coeffPerPack[0]));
        cumulative = new BigInteger("0");
        for (i = 1; i <= noOfServices; i++) {
            if (i % 2 == 0) {
                System.out.println("\nSkipping service node index " + index[i]);
                continue;
            }
            cumulative = updateCumulative(i, cumulative, coeffPerPack[0], lpcs[i], presplits[i]);
            System.out.println("\n@Service index " + index[i] + " Cumulative is: " + cumulative);
        }
        testValidate(cumulative.longValue(), coeffPerPack[0]);
    }

    ArrayList<BigInteger[]> testScPrecompute(BigInteger[] lpcs, Short[] index, BigInteger[] presplitRandom) {
        int i;
        for (i = 1; i <= noOfServices; i++) {
            lpcs[i] = precomputeLpc(i, index, lpcs[i]);
            presplitRandom[i] = presplitRandom(index[i], presplitRandom[i]);
            if (optQUIET) {
                debug("\n[debug] Index " + index[i] + ": LPC = ");
                System.out.println(lpcs[i].toString());
                debug("  presplit-random = ");
                debug(Arrays.toString(presplitRandom));

            }
        }

        ArrayList<BigInteger[]> arr = new ArrayList<>();
        arr.add(0, lpcs);
        arr.add(1, presplitRandom);
        return arr;

    }

    void testScv() {
        BigInteger[] lpcs = new BigInteger[noOfServices + 1];
        BigInteger[] presplit_random = new BigInteger[noOfServices + 1];
        int i;

        if (noOfServices > MAX_SERVICE_NODES) {
            System.out.println("\n Maximum service nodes supported is " + MAX_SERVICE_NODES);
            return;
        }
        setupCoeffPrime();
        setupServiceIndices();

        setupShareForSecret(serviceIndices);
        debug("[debug]\nPrime = " + Long.toUnsignedString(prime));
        debug("[debug]\nSecret = " + Long.toUnsignedString(coeff[0]));

        ArrayList<BigInteger[]> arr = testScPrecompute(lpcs, serviceIndices, presplit_random);
        lpcs = arr.get(0);
        presplit_random = arr.get(1);

        for (i = 1; i <= noOfServices; i++) {
            System.out.println("\n config for service node with index " + serviceIndices[i]);
            System.out.println("\n<sc-profile> profile-id 1 service-count " + noOfServices + " validate-key "
                    + Long.toUnsignedString(coeff[0]));
            int j;
            System.out.println(("coefficient "));
            for (j = 1; j < noOfServices; j++) {
                System.out.println(Long.toUnsignedString(coeffPerPack[j]));
            }
            System.out.println("prime-number " + prime + " secret_share " + testShareSecret[i]);
            System.out.println("service-index ");
            for (j = 1; j <= noOfServices; j++)
                System.out.println(serviceIndices[j]);
            System.out.println("my-index " + serviceIndices[i] + " mark 7 bits-in-random " + noOfBits + "</sc-profile>");
            System.out.println("\n[VPP] set scv profile id 0 ");
            if (i == noOfServices) {
                System.out.println("validate-key " + Long.toUnsignedString(coeff[0]));
            }
            System.out.println("prime-number " + prime + " secret_share" + testShareSecret[i]);
            System.out.println("lpc " + lpcs[i]);
            System.out.println("polynomial2 " + presplit_random[i]);
            System.out.println("bits-in-random " + noOfBits);
        }

        testPacketFlow(lpcs, serviceIndices, presplit_random);

        testPacketFlowSkip(lpcs, serviceIndices, presplit_random);

    }

    void testScvCombine() {

        if (noOfServices > MAX_SERVICE_NODES) {
            System.out.println("\n Maximum service nodes supported is " + MAX_SERVICE_NODES);
            return;
        }
        setupCoeffPrime();
        setupServiceIndices();

        setupShareForSecret(serviceIndices);

        System.out.println("\nPrime = " + Long.toUnsignedString(prime));
        System.out.println("\nSecret = " + coeff[0]);
        testCombineSecret(serviceIndices);
    }

    /************ getters and setters ************/

    public boolean getOptQUIET() {
        return optQUIET;
    }

    public void setOptQUIET(boolean optQUIET) {
        this.optQUIET = optQUIET;
    }

    public int getNoOfServices() {
        return noOfServices;
    }

    public void setNoOfServices(int noOfServices) {
        this.noOfServices = noOfServices;
    }

    public short getNoOfBits() {
        return noOfBits;
    }

    public void setNoOfBits(short noOfBits) {
        if(noOfBits>64){
            System.out.println("A maximum of 64 bits allowed, setting to 64");
            this.noOfBits=64;
            return;
        }
        this.noOfBits = noOfBits;
        this.numLimit = (long) (Math.pow(2, noOfBits));
    }

    public String getVERSION() {
        return VERSION;
    }

    public int getMAX_SERVICE_NODES() {
        return MAX_SERVICE_NODES;
    }

    public Long getCoeff(int i) {
        return coeff[i];
    }

    public Long[] getCoeffPerPack() {
        return coeffPerPack;
    }

    public Long getPrime() {
        return prime;
    }

    public Long getSecret() {
        return coeff[0];
    }

    public long getNumLimit() {
        return numLimit;
    }


    public Short getServiceIndices(int i) {
        return serviceIndices[i+1];
    }


    public Long getSecretShare(int i) {
        return testShareSecret[i+1];
    }

    public void setNumLimit(long numLimit) {
        if (numLimit <= 1) {
            System.out.println("numLimit should be greater than 1");
            return;
        }
        this.numLimit = numLimit;
        short numofbits = (short) Math.ceil(Math.log(numLimit) / Math.log(2));
        if(numofbits>64){
            System.out.println("A maximum of 64 bits allowed, setting to 64");
            setNoOfBits(numofbits);
            return;
        }
    }

}
