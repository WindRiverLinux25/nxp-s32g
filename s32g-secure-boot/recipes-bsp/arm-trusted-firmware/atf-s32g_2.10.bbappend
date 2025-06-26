FILES:${PN} = "/boot ${datadir}"

include atf-hse-secboot.inc

inherit uboot-config

# There are 256 bytes space following IVT, it is able to be used save BSP specific flags
# Boot Types, offset is 0x1100 from the beginning of bootloader image
# The default value 0 represents for Non-secboot, it doesn't need to set it explicitly.
boot_type_off_sd = "4352"
boot_type_off_qspi = "256"
non_secboot = "00000000"
a53_secboot = "00000001"
m7_secboot = "00000002"
nxp_parallel_secboot = "00000003"

str2bin () {
	# write binary as little endian
	print_cmd=`which printf`
	$print_cmd $(echo $1 | sed -E -e 's/(..)(..)(..)(..)/\4\3\2\1/' -e 's/../\\x&/g')
}

do_compile:append() {
    [ "${ATF_SIGN_ENABLE}" = "1" ] || return

    unset LDFLAGS
    unset CFLAGS
    unset CPPFLAGS

    for type in ${BOOT_TYPE}; do
        unset i j
        for plat in ${PLATFORM}; do
            build_base="${B}/$type/"
            ATF_BINARIES="${B}/${type}/${plat}/${BUILD_TYPE}"
            cp "${STAGING_DIR_HOST}/sysroot-only/fitImage" "${ATF_BINARIES}/fitImage-linux"
            bl33_dir="${DEPLOY_DIR_IMAGE}/${plat}_${type}"
            fip_location="FIP_LOCATION=$type"
            for tmp in ${DTB_FILES}; do
                name=`echo $tmp | sed 's/-//' | cut -d . -f1`
                if [ "$name" = "$plat" ]; then
                   dtb=$tmp
                   break
                fi
            done
            if [ "$type" = "sd" ]; then
                bl33_dir="${DEPLOY_DIR_IMAGE}/${plat}"
                fip_location="FIP_LOCATION=mmc"
            fi
            bl33_bin="${bl33_dir}/${UBOOT_BINARY}"
            uboot_cfg="${bl33_dir}/${UBOOT_CFGOUT}"

            hse_fw_dir="NXP_HSE_FWDIR=${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G3}"
            if echo $plat | grep -q s32g2; then
                hse_fw_dir="NXP_HSE_FWDIR=${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G2}"
            fi

            if ${@bb.utils.contains('DISTRO_FEATURES', 'optee', 'true', 'false', d)}; then
                optee_plat="$(echo $plat | cut -c1-5)"
                optee_arg="BL32=${DEPLOY_DIR_IMAGE}/optee/$optee_plat/tee-header_v2.bin \
			BL32_EXTRA1=${DEPLOY_DIR_IMAGE}/optee/$optee_plat/tee-pager_v2.bin \
			SPD=opteed"
            fi

            bl_keys=" BL2_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL2} \
                      BL31_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL31} \
                      BL32_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL32} \
                      BL33_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL33} \
                      DDRFW_KEY=${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_DDRFW} \
                     "

            i=$(expr $i + 1);
            for dtb in ${ATF_DTB}; do
                j=$(expr $j + 1)
                if  [ $j -eq $i ]; then
                    # Re-sign the kernel in order to add the keys to our dtb
                    ${UBOOT_MKIMAGE_SIGN} \
                       ${@'-D "${UBOOT_MKIMAGE_DTCOPTS}"' if len('${UBOOT_MKIMAGE_DTCOPTS}') else ''} \
                       -F -k "${UBOOT_SIGN_KEYDIR}" \
                       -K "${B}/${type}/${plat}/${BUILD_TYPE}/fdts/${dtb}" \
                       -r ${ATF_BINARIES}/fitImage-linux
                    oe_runmake -C ${S} DTB_FILE_NAME=${dtb} BUILD_BASE=$build_base PLAT=${plat} BL33=$bl33_bin BL33DIR=$bl33_dir MKIMAGE_CFG=$uboot_cfg MKIMAGE=mkimage $optee_arg $hse_fw_dir $fip_location $bl_keys DDR_FW_BIN_PATH=${DDR_FW_PATH} all
                fi
            done
            unset j
        done
        unset i
    done
}

do_install:prepend() {
    [ "${ATF_SIGN_ENABLE}" = "1" ] || return

    for type in ${BOOT_TYPE}; do
        for plat in ${PLATFORM}; do
            ATF_BINARIES="${B}/$type/${plat}/${BUILD_TYPE}"

            # Set the boot type
            secboot_type=${a53_secboot}
            if ${@bb.utils.contains('MACHINE_FEATURES', 'm7_boot', 'true', 'false', d)}; then
                secboot_type=${m7_secboot}
                if ${@bb.utils.contains('MACHINE_FEATURES', 'secure_boot_parallel', 'true', 'false', d)}; then
                    secboot_type=${nxp_parallel_secboot}
                fi
            fi

            if [ "${type}" = "sd" ]; then
                boot_type_off=${boot_type_off_sd}
            else
                boot_type_off=${boot_type_off_qspi}
            fi

            str2bin ${secboot_type} | dd of="${ATF_BINARIES}/bl2_w_dtb.s32" count=4 seek=${boot_type_off} \
                                  conv=notrunc,fsync status=none iflag=skip_bytes,count_bytes oflag=seek_bytes
        done
    done
}

python() {
    dir=d.getVar('SECBOOT_SIGN_KEYDIR')
    if dir == "" or not os.path.exists(dir):
        bb.fatal("Please set a valid private key path variable(SECBOOT_SIGN_KEYDIR) for secure boot firstly, and then build again!")

    key=d.getVar('RSA_PRIV_BL2')
    key_path = oe.path.join(dir, key)
    if not os.path.exists(key_path):
        bb.fatal("Please set valid private key name variable(RSA_PRIV_BL2 at least) for secure boot firstly, and then build again!")
}

