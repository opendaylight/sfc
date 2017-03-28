#!/usr/bin/python

from subprocess import call
from mininetBase import SFC
from odlConfGeneration import sfcEncap
import sys

if __name__ == "__main__":

    sfc = SFC(sfcEncap.VLAN, sys.argv[1])

    sw1 = sfc.addSw()
    sw2 = sfc.addSw()
    sw3 = sfc.addSw()
    sw4 = sfc.addSw()

    h1 = sfc.addHost(sw1)
    h2 = sfc.addHost(sw1)

    sf1 = sfc.addSf('1', sw2, 'test')
    sf2 = sfc.addSf('2', sw2, 'fw')

    sf4 = sfc.addSf('4', sw3, 'ids')
    sf5 = sfc.addSf('5', sw3, 'dpi')

    sf7 = sfc.addSf('7', sw4, 'optimizer')
    sf8 = sfc.addSf('8', sw4, 'fw2')

    sfc.addLink(sw1, sw2)
    sfc.addLink(sw2, sw3)
    sfc.addLink(sw3, sw4)
    sfc.addLink(sw4, sw1)

    sfc.addGw(sw1)

    chain = ['test', 'fw', 'ids', 'dpi', 'optimizer', 'fw2']

    sfc.addChain('c1', sw1, chain)

    print "start topo"

    sfc.deployTopo()

    sfc.net.stop()