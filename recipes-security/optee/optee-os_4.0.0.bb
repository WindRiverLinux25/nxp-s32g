require recipes-security/optee/optee-os.inc
require optee-nxp.inc

DEPENDS += "dtc-native"

FILESEXTRAPATHS:prepend := "${LAYER_PATH_meta-arm}/recipes-security/optee/${PN}:${THISDIR}/${PN}:"

SRCREV = "5bc04d55cc67a75824cad7a8ee4b29090ee11187"

URL:nxp-s32g ?= "git://github.com/nxp-auto-linux/optee_os;protocol=https"
BRANCH:nxp-s32g ?= "release/bsp44.0-4.0"
SRC_URI:nxp-s32g = "\
    ${URL};branch=${BRANCH} \
    file://0001-allow-setting-sysroot-for-libgcc-lookup.patch \
    file://0002-core-Define-section-attributes-for-clang.patch \
    file://0003-optee-enable-clang-support.patch \
    file://0004-core-link-add-no-warn-rwx-segments.patch \
    file://CVE-2025-46733-nxp-s32g.patch \
"
