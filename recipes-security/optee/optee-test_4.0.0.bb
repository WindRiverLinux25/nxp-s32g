require recipes-security/optee/optee-test.inc
require optee-nxp.inc

# Include ffa_spmc test group if the SPMC test is enabled.
# Supported after op-tee v3.20
EXTRA_OEMAKE:append = "${@bb.utils.contains('MACHINE_FEATURES', 'optee-spmc-test', \
                                        ' CFG_SPMC_TESTS=y CFG_SECURE_PARTITION=y', '' , d)}"

RDEPENDS:${PN} += "${@bb.utils.contains('MACHINE_FEATURES', 'optee-spmc-test', \
                                              ' arm-ffa-user', '' , d)}"

LIC_FILES_CHKSUM:nxp-s32g = "file://LICENSE.md;md5=daa2bcccc666345ab8940aab1315a4fa"

SRCREV = "79e49f95474d05a028f58a75c6c643f88e9b9295"

FILESEXTRAPATHS:prepend := "${LAYER_PATH_meta-arm}/recipes-security/optee/${PN}:"
URL:nxp-s32g ?= "git://github.com/nxp-auto-linux/optee_test.git;protocol=https"
BRANCH:nxp-s32g ?= "release/bsp44.0-4.0"
SRC_URI:nxp-s32g= "\
    ${URL};branch=${BRANCH} \
    file://run-ptest \
"

