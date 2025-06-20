require optee-nxp.inc

MAJ_VER:nxp-s32g = "${@oe.utils.trim_version("${PV}", 2)}"

SRCREV:nxp-s32g = "29575f55f36563c19f6a20001c3914b37436a4c4"

URL:nxp-s32g ?= "git://github.com/nxp-auto-linux/optee_os;protocol=https"
BRANCH:nxp-s32g ?= "${RELEASE_BASE}-${MAJ_VER}"
SRC_URI:nxp-s32g = "\
    ${URL};branch=${BRANCH} \
    file://0003-optee-enable-clang-support.patch \
"
