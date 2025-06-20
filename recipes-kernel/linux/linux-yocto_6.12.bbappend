require ${@bb.utils.contains('MACHINE', 'nxp-s32g', 'linux-yocto-nxp-s32g.inc', '', d)}

KBRANCH:nxp-s32g  = "v6.12/standard/nxp-sdk-6.6/nxp-s32g"
