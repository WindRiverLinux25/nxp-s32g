require recipes-bsp/u-boot/u-boot.inc
require u-boot-s32.inc

DESCRIPTION = "U-boot provided by NXP with focus on S32 chipsets"
PROVIDES += "u-boot"

INHIBIT_DEFAULT_DEPS = "1"
DEPENDS:append = " libgcc virtual/cross-cc python3 dtc-native bison-native"

inherit nxp-u-boot-localversion

do_compile:append:nxp-s32g() {
    if [ "${HSE_SEC_ENABLED}" = "1" ]; then
        for plat in ${UBOOT_CONFIG}; do
            cfgout="${B}/${plat}_defconfig/u-boot-s32.cfgout"
            if [ -e $cfgout ]; then
                if echo $plat | grep -q s32g2; then
                    sed -i 's|${HSE_FW_DEFAULT_NAME}|${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G2}/${HSE_FW_NAME_S32G2}|g' $cfgout
                else
                    sed -i 's|${HSE_FW_DEFAULT_NAME}|${HSE_LOCAL_FIRMWARE_DIR}/${HSE_FW_VERSION_S32G3}/${HSE_FW_NAME_S32G3}|g' $cfgout
                fi
            fi
        done
    fi
}

do_deploy:append() {
    unset i j
    for config in ${UBOOT_MACHINE}; do
        i=$(expr $i + 1);
        for type in ${UBOOT_CONFIG}; do
            j=$(expr $j + 1)
            if  [ $j -eq $i ]; then
                install -d ${DEPLOYDIR}/${type}
                install -m 0644 ${B}/${config}/${UBOOT_BINARY} ${DEPLOYDIR}/${type}/${UBOOT_BINARY}
                install -m 0644 ${B}/${config}/${UBOOT_CFGOUT} ${DEPLOYDIR}/${type}/${UBOOT_CFGOUT}

                qspi_param_bin=${B}/${config}/${QSPI_DEFAULT_PARAM_BIN_NAME}
                if [ -e ${qspi_param_bin} ]; then
                    install -m 0644 ${qspi_param_bin} ${DEPLOYDIR}/${type}/
                fi
            fi
        done
        unset j
    done
    unset i
}
