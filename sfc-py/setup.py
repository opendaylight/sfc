#!/usr/bin/env python
#
# Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html

import sys

from setuptools import setup

__author__ = 'Marcel Sestak'
__email__ = 'msestak@cisco.com'
__version__ = '0.1'
__status__ = 'alpha'


"""
SFC package installer
"""

if sys.platform.startswith('linux'):
    inst_requires = [
        'ipaddress',
        'Flask >= 0.10.1',
        'paramiko >= 1.15.2',
        'pytest >= 2.6.4',
        'requests >= 2.5.1',
        'scapy-python3 >= 0.13',
        'netifaces>=0.10.4',
        'NFQP3',
    ]
else:
    inst_requires = [
        'ipaddress',
        'Flask >= 0.10.1',
        'paramiko >= 1.15.2',
        'pytest >= 2.6.4',
        'requests >= 2.5.1',
        'scapy-python3 >= 0.13',
        'netifaces>=0.10.4',
    ]


def readme(file):
    """
    Read file content to string

    :param file: target file
    :type file: str

    :return str

    """
    # Support for Python 2.xx
    if sys.version_info < (3, 0):
        with open(name=file, mode='r') as _file:
            content = _file.read()
    else:
        with open(file=file, mode='r', encoding='utf-8') as _file:
            content = _file.read()
    return content

setup(
    name='sfc',
    version='0.1.365',
    keywords='sfc nsh nfq',
    description='Service Function Chaining',
    long_description=readme('README.rst'),
    url='https://wiki.opendaylight.org/view/Service_Function_Chaining:Main',
    author='SFC Developers',
    author_email='sfc-dev@lists.opendaylight.org',
    packages=[
        'sfc',
        'sfc.cli',
        'sfc.nsh',
        'sfc.common'
    ],
    install_requires=inst_requires,
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Programming Language :: Python :: 3.4',
        'Operating System :: POSIX :: Linux',
        'Natural Language :: English',
    ]
)
