DESCRIPTION = "NXP LLCE FIRMWARES"
PROVIDES += "llce"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LIENCES.txt;md5=324d5bf9404f8bf2b01cfc66037447f6"

inherit deploy

do_configure[noexec] = "1"
do_compile[noexec] = "1"

SRC_URI = " \
	file://LIENCES.txt \
"

# Tell yocto not to bother stripping our binaries, especially the firmware
# since 'aarch64-fsl-linux-strip' fails with error code 1 when parsing the firmware
# ("Unable to recognise the format of the input file")
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"

S = "${WORKDIR}/sources"
UNPACKDIR = "${S}"

PACKAGE_ARCH = "${MACHINE_ARCH}"


do_install() {

	LLCE_FW_PLAT_FOLDERS="${LLCE_FW_S32G2} ${LLCE_FW_S32G3}"
	for folder in $LLCE_FW_PLAT_FOLDERS; do

		mkdir -p "${FW_INSTALL_DIR}/${folder}"
		if [ "${folder}" = "${LLCE_FW_S32G3}" ]; then
                        src_dir=${LLCE_LOCAL_FIRMWARE_DIR_S32G3}
                else
                        src_dir=${LLCE_LOCAL_FIRMWARE_DIR}
                fi

                dst_dir=${FW_INSTALL_DIR}/${folder}
                for bin in ${LLCE_FW_BIN_LIST}; do
                        install -D "${src_dir}/${bin}" "${dst_dir}/${bin}"
                done
	done
}

do_deploy() {

	LLCE_FW_PLAT_FOLDERS="${LLCE_FW_S32G2} ${LLCE_FW_S32G3}"
	for folder in $LLCE_FW_PLAT_FOLDERS; do

	        install -d ${DEPLOYDIR}/${folder}
                src_dir=${FW_INSTALL_DIR}/${folder}
                dst_dir=${DEPLOYDIR}/${folder}

                for bin in ${LLCE_FW_BIN_LIST}; do
                        install -m 0644 "${src_dir}/${bin}" "${dst_dir}/${bin}"
                done
	done
}


python () {
    postinst = """
    if [ -z "$D" ]; then
        if grep -q "s32g3" /sys/firmware/devicetree/base/compatible; then
            plat=s32g3
        else
            plat=s32g2
        fi
    else
        [ -z "${S32G_SOC_VARIANT}" ] && exit -1
        plat=${S32G_SOC_VARIANT}
    fi

    bins="dte.bin frpe.bin ppe_tx.bin ppe_rx.bin"
    for bin in $bins; do
        if [ -f "$D/lib/firmware/$bin" ]; then
                continue
        fi

        ln -rs $D/lib/firmware/llce_fw_$plat/$bin $D/lib/firmware/$bin
    done

    if [ -z "$D" ]; then
        mod_list="llce_can llce_core llce_mailbox"
        for mod in $mod_list; do
                if grep -wq "^$mod" /proc/modules ; then
                        rmmod $mod;
                fi
        done

        for mod in $mod_list; do
                modprobe $mod
        done
    fi
"""

    pn = d.getVar('PN');

    if bb.utils.contains('IMAGE_FEATURES', 'read-only-rootfs', True, False, d):
        if d.getVar('S32G_SOC_VARIANT') is None:
            bb.fatal("You have to set S32G_SOC_VARIANT for a read only rootfs")

        d.setVar('pkg_postinst:%s' % pn, postinst)
    else:
        d.setVar('pkg_postinst_ontarget:%s' % pn, postinst)
}

# do_package_qa throws error "QA Issue: Architecture did not match"
# when checking the firmware
do_package_qa[noexec] = "1"
do_package_qa_setscene[noexec] = "1"

FILES:${PN} += "/lib/firmware/*"

COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
