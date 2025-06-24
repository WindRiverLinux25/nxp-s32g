# Copyright 2019-2021 NXP
#
# This is the PFE driver for Linux kernel 5.4 and 5.10

require pfe_driver.inc

# Dummy entry to keep the recipe parser happy if we don't use this recipe
FW_INSTALL_DIR = "${D}/lib/firmware"
FW_INSTALL_CLASS_NAME ?= "s32g_pfe_class.fw"
FW_INSTALL_UTIL_NAME ?= "s32g_pfe_util.fw"

# Tell yocto not to bother stripping our binaries, especially the firmware
# since 'aarch64-fsl-linux-strip' fails with error code 1 when parsing the firmware
# ("Unable to recognise the format of the input file")
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"


# In case the pfe-slave is built, change to multi instance driver(allow override)
PFE_MASTER_OPTIONS:append = "${@bb.utils.contains('IMAGE_INSTALL', 'pfe-slave', ' PFE_CFG_MULTI_INSTANCE_SUPPORT=1 PFE_CFG_PFE_MASTER=1', '', d)}"
PFE_MASTER_OPTIONS:append = "${@bb.utils.contains('PACKAGE_INSTALL', 'pfe-slave', ' PFE_CFG_MULTI_INSTANCE_SUPPORT=1 PFE_CFG_PFE_MASTER=1', '', d)}"
PFE_MASTER_OPTIONS:append = "${@bb.utils.contains('IMAGE_INSTALL_INITRAMFS', 'pfe-slave', ' PFE_CFG_MULTI_INSTANCE_SUPPORT=1 PFE_CFG_PFE_MASTER=1', '', d)}"
EXTRA_OEMAKE:append = " ${PFE_MASTER_OPTIONS}"


module_do_install() {
	install -D "${MDIR}/pfeng.ko" "${INSTALL_DIR}/pfeng.ko"

	if [ -f ${PFE_LOCAL_FIRMWARE_DIR}/${FW_INSTALL_CLASS_NAME} ];then
		mkdir -p "${FW_INSTALL_DIR}"
		install -D "${PFE_LOCAL_FIRMWARE_DIR}/${FW_INSTALL_CLASS_NAME}" "${FW_INSTALL_DIR}/${FW_INSTALL_CLASS_NAME}"
	fi

	if [ -f ${PFE_LOCAL_FIRMWARE_DIR}/${FW_INSTALL_UTIL_NAME} ];then
		mkdir -p "${FW_INSTALL_DIR}"
		install -D "${PFE_LOCAL_FIRMWARE_DIR}/${FW_INSTALL_UTIL_NAME}" "${FW_INSTALL_DIR}/${FW_INSTALL_UTIL_NAME}"
	fi
}

do_deploy() {
	install -d ${DEPLOYDIR}

	if [ -f ${FW_INSTALL_DIR}/${FW_INSTALL_CLASS_NAME} ];then
		install -m 0644 ${FW_INSTALL_DIR}/${FW_INSTALL_CLASS_NAME} ${DEPLOYDIR}/${FW_INSTALL_CLASS_NAME}
	fi

	if [ -f ${FW_INSTALL_DIR}/${FW_INSTALL_UTIL_NAME} ];then
		install -m 0644 ${FW_INSTALL_DIR}/${FW_INSTALL_UTIL_NAME} ${DEPLOYDIR}/${FW_INSTALL_UTIL_NAME}
	fi
}

addtask do_deploy after do_install

# do_package_qa throws error "QA Issue: Architecture did not match"
# when checking the firmware
do_package_qa[noexec] = "1"
do_package_qa_setscene[noexec] = "1"

FILES:${PN} += "/lib/firmware/${FW_INSTALL_CLASS_NAME} \
    /lib/firmware/${FW_INSTALL_UTIL_NAME} \
    ${sysconfdir}/modules-load.d/* \
"


