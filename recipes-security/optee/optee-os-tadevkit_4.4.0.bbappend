require optee-os-nxp.inc

do_compile:nxp-s32g() {
    for plat in ${OPTEE_PLAT_FLAVORS}; do
        if [ "${HSE_SEC_ENABLED}" = "1" ]; then
            if [ "$plat" = "s32g2" ]; then
                fw_version="${HSE_FW_VERSION_S32G2}"
            else
                fw_version="${HSE_FW_VERSION_S32G3}"
            fi
            oe_runmake -C ${S} PLATFORM_FLAVOR=$plat CFG_NXP_HSE=y \
                       CFG_NXP_HSE_FWDIR=${HSE_LOCAL_FIRMWARE_DIR}/${fw_version} \
                       O=${B}/$plat all
        else
            oe_runmake -C ${S} PLATFORM_FLAVOR=$plat O=${B}/$plat all
        fi
    done
}

do_install:nxp-s32g() {
    #install TA devkit
    install -d ${D}${includedir}/optee/export-user_ta/
    for f in ${B}/${DEFAULT_TADEVKIT_VARIANT}/export-ta_${OPTEE_ARCH}/* ; do
        cp -aR $f ${D}${includedir}/optee/export-user_ta/
    done
}
