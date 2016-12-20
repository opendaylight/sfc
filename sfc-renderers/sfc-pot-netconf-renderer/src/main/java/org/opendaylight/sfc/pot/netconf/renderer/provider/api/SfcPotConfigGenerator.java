/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.pot.netconf.renderer.provider.api;

import java.math.BigInteger;
import java.util.Random;

/**
 * This class is used for configuration generation for SFC Proof of Transit.
 *
 * @author Sagar Srivastav (sagsriva@cisco.com)
 * @version 0.2
 * @since 2016-05-01
 */
public class SfcPotConfigGenerator {

    final static String VERSION = "0.5";
    final static int MAX_SERVICE_NODES = 100;
    final static short DEFAULT_NUM_BITS = 60;

    private short noOfBits;
    private int noOfServices = -1;
    private long numLimit = 0;
    public long[] secretSharePoly1;
    private long[] coeffOfPoly1;
    private long[] coeffOfPoly2;
    private BigInteger bigPrime;
    private short[] serviceIndices;
    private BigInteger[] preEvalPoly2;
    private BigInteger[] lpcs;

    public SfcPotConfigGenerator(int noOfServices) {
      this.noOfServices = noOfServices;
      secretSharePoly1 = new long[noOfServices];
      serviceIndices = new short[noOfServices];
      preEvalPoly2 = new BigInteger[noOfServices];
      lpcs = new BigInteger[noOfServices];
      setNoOfBits(DEFAULT_NUM_BITS);
      /* default, calling to set numLimit; */
    }

    /* function to set up coefficients and the prime number */
    private void setupCoeffPrime() {
      int i;
      long largestCoeff = 0;
      coeffOfPoly1 = new long[noOfServices];
      coeffOfPoly2 = new long[noOfServices];
      for (i = 0; i < noOfServices; i++) {
        coeffOfPoly1[i] = (long) 0;
        coeffOfPoly2[i] = (long) 0;
        Random r = new Random();
        coeffOfPoly1[i] = absLong(r.nextLong()) % numLimit + 1;
        coeffOfPoly2[i] = absLong(r.nextLong()) % numLimit + 1;
        if (coeffOfPoly1[i] > largestCoeff)
          largestCoeff = coeffOfPoly1[i];
        if (coeffOfPoly2[i] > largestCoeff)
          largestCoeff = coeffOfPoly2[i];
      }
      bigPrime = new BigInteger(Long.toUnsignedString(largestCoeff)).nextProbablePrime();
    }

    /* Function to evaluate a polynomial without the constant
     * part given the value and coefficients.
     */
    private BigInteger evaluatePoly(int x, long[] coefficients) {
      BigInteger powX,term, precomputeShare = new BigInteger("0");
      powX = new BigInteger("1");
      for (int exp = 1; exp < noOfServices; exp++) {
        powX = powX.multiply(new BigInteger("" + x));
        term = powX.multiply(new BigInteger(Long.toUnsignedString(coefficients[exp])));
        precomputeShare = precomputeShare.add(term);
      }
      precomputeShare = precomputeShare.mod(bigPrime);
      return precomputeShare;
    }

    /* Add secret-key to the evaluated polynomial and prime mod it. */
    private long splitShareIndex(int index) {
      BigInteger bigShare = evaluatePoly(index, coeffOfPoly1);
      bigShare = bigShare.add(new BigInteger(Long.toUnsignedString(coeffOfPoly1[0])));
      bigShare = bigShare.remainder(bigPrime);
      return bigShare.longValue();
    }

    /* calculate Lpc */
    private BigInteger calculateLpc(int index) {
      int count;
      long startPosition, nextPosition;
      BigInteger bigDenom, bigNumerator, bigLpc;

      bigDenom = new BigInteger("1");
      bigNumerator = new BigInteger("1");
      for (count = 0; count < noOfServices; count++) {
        if (index == count)
          continue;
        startPosition = serviceIndices[index];
        nextPosition = serviceIndices[count];
        bigNumerator = bigNumerator.multiply(new BigInteger(Long.toString(-1 * nextPosition)));
        bigNumerator = bigNumerator.mod(bigPrime);
        bigDenom = bigDenom.multiply(new BigInteger(Long.toString(startPosition - nextPosition)));
        bigDenom = bigDenom.mod(bigPrime);
      }
      bigDenom = bigDenom.modInverse(bigPrime);
      bigLpc = bigNumerator.multiply(bigDenom);
      bigLpc = bigLpc.mod(bigPrime);

      return bigLpc;
    }

    /* setup service indices */
    private void setupServiceIndices() {
      for (int i = 0; i < noOfServices; i++) {
        serviceIndices[i] = (short) ((i+1) * 2);
      }
      //shuffle indices
      Random r = new Random();
      for (int i = 0; i < noOfServices; i++) {
        int j = r.nextInt(noOfServices);
        Short temp = serviceIndices[i];
        serviceIndices[i] = serviceIndices[j];
        serviceIndices[j] = temp;
      }
    }

    /* setup secret share for polynomial 1 */
    private void setupSecretSharePoly1() {
      for (int i = 0; i < noOfServices; i++) {
          secretSharePoly1[i] = splitShareIndex(serviceIndices[i]);
      }
    }

    /* setup Lpc */
    private void setupLpcs() {
      for (int i = 0; i < noOfServices; i++) {
          lpcs[i] = calculateLpc(i);
        }
      }

    /* evaluate public polynomial */
    private void setupPreEvalPoly2() {
      for (int i = 0; i < noOfServices; i++) {
          preEvalPoly2[i] = evaluatePoly(serviceIndices[i], coeffOfPoly2);
        }
      }

    /* method to generate configs */
    public void generateScvConfig() {
      setupCoeffPrime();
      setupServiceIndices();
      setupSecretSharePoly1();
      setupLpcs();
      setupPreEvalPoly2();
    }

    /* method to make the first bit 0 of the number to get a positive number */
    private long absLong(long n) {
        n = (n << 1) >>> 1;
        return n;
    }

    /* Get/Set methods */
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
      if (noOfBits > 64) {
          noOfBits = 64;
      }
      this.noOfBits = noOfBits;
      this.numLimit = (long) (Math.pow(2, noOfBits));
    }

    public String getVersion() {
      return VERSION;
    }

    public int getMaxServiceNodes() {
      return MAX_SERVICE_NODES;
    }

    public Long getPrime() {
      return bigPrime.longValue();
    }

    public Long getSecret() {
      return coeffOfPoly1[0];
    }

    public long getNumLimit() {
      return numLimit;
    }

    public Short getServiceIndices(int i) {
      return serviceIndices[i];
    }

    public short[] getServiceIndices() {
      short[] res = new short[serviceIndices.length];
      for(int i=0;i<serviceIndices.length;i++) res[i] = serviceIndices[i];
      return res;
    }

    public long getSecretShare(int i) {
      return secretSharePoly1[i];
    }

    public BigInteger getPublicPoly(int i) {
      return preEvalPoly2[i];
    }

    public void setNumLimit(long numLimit) {
      if (numLimit < 2) {
          numLimit = 2;
      }
      short numofbits = (short) Math.ceil(Math.log(numLimit) / Math.log(2));
      setNoOfBits(numofbits);
    }

    public BigInteger[] getLpcs() {
        BigInteger[] res = new BigInteger[lpcs.length];
        System.arraycopy(lpcs, 0, res, 0, lpcs.length);
        return res;
    }

    public long getCoeff(int i) {
      return coeffOfPoly1[i];
    }

    public BigInteger getLpc(int i) {
      return lpcs[i];
    }
}
