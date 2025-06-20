# ATF repository
URL ?= "git://github.com/nxp-auto-linux/arm-trusted-firmware.git;protocol=https"
BRANCH ?= "release/bsp44.0-2.10.14"
SRC_URI = "${URL};branch=${BRANCH}"
SRCREV ?= "c9a7f81230064d21e403cfc315cbd3ffdc18752f"
SRC_URI[sha256sum] = "15d263b62089b46375effede12a1917cd7b267b93dd97c68fd5ddbd1dddede07"

COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
