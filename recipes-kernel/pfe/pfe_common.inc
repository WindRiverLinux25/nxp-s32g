# Copyright 2019-2022 NXP
#
# PFE common file for Master/Slave driver and libfci components

SUMMARY = "Linux driver for the Packet Forwarding Engine hardware"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://LICENSE.txt;md5=5dcdfe25f21119aa5435eab9d0256af7"

URL ?= "git://github.com/nxp-auto-linux/pfeng.git;protocol=https;branch=release/linux_1.10.0"
SRC_URI = "${URL}"


SRCREV = "d41ea8bcbadd1e6ee8ab4b4dc9ae6b11c617a92d"

PATCHTOOL = "git"

COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
