# Copyright 2019-2020 NXP

DESCRIPTION = "ARM Trusted Firmware"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://license.rst;md5=1dd070c98a281d18d9eefd938729b031"

DEPENDS += "dtc-native xxd-native bc-native u-boot-tools-native openssl-native"
DEPENDS += "${@ 'u-boot-tools-scmi-native' if d.getVar('SCMI_DTB_NODE_CHANGE') == 'true' else ''}"
DEPENDS += "${@bb.utils.contains('ATF_SIGN_ENABLE', '1', 'mbedtls', '', d)}"

S = "${WORKDIR}/git"
B = "${WORKDIR}/build"

include atf-s32g_2.10.inc

FILESEXTRAPATHS:prepend := "${THISDIR}/files:" 
SRC_URI += " \
    file://0001-Makefile-Add-BUILD_PLAT-to-FORCE-s-order-only-prereq.patch \
    file://0001-s32g-evb-usb-remove-usb-phy-device-node.patch \
    file://0001-s32-clk-Return-the-preset-freq-when-we-can-t-calcula.patch \
"

PATCHTOOL = "git"
PLATFORM = "s32g2xxaevb s32g274ardb2 s32g399ardb3 s32g3xxaevb"
BUILD_TYPE = "release"

ATF_S32G_ENABLE = "1"

HSE_BUILD_OPT = "HSE_SUPPORT"
RSA_PRIV_FIP ?= "${B}/${HSE_SEC_KEYS}/${HSE_SEC_PRI_KEY}"

RSA_PRIV_BL2 ??= ""
RSA_PRIV_BL31 ??= ""
RSA_PRIV_BL32 ??= ""
RSA_PRIV_BL33 ??= ""
RSA_PRIV_DDRFW ??= ""
BL2_HANDLE ??= ""
BL31_HANDLE ??= ""
BL32_HANDLE ??= ""
BL33_HANDLE ??= ""
DDRFW_HANDLE ??= ""

DDR_FW_PATH ?= ""

HSE_ARGS = " \
              HSE_SUPPORT=1 \
              "

SECBOOT_ARGS = " \
                 SECBOOT_SUPPORT=1 \
                 BL31_HSE_KEYHANDLE=${BL31_HANDLE} \
                 BL32_HSE_KEYHANDLE=${BL32_HANDLE} \
                 BL33_HSE_KEYHANDLE=${BL33_HANDLE} \
                 DDRFW_HSE_KEYHANDLE=${DDRFW_HANDLE} \
                 MBEDTLS_DIR=${RECIPE_SYSROOT}/usr/share/mbedtls-source \
                 "

EXTRA_OEMAKE += " \
                CROSS_COMPILE=${TARGET_PREFIX} \
                ARCH=${TARGET_ARCH} \
                BUILD_BASE=${B} \
                "

M7BOOT_ARGS = " FIP_OFFSET_DELTA=0x2000"
EXTRA_OEMAKE += "${@bb.utils.contains('MACHINE_FEATURES', 'm7_boot', '${M7BOOT_ARGS}', '', d)}"

# FIXME: Allow linking of 'tools' binaries with native libraries
#        used for generating the boot logo and other tools used
#        during the build process.
EXTRA_OEMAKE += 'HOSTCC="${BUILD_CC} ${BUILD_CPPFLAGS} ${BUILD_LDFLAGS}" \
                 HOSTLD="${BUILD_LD}" \
                 OPENSSL_DIR="${STAGING_DIR_NATIVE}/${prefix_native}" \
                 LIBPATH="${STAGING_LIBDIR_NATIVE}" \
                 HOSTSTRIP=true'

SCPRT_ARGS = " \
    S32CC_USE_SCP=1 \
    FIP_ALIGN=64 \
"
EXTRA_OEMAKE += "${@bb.utils.contains('MACHINE_FEATURES', 'srm', '${SCPRT_ARGS}', '', d)}"

# Switch to SCMI versions for pinctrl and NVMEM if it's the case
EXTRA_OEMAKE += "S32CC_USE_SCMI_PINCTRL=${SCMI_USE_SCMI_PINCTRL}"
EXTRA_OEMAKE += "S32CC_USE_SCMI_NVMEM=${SCMI_USE_SCMI_NVMEM}"

PINCTRL_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_PINCTRL', '1', '--pinctrl', '--no-pinctrl', d)}"
GPIO_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_GPIO', '1', '--gpio', '--no-gpio', d)}"
NVMEM_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_NVMEM', '1', '--nvmem', '--no-nvmem', d)}"

EXTRA_OEMAKE += "${@['', '${HSE_ARGS}']['s32g' in d.getVar('MACHINE') and d.getVar('HSE_SEC_ENABLED') == '1']}"
EXTRA_OEMAKE += "${@['', '${SECBOOT_ARGS}']['s32g' in d.getVar('MACHINE') and d.getVar('ATF_SIGN_ENABLE') == '1']}"

# Fix the dtc compile issue if SRM enabled
do_compile:prepend() {

    if ${SCMI_DTB_NODE_CHANGE}; then
        for hdr in ${STAGING_DIR_HOST}/sysroot-only/plat-hdrs/*; do
            bname="$(basename ${hdr})"
            lhdr=$(find "${S}/include" -name "${bname}")

            if [ -z "${lhdr}" ]; then
                bbfatal_log "Failed to locate ${bname} header file"
            fi

            if ! diff -NZbau "${hdr}" "${lhdr}"; then
                bbfatal_log "There is a difference in the SCMI header content between TF-A and the Linux repository (${hdr} vs ${lhdr})."
            fi
        done
    fi
}

do_compile() {
    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    oe_runmake -C "${S}" clean

    for type in ${BOOT_TYPE}; do
        for plat in ${PLATFORM}; do
            build_base="${B}/$type/"
            ATF_BINARIES="${B}/$type/${plat}/${BUILD_TYPE}"
            bl33_dir="${DEPLOY_DIR_IMAGE}/${plat}_${type}"
            fip_location="FIP_LOCATION=$type"
            dtb=""
            for tmp in ${DTB_FILES}; do
                name=`echo $tmp | sed 's/-//' | cut -d . -f1`
                if [ "$name" = "$plat" ]; then
                   dtb=$tmp
                   break
                fi
            done
            [ -z "$dtb" ] && dtb="$plat.dtb"
            if [ "$type" = "sd" ]; then
                bl33_dir="${DEPLOY_DIR_IMAGE}/${plat}"
                fip_location="FIP_LOCATION=mmc"
            fi
            bl33_bin="${bl33_dir}/${UBOOT_BINARY}"
            uboot_cfg="${bl33_dir}/${UBOOT_CFGOUT}"
            if [ "${ATF_SIGN_ENABLE}" = "1" ]; then
                hse_fw_dir="NXP_HSE_FWDIR=${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G3}"
                if echo $plat | grep -q s32g2; then
                    hse_fw_dir="NXP_HSE_FWDIR=${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G2}"
                fi
            fi

            if ${@bb.utils.contains('DISTRO_FEATURES', 'optee', 'true', 'false', d)}; then
                optee_plat="$(echo $plat | cut -c1-5)"
                optee_arg="BL32=${DEPLOY_DIR_IMAGE}/optee/$optee_plat/tee-header_v2.bin \
			BL32_EXTRA1=${DEPLOY_DIR_IMAGE}/optee/$optee_plat/tee-pager_v2.bin \
			SPD=opteed"
            fi

            bl_keys=""
            if [ "${ATF_SIGN_ENABLE}" = "1" ]; then
                bl_keys=" BL2_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL2} \
                          BL31_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL31} \
                          BL32_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL32} \
                          BL33_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL33} \
                          DDRFW_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_DDRFW} \
                         "
            fi

            oe_runmake -C ${S} DTB_FILE_NAME=${dtb} BUILD_BASE=$build_base PLAT=${plat} BL33=$bl33_bin BL33DIR=$bl33_dir MKIMAGE_CFG=$uboot_cfg MKIMAGE=mkimage $optee_arg $hse_fw_dir $fip_location $bl_keys DDR_FW_BIN_PATH=${DDR_FW_PATH} all

            if ${SCMI_DTB_NODE_CHANGE}; then
                oe_runmake -C "${S}" dtbs
                dtb_name="$(ls ${ATF_BINARIES}/fdts/*.dtb)"
                nativepython3 ${STAGING_BINDIR_NATIVE}/scmi_dtb_node_change.py \
                    ${dtb_name} \
                    ${GPIO_OPT} \
                    ${PINCTRL_OPT} \
                    ${NVMEM_OPT}
            fi
        done
    done
}

do_install() {
    install -d ${D}/boot
    for type in ${BOOT_TYPE}; do
        for plat in ${PLATFORM}; do
            ATF_BINARIES="${B}/${type}/${plat}/${BUILD_TYPE}"
            if [ "${type}" = "sd" ]; then
                cp -v ${ATF_BINARIES}/bl2_w_dtb.bin ${D}/boot/bl2_w_dtb-${plat}.bin
                cp -v ${ATF_BINARIES}/bl2_w_dtb.s32 ${D}/boot/bl2_w_dtb-${plat}.s32
                cp -v ${ATF_BINARIES}/fip.bin ${D}/boot/fip-${plat}.bin
            else
                cp -v ${ATF_BINARIES}/bl2_w_dtb.bin ${D}/boot/bl2_w_dtb-${plat}_${type}.bin
                cp -v ${ATF_BINARIES}/bl2_w_dtb.s32 ${D}/boot/bl2_w_dtb-${plat}_${type}.s32
                cp -v ${ATF_BINARIES}/fip.bin ${D}/boot/fip-${plat}_${type}.bin
            fi
        done
    done
}

do_deploy() {
    install -d ${DEPLOY_DIR_IMAGE}

    for type in ${BOOT_TYPE}; do
        for plat in ${PLATFORM}; do
            ATF_BINARIES="${B}/$type/${plat}/${BUILD_TYPE}"

            if [ "${type}" = "sd" ]; then
                cp -v ${ATF_BINARIES}/bl2_w_dtb.s32 ${DEPLOY_DIR_IMAGE}/bl2_w_dtb-${plat}.s32
                cp -v ${ATF_BINARIES}/fip.bin ${DEPLOY_DIR_IMAGE}/fip-${plat}.bin
            else
                cp -v ${ATF_BINARIES}/bl2_w_dtb.s32 ${DEPLOY_DIR_IMAGE}/bl2_w_dtb-${plat}_${type}.s32
                cp -v ${ATF_BINARIES}/fip.bin ${DEPLOY_DIR_IMAGE}/fip-${plat}_${type}.bin
            fi
        done
    done
}

addtask deploy after do_install before do_build

do_compile[depends] = "virtual/bootloader:do_deploy"
do_compile[depends] += "${@bb.utils.contains('DISTRO_FEATURES', 'optee', 'optee-os:do_deploy', '', d)}"

FILES:${PN} += "/boot/*"

KERNEL_PN = "${@d.getVar('PREFERRED_PROVIDER_virtual/kernel')}"
python () {
    # Make "bitbake atf-s32g" depends linux kernel
    d.appendVar('DEPENDS', " " + d.getVar('KERNEL_PN'))
}
