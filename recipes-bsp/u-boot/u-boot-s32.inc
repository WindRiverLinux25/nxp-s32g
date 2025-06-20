require recipes-bsp/u-boot/u-boot-common.inc

PE = ""

LICENSE = "GPL-2.0-only & BSD-3-Clause & BSD-2-Clause & LGPL-2.0-only & LGPL-2.1-only"
LIC_FILES_CHKSUM = " \
    file://Licenses/gpl-2.0.txt;md5=b234ee4d69f5fce4486a80fdaf4a4263 \
    file://Licenses/bsd-2-clause.txt;md5=6a31f076f5773aabd8ff86191ad6fdd5 \
    file://Licenses/bsd-3-clause.txt;md5=4a1190eac56a9db675d58ebe86eaf50c \
    file://Licenses/lgpl-2.0.txt;md5=4cf66a4984120007c9881cc871cf49db \
    file://Licenses/lgpl-2.1.txt;md5=4fbd65380cdd255951079008b364516c \
"

HSE_SUPPORT_CFG = " \
    file://hse_support.cfg \
"

SRC_URI = "git://github.com/nxp-auto-linux/u-boot.git;protocol=https;branch=release/bsp44.0-2022.04"
SRC_URI += " \
    file://0001-Revert-arch-s32cc-ERR051257-shouldn-t-be-applied-for.patch \
    file://0001-Revert-s32cc-mmc-Enable-MMC_IO_VOLTAGE.patch \
    file://0001-tools-s32ccimage-add-reserved-member-for-struct-prog.patch \
    file://0001-u-boot-s32-Makefile-add-scripts_basic-dependency-to-.patch \
    file://0001-configs-Enable-commands-for-ostree.patch \
    file://0001-nxp-s32g-include-config_distro_bootcmd.h-Check-go-be.patch \
    file://0001-s32g-remove-SAF1508-phy-driver-and-use-common-ulpi-i.patch \
    ${@bb.utils.contains('HSE_SEC_ENABLED', '1', '${HSE_SUPPORT_CFG}', '', d)} \
    file://0001-s32ccimage.c-Use-PATH_MAX-for-path-length.patch \
    file://0001-hse-enable-eFUSE-power-on-s32g399-RDB3-platform.patch \
    file://0001-phy-s32cc-serdes-configure-REF_USE_PAD-bit-during-re.patch \
    file://0001-s32-env-support-for-booting-up-fitImage.patch \
    file://0001-s32-env-Add-support-to-load-the-initrd-in-mmcboot.patch \
    ${@bb.utils.contains('DISTRO_FEATURES', 'optee', 'file://optee_config.cfg', '', d)} \
"

SRCREV = "9a003232781ee9999d7e41a1cd62024400794560"

PATCHTOOL = "git"
SCMVERSION = "y"
LOCALVERSION = ""
PACKAGE_ARCH = "${MACHINE_ARCH}"
UBOOT_INITIAL_ENV = ""

USRC ?= ""
S = '${@oe.utils.conditional("USRC", "", "${WORKDIR}/git", "${USRC}", d)}'

COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"

DEFAULT_PREFERRENCE = "-1"

QSPI_DEFAULT_PARAM_BIN_NAME ?= "qspi_params_MX25UW51245G.bin"
