#
# Copyright (C) 2020 Wind River Systems, Inc.
#

FILESEXTRAPATHS:prepend:nxp-s32g := "${THISDIR}/files:"
SRC_URI:append:nxp-s32g = " ${@bb.utils.contains('DISTRO_FEATURES', 'dm-verity', 'file://0001-crypto-hse-Drop-the-sha256-support.patch', '', d)}"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
TARGET_SUPPORTED_KTYPES:nxp-s32g = "standard preempt-rt"

DEPENDS:append = "${@ ' u-boot-tools-scmi-native' if d.getVar('SCMI_DTB_NODE_CHANGE') == 'true' else ''}"

PINCTRL_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_PINCTRL', '1', '--pinctrl', '--no-pinctrl', d)}"
GPIO_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_GPIO', '1', '--gpio', '--no-gpio', d)}"
NVMEM_OPT = "${@oe.utils.conditional('SCMI_USE_SCMI_NVMEM', '1', '--nvmem', '--no-nvmem', d)}"

PLAT_HEADERS_TAGS = " s32gen1 s32cc s32g"

# Fix the dtc compile issue if SRM enabled
do_compile:prepend:nxp-s32g() {

    if ${SCMI_DTB_NODE_CHANGE}; then

        # DTB might have been changed from an earlier build, see SCMI_DTB_NODE_CHANGE changes.
        # Clean-up these changes to guarantee an updated DTB.
        dtb_dir="${B}/arch/arm64/boot/dts"
        dtb_names="${KERNEL_DEVICETREE}"
        echo "dtb_names are: $dtb_names"

        for dtb_name in $dtb_names;
        do
            if [ -f ${dtb_dir}/${dtb_name} ]; then
                rm ${dtb_dir}/${dtb_name}
            fi
        done
    fi
}

# Switch from SIUL2 to SCMI drivers if needed
do_compile:append:nxp-s32g() {
    for dtb_name in $dtb_names; do
        if ${SCMI_DTB_NODE_CHANGE}; then
            nativepython3 ${STAGING_BINDIR_NATIVE}/scmi_dtb_node_change.py \
                ${dtb_dir}/${dtb_name} \
                ${GPIO_OPT} \
                ${PINCTRL_OPT} \
                ${NVMEM_OPT}
        fi
    done
}

SYSROOT_DIRS:append = " /sysroot-only"
do_install:append:nxp-s32g() {
    if ${SCMI_DTB_NODE_CHANGE}; then
        install -d ${D}/sysroot-only/plat-hdrs
        for tag in ${PLAT_HEADERS_TAGS}; do
            for proto in clock reset perf nvmem; do
                hdr=$(find ${S} -name "*${tag}-*scmi*${proto}.h")
                if [ -z "${hdr}" ]; then
                    continue
                fi

                install -m 0644 "${hdr}" ${D}/sysroot-only/plat-hdrs
            done

            # Export other platform headers
            for name in can-ts; do
                hdr=$(find ${S} -name "*${tag}-*${name}*.h")
                if [ -z "${hdr}" ]; then
                    continue
                fi

                install -m 0644 "${hdr}" ${D}/sysroot-only/plat-hdrs
            done
        done
    fi
}
