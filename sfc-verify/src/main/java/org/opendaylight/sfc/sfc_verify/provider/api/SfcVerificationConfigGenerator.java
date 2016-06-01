/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sfc_verify.provider.api;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

/**
 * This class is used for configuration generation for SFC verification.
 *
 * @author Sagar Srivastav (sagsriva@cisco.com)
 * @version 0.2
 * @see org.opendaylight.sfc.sfc_verify.provider.api.SfcVerificationConfigGenerator
 * @since 2016-05-01
 */
public class SfcVerificationConfigGenerator {

    final static String VERSION = "0.5";
    final static int MAX_SERVICE_NODES = 100;
    final static short DEFAULT_NUM_LIMIT = 60;    //number of bits

    private short noOfBits;
    private int noOfServices = -1;
    private long numLimit = 0;
    public Long[] secretSharePoly1;
    private Long[] coeffOfPoly1;
    private Long[] coeffOfPoly2;
    private BigInteger bigPrime;
    private Short[] serviceIndices;
    private BigInteger[] preEvalPoly2;
    private BigInteger[] lpcs;

    public SfcVerificationConfigGenerator(int noOfServices) {
      this.noOfServices = noOfServices;
      secretSharePoly1 = new Long[noOfServices + 1];
      serviceIndices = new Short[noOfServices + 1];
      preEvalPoly2 = new BigInteger[noOfServices + 1];
      lpcs = new BigInteger[noOfServices + 1];
      setNoOfBits(DEFAULT_NUM_LIMIT);                                    //default, calling to set numLimit;
    }

    void setupCoeffPrime() {
      int i;
      long largestCoeff = 0;
      BigInteger largestCoeffP;
      coeffOfPoly1 = new Long[noOfServices];
      coeffOfPoly2 = new Long[noOfServices];
      for (i = 0; i < noOfServices; i++) {
        coeffOfPoly1[i] = (long) 0;
        coeffOfPoly2[i] = (long) 0;
        Random r = new Random();
        coeffOfPoly1[i] = (long) (numLimit <= 1 ? absLong(r.nextLong()) : absLong(r.nextLong()) % numLimit) + 1;
        coeffOfPoly2[i] = (long) (numLimit <= 1 ? absLong(r.nextLong()) : absLong(r.nextLong()) % numLimit) + 1;
        if (coeffOfPoly1[i] > largestCoeff)
          largestCoeff = coeffOfPoly1[i];
        if (coeffOfPoly2[i] > largestCoeff)
          largestCoeff = coeffOfPoly2[i];
      }

      largestCoeffP = new BigInteger(Long.toUnsignedString(largestCoeff));
      bigPrime = largestCoeffP.nextProbablePrime();
    }

    BigInteger precomputeSplitShare(int x, int n, Long[] coefficients) {

      int exp = 1;
      BigInteger powX,term, precomputeShare = new BigInteger("0");;
      powX = new BigInteger("1");
      for (exp = 1; exp < n; exp++) {
        powX = powX.multiply(new BigInteger("" + x));
        term = powX.multiply(new BigInteger(Long.toUnsignedString(coefficients[exp])));
        precomputeShare = precomputeShare.add(term);
      }
      precomputeShare = precomputeShare.mod(bigPrime);
      return precomputeShare;
    }

    long splitShareIndex(int index, int n, Long[] coefficients) {
      BigInteger bigShare;
      long share = 0;

      bigShare = precomputeSplitShare(index, n, coefficients);
      bigShare = bigShare.add(new BigInteger(Long.toUnsignedString(coefficients[0])));

      bigShare = bigShare.remainder(bigPrime);

      share = bigShare.longValue();
      return (share);
    }

    BigInteger calculateLpc(int index, Short[] serviceIndices, int n) {
      int count;
      long startPosition, nextPosition;
      BigInteger bigDenom, bigNumerator, bigLpc;

      bigDenom = new BigInteger("1");
      bigNumerator = new BigInteger("1");
      for (count = 1; count <= n; count++) {
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

    void setupServiceIndices() {
      int i, j;
      j = 0;

      for (i = 1; i <= noOfServices; i++) {
        serviceIndices[i] = (short) ((i) * 2);
      }
      Random r = new Random();
      for (i = noOfServices; i > 0; i--) {
        j = r.nextInt(noOfServices) + 1;
        Short temp = serviceIndices[i];
        serviceIndices[i] = serviceIndices[j];
        serviceIndices[j] = temp;
      }
    }

    void setupSecretSharePoly1(Short[] serviceIndices) {
      int i;
      long share;

      for (i = 1; i <= noOfServices; i++) {
        share = splitShareIndex(serviceIndices[i], noOfServices, coeffOfPoly1);
        secretSharePoly1[i] = share;
      }
    }

    void generateScvConfig() {

      setupCoeffPrime();
      setupServiceIndices();
      setupSecretSharePoly1(serviceIndices);
      ArrayList<BigInteger[]> arr = scPrecompute(lpcs, serviceIndices, preEvalPoly2);
      lpcs = arr.get(0);
      preEvalPoly2 = arr.get(1);
    }

    long absLong(long n) {

        n = (n << 1) >>> 1;
        return n;
    }

    ArrayList<BigInteger[]> scPrecompute(BigInteger[] lpcs, Short[] index, BigInteger[] presplitRandom) {
        int i;
        for (i = 1; i <= noOfServices; i++) {
          lpcs[i] = calculateLpc(i, index, noOfServices);
          presplitRandom[i] = precomputeSplitShare(index[i],noOfServices, coeffOfPoly2);
        }

        ArrayList<BigInteger[]> arr = new ArrayList<>();
        arr.add(0, lpcs);
        arr.add(1, presplitRandom);
        return arr;

      }

    /************ getters and setters ************/

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
          System.out.println("A maximum of 64 bits allowed, setting to 64");
          this.noOfBits = 64;
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

    public Long getPrime() {
      Long res = bigPrime.longValue();
      return res;
    }

    public Long getSecret() {
      Long res = Long.valueOf(coeffOfPoly1[0]);
      return res;
    }

    public long getNumLimit() {
      return numLimit;
    }

    public Short getServiceIndices(int i) {
      Short res = Short.valueOf(serviceIndices[i + 1]);
      return res;
    }

    public Short[] getServiceIndices() {
      Short[] res = new Short[serviceIndices.length];
      for(int i=0; i < res.length; i++) res[i] = Short.valueOf(serviceIndices[i]);
      return res;
    }

    public Long getSecretShare(int i) {
      Long res = Long.valueOf(secretSharePoly1[i + 1]);
      return res;
    }

    public BigInteger getPublicPoly(int i) {
      BigInteger res = new BigInteger(preEvalPoly2[i].toString());
      return res;
    }

    public void setNumLimit(long numLimit) {
      if (numLimit <= 1) {
          System.out.println("numLimit should be greater than 1");
          return;
      }
      this.numLimit = numLimit;
      short numofbits = (short) Math.ceil(Math.log(numLimit) / Math.log(2));
      if (numofbits > 64) {
          System.out.println("A maximum of 64 bits allowed, setting to 64");
          setNoOfBits(numofbits);
          return;
      }
    }

    public BigInteger[] getLpcs() {
      BigInteger[] res = new BigInteger[lpcs.length];
      for(int i=0; i<res.length; i++)
          res[i] = new BigInteger(lpcs[i].toString());

      return res;
    }

    public Long getCoeff(int i) {
      //TODO:FIX:Long res = coeff[i];
      Long res = Long.valueOf(0);
      return res;
    }

  }
