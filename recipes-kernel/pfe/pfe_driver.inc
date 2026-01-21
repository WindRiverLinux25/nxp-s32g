#This is a inclued file for pfeng MASTER/SLAVE drivers

inherit module deploy
require pfe_common.inc

# Provide a default so things will parse.
#
PFE_LOCAL_FIRMWARE_DIR ??= "."

SRC_URI:append = " \
        file://0001-pfe_compiler-add-GCC-version-10.2.0-support.patch \
        file://0001-fix-hwts-kmemleak.patch \
        file://0001-pfe-set-mac_managed_pm-to-true-to-fix-mdio-resume-wa.patch \
        file://0001-pfe-fix-the-lock-inconsistent-warning-when-rmmod-the.patch \
        file://0001-pfe-update-to-fix-the-possible-circular-locking.patch \
        file://0001-pfe-fix-the-kernel-calltrace-when-dhcp-for-pfe-ports.patch \
        file://0001-pfe-slave-add-mutex_unlock-in-pfe_emac_destroy.patch \
	file://0001-pfe-slave-use-new-macro-to-replace-the-oal_mutex_ini.patch \
	file://0001-pfe-slave-extends-the-wait-time.patch \
	file://0001-pfe-phylink-set-phy-interface-mode-to-SGMII-when-in-.patch \
	file://0001-pfeng-update-to-compatible-with-v6.12-kernel.patch \
	file://0001-pfe-remove-packed-attribute-of-LLIST_t-struct-to-fix.patch \
"

S = "${WORKDIR}/git"
MDIR = "${S}/sw/linux-pfeng"
INSTALL_DIR = "${D}/${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/net/ethernet/nxp/pfe"

EXTRA_OEMAKE:append = " KERNELDIR=${STAGING_KERNEL_DIR} MDIR=${MDIR} -C ${MDIR} V=1 drv-build PLATFORM=${HOST_SYS}"
