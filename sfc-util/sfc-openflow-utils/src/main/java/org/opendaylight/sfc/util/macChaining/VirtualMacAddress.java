/*
 * Copyright (c) 2016 Hewlett Packard Enterprise Development LP. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.macChaining;


import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;

import java.util.*;

/*  VirtualMAC format
     48              24  23   22                     0
     +-----------------+----+----+------+-----+------+
     |       OUI       | R  | B  | PORT | CID | SFID |
     +-----------------+----+----+------+-----+------+
*/
public final class VirtualMacAddress {

    // Configuration values
    private static final long BASE_OUI = 0xF00000000000L;
    private static final int FLAGS_LEN = 2;
    private static final int PORT_LEN = 6;
    private static final int CID_LEN = 8;

    public static final int SFID_LEN;
    private static final int MAX_FLAGS;
    private static final int MAX_PORT;
    private static final int MAX_CID;

    private static final Map<UUID, Integer> MAPPING;

    private static final Object LOCKER = new Object();
    private static final Deque<Integer> POOL;
    private static final boolean IN_USE[];

    private final int step = 1;

    static {
        MAPPING = new HashMap<>();

        int bits_used = PORT_LEN + CID_LEN + FLAGS_LEN;
        int bits_remaining = 24 - bits_used;
        if (bits_remaining <= 1)
            throw new IllegalArgumentException(String.format(
                    "The sum of PORT_LEN and CID_LEN must be lower than %d",
                    24 - FLAGS_LEN
            ));

        SFID_LEN = bits_remaining;


        // TODO: Load database information and exclude the IDs already used
        MAX_CID = (int) Math.pow(2, CID_LEN);
        IN_USE = new boolean[MAX_CID];
        POOL = new LinkedList<>();
        for (int i = 0; i < MAX_CID; i++)
            POOL.add(i);

        MAX_FLAGS = (int) Math.pow(2, FLAGS_LEN);
        MAX_PORT = (int) Math.pow(2, PORT_LEN);
    }

    private static int reserveId() {
        synchronized (LOCKER) {
            if (POOL.isEmpty())
                throw new NoSuchElementException("No more ids available (are you calling release()?)");

            int id = POOL.pop();
            IN_USE[id] = true;
            return id;
        }
    }

    private static void releaseId(int id) {
        synchronized (LOCKER) {
            if (id < 0 || id >= MAX_CID)
                throw new IllegalArgumentException(String.format("Id must be in between 0 and %d", MAX_CID));

            if (!IN_USE[id])
                throw new IllegalArgumentException(String.format("Id %d was not previously reserved", id));

            IN_USE[id] = false;
            POOL.push(id);
        }
    }

    public static int getChainIdFor(UUID uuid) {
        synchronized (MAPPING) {
            Integer id = MAPPING.get(uuid);
            if (id == null) {
                id = reserveId();
                MAPPING.put(uuid, id);
            }

            return id;
        }
    }

    public static VirtualMacAddress getForwardAddress(long uuid, long port) {
        UUID id = new UUID(0, uuid);
        return new VirtualMacAddress(BI_FORWARD, port, getChainIdFor(id));
    }

    public static VirtualMacAddress getBackwardAddress(long uuid, long port) {
        UUID id = new UUID(0, uuid);
        return new VirtualMacAddress(BI_BACKWARD, port, getChainIdFor(id));
    }

    public final static int BI_BACKWARD = 0x400000;
    public final static int BI_FORWARD = 0x000000;
    public final static int REVERSE = 0x800000;


    private byte[] macAddr;
    private final int chain_id;
    private final int flags;
    private final int port;

    public VirtualMacAddress(int flags, long port, int chain_id) {
        if (port >= MAX_PORT || port < 0)
            throw new IllegalArgumentException(String.format(
                    "Invalid port value. Valid values are between 0 and %d",
                    MAX_PORT
            ));

        final int flags_mask = (MAX_FLAGS - 1) << (24 - FLAGS_LEN);
        if ((flags & (~flags_mask)) != 0)
            throw new IllegalArgumentException(String.format(
                    "Invalid flags composition: 0x%06x",
                    flags
            ));

        this.chain_id = chain_id;
        this.port = (int) port;
        this.flags = flags;
        this.macAddr = toByte(
                        BASE_OUI                        |
                        flags                           | // Flag bits: Reversed + Bidirectional
                        port << (CID_LEN + SFID_LEN)    |
                        chain_id << (SFID_LEN)          |
                        (int) (Math.pow(2, SFID_LEN) - 1) // All bits 1
        );

    }

    public int getChainId() {
        return chain_id;
    }

    private MacAddress getMacAddr(byte[] hop) {
        return new MacAddress(toString(hop));
    }

    public MacAddress getHop(short index) {
        byte[] hop = toByte(toLong() + index);
        return getMacAddr(hop);
    }

    public long getEmbeddedPort() {
        return port;
    }

    public void release() {
        releaseId(chain_id);
    }

    //TODO: not tested yet
    private byte[] toByte (long address) {
        byte[] addressInBytes = new byte[]{
                (byte) ((address >> 40) & 0xff),
                (byte) ((address >> 32) & 0xff),
                (byte) ((address >> 24) & 0xff),
                (byte) ((address >> 16) & 0xff),
                (byte) ((address >> 8) & 0xff),
                (byte) ((address >> 0) & 0xff)
        };
        return addressInBytes;
    }

    private String toString (byte[] hop) {

        StringBuilder builder = new StringBuilder();
        for (byte b: hop) {
            if (builder.length() > 0) {
                builder.append(":");
            }
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }

    private long toLong() {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            long t = (macAddr[i] & 0xffL) << ((5 - i) * 8);
            mac |= t;
        }
        return mac;
    }
}
