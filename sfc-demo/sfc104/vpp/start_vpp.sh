#!/bin/bash

umount /run/hugepages/kvm
mount -t hugetlbfs nodev /run/hugepages/kvm
start vpp
start honeycomb
