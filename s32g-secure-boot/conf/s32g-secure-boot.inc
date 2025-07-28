#
# Copyright (C) 2020 Wind River Systems, Inc.
#

ATF_SIGN_ENABLE = "1"
UBOOT_SIGN_KEYDIR = "${LAYER_PATH_s32g-secure-layer}/s32g-sample-key/fitImage-rsa2048-keys"
UBOOT_MKIMAGE_DTCOPTS = "-I dts -O dtb -p 2000"
# Key used to sign configuration part in fit image
UBOOT_SIGN_KEYNAME = "boot_key"
# Key used to sign kernel and dtb parts in fit image
UBOOT_SIGN_IMG_KEYNAME = "boot_key_individual"
FIT_SIGN_INDIVIDUAL  = "1"

UBOOT_ENTRYPOINT = "0xa4000000"
UBOOT_LOADADDRESS = "0xa4000000"
UBOOT_DTB_LOADADDRESS = "0xb4000000"

UBOOT_SUFFIX = "img"

# NXPS32G secure boot feature supports rsa2048.
# Set KERNEL_RAS_TYPE as the expected value. The default one is rsa2048.
KERNEL_RAS_TYPE = "rsa2048"

# NXPS32G secure boot feature supports sha1 and sha256 digest.
# Set KERNEL_CSUM_TYPE as the expected value. The default one is sha1.
KERNEL_CSUM_TYPE = "sha1"

ATF_DTB:nxp-s32g = "s32g399a-rdb3.dtb"
ATF_DTB:aptiv_cvc_sousa = "aptiv-cvc-sousa.dtb"
ATF_DTB:aptiv-cvc-fl = "aptiv_cvc_fl.dtb"

SECBOOT_SIGN_KEYDIR ?= "${LAYER_PATH_s32g-secure-layer}/s32g-sample-key/secboot-rsa2048-keys"
