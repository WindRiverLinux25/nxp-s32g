require ${@bb.utils.contains_any('MACHINE', 'nxp-s32g aptiv-cvc-131', 'linux-yocto-nxp-s32g.inc', '', d)}

KBRANCH:nxp-s32g  = "v6.12/standard/preempt-rt/nxp-sdk-6.6/nxp-s32g"
