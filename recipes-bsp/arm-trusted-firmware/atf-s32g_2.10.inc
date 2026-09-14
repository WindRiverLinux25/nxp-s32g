# ATF repository
URL ?= "git://github.com/nxp-auto-linux/arm-trusted-firmware.git;protocol=https"
BRANCH ?= "release/bsp46.0-2.10.25"
SRC_URI = "${URL};branch=${BRANCH}"
SRCREV ?= "c834244b9c54c79e80315aecc9807f5b0eb79342"

COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
