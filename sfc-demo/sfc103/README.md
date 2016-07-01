SFC103 Demo
===========

Overview
--------

SFC103 demo is to show standalone SFC classifier.


Note
----

It takes long time to complete the demo including vagrant box download,
SFC download/build and ovs with NSH installation. The duration depends
On your network. Normally, it takes several hours.

Topology
-------

                           +-----------------+
                           |       SFC       |
                           |   192.168.1.5   |
                           +-----------------+
                       /      |          |     \
                    /         |          |         \
                /             |          |             \
+---------------+  +--------------+   +--------------+  +---------------+
|  Classifier1  |  |    SFF1      |   |     SFF2     |  |  Classifier2  |
|  192.168.1.10 |  | 192.168.1.20 |   | 192.168.1.50 |  |  192.168.1.60 |
+---------------+  +--------------+   +--------------+  +---------------+
                              |          |
                              |          |
                   +---------------+  +--------------+
                   |     DPI-1     |  |     FW-1     |
                   | 192.168.1.30  |  | 192.168.1.40 |
                   +---------------+  +--------------+

Setup Demo
----------
1. Install virtualbox & vagrant in ubuntu 14.04.03
2. ./demo.sh


Cleanup Demo
------------
1. vagrant destroy -f


Trouble Shooting(TBD)
--------------------
