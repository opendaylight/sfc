
.. contents:: Table of Contents
   :depth: 3

===============================
Directional data plane locators
===============================

[gerrit filter: https://git.opendaylight.org/gerrit/#/q/topic:directional-dpl]

This specification proposes to have an optional direction associated
with data plane locators.

Problem description
===================
Service functions that are not SFC encapsulation aware need to use
alternative mechanisms to recognize and apply SFC features. One common
alternative is to use multiple interfaces. For example, each interface
could be associated exclusively to the ingress traffic of a specific
service function path.

Service functions for which this concept will most commonly apply are
those network devices that operate as 'bump in the wire', where two
interfaces acting as ingress and egress interfaces for traffic in one
direction, would be the egress and ingress interfaces respectively in
the opposite direction.

Currently, the connection between a service function and a service
function forwarder is described by single pair of data plane locators,
one for each side of the connection. For symmetric service chains, the
same locator pair is used when rendering the forward and reverse paths.
This prevents the possibility to apply the mechanism explained
previously.

The purpose of this specification is to overcome this first limitation,
enabling the support of bump in the wire service functions that are sfc
aware, understood as service functions that allow to employ inline
mechanisms to steer traffic (mac chaining, nsh, vlan, mpls...), versus
bump in the wire service functions that are sfc unaware and thus might
not allow any modification over the source traffic.

Use Cases
---------
Support 'bump in the wire' network devices that can be made sfc aware to
act as service functions.

Proposed change
===============
The proposed solution is to add the flexibility to configure two pairs
of data plane locators to describe the association between the service
function and service function forwarder. Each pair will be assigned a
direction, either forward or reverse, and will be used as ingress for
paths on the same direction and as egress from paths on the opposite
direction. The forward pair will be used to ingress traffic to the
service function on forward paths and for the egress traffic from the
service function on reverse paths. The reverse pair will be used for
the egress traffic from the service function for the forward path and
to ingress the traffic to the service function for the reverse path.
This can seen in the following figure:

::

                        +-----------------------------------------------+
                        |                                               |
                        |                                               |
                        |                      SF                       |
                        |                                               |
                        |  sf-forward-dpl                sf-reverse-dpl |
                        +--------+-----------------------------+--------+
                                 |                             |
                         ^       |      +              +       |      ^
                         |       |      |              |       |      |
                         |       |      |              |       |      |
                         +       |      +              +       |      +
                    Forward Path | Reverse Path   Forward Path | Reverse Path
                         +       |      +              +       |      +
                         |       |      |              |       |      |
                         |       |      |              |       |      |
                         |       |      |              |       |      |
                         +       |      v              v       |      +
                                 |                             |
                     +-----------+-----------------------------------------+
      Forward Path   |     sff-forward-dpl               sff-reverse-dpl   |   Forward Path
    +--------------> |                                                     | +-------------->
                     |                                                     |
                     |                         SFF                         |
                     |                                                     |
    <--------------+ |                                                     | <--------------+
      Reverse Path   |                                                     |   Reverse Path
                     +-----------------------------------------------------+


The SFF-SF dictionary will be expanded to accommodate two pairs of
locators as depicted in `Yang changes`_. Renderers that
support this feature shall give precedence to the new directional
locators over the old locators. Once all renderers support the feature,
the single locator pair may be deprecated or may remain as a simple way
to configure a bidirectional locator.

Logical SFF configuration model needs to change as it does not
currently use the SFF-SF dictionary. Logical SFF configuration model
uses a placeholder service function forwarder configuration. The
proposal is to align the logical SFF configuration model with a
standard SFC configuration, otherwise two different models will need to
be continuously proposed for every other feature, causing more
divergence over time. As depicted in `REST API`_, logical interfaces
shall be configured as locators both on the SF and the SFF side.

The implementation targets the openflow renderer. The openflow
processor will provide to the transport specific processor the
appropriate data plane locator pair based on the direction of the path
being rendered.

Also in Logical SFF context, service binding will be performed on the
service function forwarder logical interfaces as soon as intervenes on
a path. On egress towards the service function, egress actions will be
requested from genius for the interface provided by the openflow
processor.

It is worth mentioning that the proposed change may not be enough to
fully support legacy 'bump in the wire' network devices that are sfc
unaware acting as service functions. For this, it might be additionally
needed to:

* Provide the service function with the original unmodified source
  traffic.
* And as a consequence, on service function egress, reclassify the
  traffic to a path based on the service function forwarder ingress
  port.
* And as a consequence, avoid using that port as ingress for more than
  one path.

Directional data plane locators is a step towards 'bump in the wire'
full support and useful in itself for those service functions that
while operating in this mode, are sfc aware in that they allow to use
already supported mechanisms (mac chaining, nsh...) to steer SFC
traffic.

Pipeline changes
----------------
The existing OpenFlow pipeline will not be affected by this change.

Yang changes
------------
The following data model is the updated service-function-dictionary
within the service function forwarder.

.. code-block:: none
   :caption: service-function-forwarder.yang

        list service-function-dictionary {
            key "name";
            leaf name {
              type sfc-common:sf-name;
              description
                  "The name of the service function.";
            }
            container sff-sf-data-plane-locator {
              description
                "SFF and SF data plane locators to use when sending
                 packets from this SFF to the associated SF";
              leaf sf-dpl-name {
                type sfc-common:sf-data-plane-locator-name;
                description
                  "The SF data plane locator to use when sending
                   packets to the associated service function.
                   Used both as forward and reverse locators for
                   paths of a symmetric chain.";
              }
              leaf sff-dpl-name {
                type sfc-common:sff-data-plane-locator-name;
                description
                  "The SFF data plane locator to use when sending
                   packets to the associated service function.
                   Used both as forward and reverse locators for
                   paths of a symmetric chain.";
              }
              leaf sf-forward-dpl-name {
                type sfc-common:sf-data-plane-locator-name;
                description
                  "The SF data plane locator to use when sending
                   packets to the associated service function
                   on the forward path of a symmetric chain";
              }
              leaf sf-reverse-dpl-name {
                type sfc-common:sf-data-plane-locator-name;
                description
                  "The SF data plane locator to use when sending
                   packets to the associated service function
                   on the reverse path of a symmetric chain";
              }
              leaf sff-forward-dpl-name {
                type sfc-common:sff-data-plane-locator-name;
                description
                  "The SFF data plane locator to use when sending
                   packets to the associated service function
                   on the forward path of a symmetric chain.";
              }
              leaf sff-reverse-dpl-name {
                type sfc-common:sff-data-plane-locator-name;
                description
                  "The SFF data plane locator to use when sending
                   packets to the associated service function
                   on the reverse path of a symmetric chain.";
              }
            }
        }

Logical interface locator support is also added to the service function
forwarder data plane locator.

.. code-block:: none
   :caption: service-function-forwarder-logical.yang

        augment "/sfc-sff:service-function-forwarders/"
              + "sfc-sff:service-function-forwarder/"
              + "sfc-sff:sff-data-plane-locator/"
              + "sfc-sff:data-plane-locator/"
              + "sfc-sff:locator-type/" {
          description "Augments the Service Function Forwarder to allow the use of logical
                      interface locators";
          case logical-interface {
            uses logical-interface-locator;
          }
        }

A new leaf is added to the rendered service path model to flag reverse
paths.

.. code-block:: none
   :caption: rendered-service-path.yang

        leaf reverse-path {
          type boolean;
          mandatory true;
          description
            "True if this path is the reverse path of a symmetric
             chain.";
        }

Configuration impact
--------------------
New optional parameters are added to the SFF-SF dictionary. These new
parameters may not be configured in which case behavior is not changed.

The new flag introduced in the rendered service path model does not
have configuration impact as the entity is not meant to be configured.

Logical SFF configuration model will change. Both, previous and new
configuration models will be supported.

Thus backward compatibility is preserved despite the introduced
changes.

Clustering considerations
-------------------------
Clustering support will not be affected by this change.

Other Infra considerations
--------------------------
None.

Security considerations
-----------------------
None.

Scale and Performance Impact
----------------------------
None.

Targeted Release
----------------
This feature is targeted for the Oxygen release.

Alternatives
------------
One first consideration is that if one SF interface required two data
plane locators, two SF interfaces is going to require four data plane
locators to be fully described, specially considering a scenario
where we would like to explicitly configure different openflow ports on
the SFF side for each direction. The proposed solution leverages the
fact that the data plane locators are already contained in lists on
both the SFF and the SF.

One alternative is to introduce a new locator type that serves as an
indirection through which the names of forward and reverse locators can
be specified. Thus three locators are required total for each side of
the SFF-SF association: one forward locator, one reverse locator and
the new locator that tells which is which, whose name would be used in
the SFF-SF dictionary. The advantage is that the SFF configuration model
needs not to be changed. As disadvantages, it needs an extra data plane
locator on each side, it might be confusing being able to specify
different SFF names and transport types between the three locators on
SF side, and finally, the indirection overall leads to a less explicit
model/api and code wise it would probably require to hide all locator
checks or manipulation behind helper code.

Another option is to expand the key of the SFF-SF dictionary to include
direction so that two dictionary entries can be specified for each
SFF/SF pair. This was discarded because is not backward compatible.

Usage
=====

Features to Install
-------------------
All changes will be in the following existing Karaf features:

* odl-sfc-genius
* odl-sfc-openflow-renderer

REST API
--------
The following JSON shows how the service function and service function
forwarder are configured in the context of Logical SFF with directional
locators.

.. code-block:: rest

    URL: http://localhost:8181/restconf/config/service-function:service-functions/

    {
      "service-functions": {
        "service-function": [
          {
            "name": "firewall-1",
            "type": "firewall",
            "sf-data-plane-locator": [
              {
                "name": "firewall-ingress-dpl",
                "interface-name": "eccb57ae-5a2e-467f-823e-45d7bb2a6a9a",
                "transport": "service-locator:mac",
                "service-function-forwarder": "sfflogical1"
              },
              {
                "name": "firewall-egress-dpl",
                "interface-name": "df15ac52-e8ef-4e9a-8340-ae0738aba0c0",
                "transport": "service-locator:mac",
                "service-function-forwarder": "sfflogical1"
              }
            ]
          }
        ]
      }
    }

.. code-block:: rest

    URL: http://localhost:8181/restconf/config/service-function-forwarder:service-function-forwarders/

    {
      "service-function-forwarders": {
        "service-function-forwarder": [
          {
            "name": "sfflogical1",
            "sff-data-plane-locator": [
              {
                "name": "firewall-ingress-dpl",
                "data-plane-locator": {
                  "interface-name": "df15ac52-e8ef-4e9a-8340-ae0738aba0c0",
                  "transport": "service-locator:mac"
                }
              },
              {
                "name": "firewall-egress-dpl",
                "data-plane-locator": {
                  "interface-name": "eccb57ae-5a2e-467f-823e-45d7bb2a6a9a",
                  "transport": "service-locator:mac"
                }
              }
            ],
            "service-function-dictionary": [
              {
                "name": "firewall-1",
                "sff-sf-data-plane-locator":
                {
                  "sf-forward-dpl-name": "firewall-ingress-dpl",
                  "sf-reverse-dpl-name": "firewall-egress-dpl",
                  "sff-forward-dpl-name": "firewall-egress-dpl",
                  "sff-reverse-dpl-name": "firewall-ingress-dpl",
                }
              }
            ]
          }
        ]
      }
    }

CLI
---
No new CLI commands will be added but the existing ones will be
enhanced to display more details about the associations between service
function and service function forwarders.

Implementation
==============

Assignee(s)
-----------
Primary assignee:

*  Jaime Caamaño, #jaicaa, jcaamano@suse.com

Work Items
----------
* Update the service function forwarder yang model.
* Update odl-sfc-genius to bind on service function forwarder
  interfaces.
* Update odl-openflow-renderer processor and surrounding utilities to
  use the proper data plane locator based on the direction of the path.
* Update provider to set the reverse flag on reverse rendered service
  paths.
* Update the shell command for service functions and service
  function forwarders to display the associations between them.
* Update CSIT Full Deploy to use new Logical SFF configuration model.
* Update the user & developer guide to document directional data plane
  locators.
* Update the user & developer guide to reflect the new Logical SFF
  configuration model.

Dependencies
============
The following projects currently depend on SFC:

* GBP
* Netvirt

No backward incompatible changes are introduced but these projects, as
neutron implementations, are target users for the new feature in
Logical SFF scenario.

Testing
=======

Unit Tests
----------
Unit tests will be added for new code introduced through this feature.

Integration Tests
-----------------
None.

CSIT
----
Existing Full Deploy CSIT will be updated to use the new Logical SFF
configuration model.

Documentation Impact
====================
Both the User Guide and Developer Guide will need to be updated.

Open Issues
===========

* Drop support for the old Logical SFF configuration model?
* New CSIT tests not proposed yet because it requires testing with
  traffic, which we don't currently have and is a major undertaking on
  itself.

References
==========
NA
