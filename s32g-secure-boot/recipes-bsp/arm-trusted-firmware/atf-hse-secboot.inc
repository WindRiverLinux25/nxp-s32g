# Copyright 2023,2024 NXP

DEPENDS:append = " atf-s32g-tools-native"

SECBOOT_PUBKEYS_DIR = "${S}/${HSE_SEC_KEYS}"
SECBOOT_DEVICE = "${@bb.utils.contains('UBOOT_MACHINE', 'qspi', '/dev/mtd0', '/dev/mmcblk0', d)}"
OP_TEE = "${@bb.utils.contains('DISTRO_FEATURES', 'optee', '1', '0', d)}"
M7_SEC = "${@bb.utils.contains_any('MACHINE_FEATURES', 'm7_boot secure_boot_parallel', '1', '0', d)}"
M7_BIN_FILE ?= "${@ "m7.bin" if "${M7_SEC}" == '1' else ''}"
M7_SIGN_FILE ?= "${@ "m7-sign.bin" if "${M7_SEC}" == '1' else ''}"

BL2_NAME = "bl2_w_dtb"
BL2_SIGN_NAME = "bl2-signature"

RSA_PUB_KEYS ?= "\
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL2} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL31} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL33} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_DDRFW} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL2_DER} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL31_DER} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL33_DER} \
                ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_DDRFW_DER} \
               "

SYSROOT_DIRS:append  = " \
                           ${SECBOOT_KEYS_INSTALL_PATH} \
                           ${SECBOOT_BINS_INSTALL_PATH} \
                       "

do_generate_bl_public_keys () {

    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL2} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL2}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL2} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL2_DER}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL31} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL31}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL31} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL31_DER}
    if [ ${OP_TEE} -eq 1 ]; then
        openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL32} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL32}
        openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL32} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL32_DER}
    fi
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL33} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL33}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_BL33} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL33_DER}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_DDRFW} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_DDRFW}
    openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_DDRFW} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_DDRFW_DER}
    if [ ${M7_SEC} -eq 1 ]; then
        openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_M7} -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_M7}
        openssl rsa -in ${SECBOOT_SIGN_KEYDIR}/${RSA_PRIV_M7} -outform DER -pubout -out ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_M7_DER}
    fi
}

do_generate_bl_public_keys[depends] += "openssl-native:do_populate_sysroot"
addtask do_generate_bl_public_keys after do_compile before do_install

do_compile:prepend () {
	mkdir -p ${SECBOOT_PUBKEYS_DIR}
}


do_deploy:append() {

        for type in ${BOOT_TYPE}; do
            for plat in ${PLATFORM}; do
	        ATF_BINARIES="${B}/${type}/${plat}/${BUILD_TYPE}"
                if [ "${type}" = "sd" ]; then
                    cp -vf ${ATF_BINARIES}/${BL2_NAME}.bin ${DEPLOY_DIR_IMAGE}/${BL2_NAME}-${plat}.bin
                    cp -vf ${ATF_BINARIES}/${BL2_SIGN_NAME}.bin ${DEPLOY_DIR_IMAGE}/${BL2_NAME}-${plat}.bin.sign
                else
                    cp -vf ${ATF_BINARIES}/${BL2_NAME}.bin ${DEPLOY_DIR_IMAGE}/${BL2_NAME}-${plat}_${type}.bin
                    cp -vf ${ATF_BINARIES}/${BL2_SIGN_NAME}.bin ${DEPLOY_DIR_IMAGE}/${BL2_NAME}-${plat}_${type}.bin.sign
                fi
            done
        done

	cp -vf ${RSA_PUB_KEYS} ${DEPLOY_DIR_IMAGE}

	if [ ${OP_TEE} -eq 1 ]; then
		cp -vf ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL32} ${DEPLOY_DIR_IMAGE}
	fi
	if [ ${M7_SEC} -eq 1 ]; then
		cp -vf ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_M7} ${DEPLOY_DIR_IMAGE}
	fi
}

do_install:append() {
	install -d ${D}${SECBOOT_KEYS_INSTALL_PATH}
	install -d ${D}${SECBOOT_BINS_INSTALL_PATH}

        for type in ${BOOT_TYPE}; do
            for plat in ${PLATFORM}; do
                ATF_BINARIES="${B}/${type}/${plat}/${BUILD_TYPE}"
                if [ "${type}" = "sd" ]; then
                    cp -vf ${ATF_BINARIES}/${BL2_NAME}.bin ${D}${SECBOOT_BINS_INSTALL_PATH}/${BL2_NAME}-${plat}.bin
                    cp -vf ${ATF_BINARIES}/${BL2_SIGN_NAME}.bin ${D}${SECBOOT_BINS_INSTALL_PATH}/${BL2_NAME}-${plat}.bin.sign
                else
                    cp -vf ${ATF_BINARIES}/${BL2_NAME}.bin ${D}${SECBOOT_BINS_INSTALL_PATH}/${BL2_NAME}-${plat}_${type}.bin
                    cp -vf ${ATF_BINARIES}/${BL2_SIGN_NAME}.bin ${D}${SECBOOT_BINS_INSTALL_PATH}/${BL2_NAME}-${plat}_${type}.bin.sign
                fi
            done
        done

	install -m 0666 ${RSA_PUB_KEYS} ${D}${SECBOOT_KEYS_INSTALL_PATH}
	if [ ${OP_TEE} -eq 1 ]; then
		install -m 0666 ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL32} ${D}${SECBOOT_KEYS_INSTALL_PATH}
		install -m 0666 ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_BL32_DER} ${D}${SECBOOT_KEYS_INSTALL_PATH}
	fi
	if [ ${M7_SEC} -eq 1 ]; then
		install -m 0666 ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_M7} ${D}${SECBOOT_KEYS_INSTALL_PATH}
		install -m 0666 ${SECBOOT_PUBKEYS_DIR}/${RSA_PUB_M7_DER} ${D}${SECBOOT_KEYS_INSTALL_PATH}
        fi
}

FILES:${PN} += "${SECBOOT_BINS_INSTALL_PATH}${BL2_NAME}-*"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL2}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL31}"
FILES:${PN} += "${@ "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL32}" if "${OP_TEE}" == '1' else ''}"
FILES:${PN} += "${@ "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_M7}" if "${M7_SEC}" == '1' else ''}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL33}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_DDRFW}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL2_DER}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL31_DER}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL33_DER}"
FILES:${PN} += "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_DDRFW_DER}"
FILES:${PN} += "${@ "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_BL32_DER}" if "${OP_TEE}" == '1' else ''}"
FILES:${PN} += "${@ "${SECBOOT_KEYS_INSTALL_PATH}${RSA_PUB_M7_DER}" if "${M7_SEC}" == '1' else ''}"
