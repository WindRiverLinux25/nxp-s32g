# Copyright 2022, 2024 NXP

DESCRIPTION = "ARM Trusted Firmware Tools"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://license.rst;md5=1dd070c98a281d18d9eefd938729b031"

include atf-s32g_2.10.inc

BBCLASSEXTEND = "native"
DEPENDS += "openssl"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

EXTRA_OEMAKE:class-target += 'CROSS_COMPILE="${TARGET_PREFIX}" CC="${CC} ${CFLAGS} ${LDFLAGS}" HOSTCC="${CC} ${CFLAGS} ${LDFLAGS}" OPENSSL_DIR="${STAGING_DIR}/${prefix}"'
EXTRA_OEMAKE:class-native += 'CC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}" HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}" OPENSSL_DIR="${STAGING_DIR_NATIVE}/"'

do_compile() {
	oe_runmake -C "${S}" fiptool
}

do_install() {
	install -d ${D}${bindir}
	install -m 0755 ${S}/tools/fiptool/fiptool ${D}/${bindir}/
}

FILES:${PN} = "${bindir}/fiptool"

