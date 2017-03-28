#! /usr/bin/env python
from scapy.all import *
import time
import fcntl, socket, struct
from subprocess import call

print 'starting service function ...'
interface = sys.argv[1]
vlanTag = None
if len(sys.argv) > 2:
    vlanTag = sys.argv[2]
    print 'tag = ' + vlanTag

def get_mac(iface=interface):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    info = fcntl.ioctl(s.fileno(), 0x8927,  struct.pack('256s', iface[:15]))
    return ':'.join(['%02x' % ord(char) for char in info[18:24]])

mac = get_mac()
print 'mac = ' + mac


def pkt_incoming(pkt):
    return pkt.dst == mac  and 'IP' in pkt #and 'UDP' in pkt

def pkt_replay(pkt):
    print ("Input:")
    pkt.show()
    send_pkt(pkt)
    print "-------------------------------------"

def send_pkt(pkt):
    e, i, = pkt, pkt['IP']
    p = Ether(src=e.dst, dst=e.src)/i
    sendp(p, iface=interface)

sniff(lfilter=pkt_incoming, store=0, iface=interface, prn=pkt_replay)


