DESCRIPTION = "NXP HSE PKCS#11 Module"
PROVIDES += "pkcs11-hse"

LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = " \
    file://README.md;md5=baa031ec250d18077a8900486c104a74\
"

DEPENDS = "openssl libp11"
RDEPENDS:${PN} = "opensc pcsc-lite ccid"

URL ?= "git://github.com/nxp-auto-linux/pkcs11-hse.git;protocol=https"
BRANCH ?= "release/bsp44.0"
SRC_URI = "${URL};branch=${BRANCH}"

SRCREV = "0542cc5a248cc2d35af339c38270e2059a927b31"
SRC_URI[sha256sum] = "b529fcbbb8f4347310d433162b81291da5955f9916d5c6ad5f4dc316ef6aef14"

SRC_URI += " \
    file://0001-pkcs11-hse-Makefile-using-internal-compile-variables.patch \
    file://0001-hse-pkcs-secboot-add-code-to-support-m7-secure-boot.patch \
    file://0001-examples-hse-secboot-update-usage-messages.patch \
"

PATCHTOOL = "git"
PACKAGE_ARCH = "${MACHINE_ARCH}"

S = "${WORKDIR}/git"

# Disable -O2 optimization, since it seems to be exposing an alignment issue
SELECTED_OPTIMIZATION:remove = "-O2"

EXTRA_OEMAKE += " \
	CROSS_COMPILE=${TARGET_PREFIX} \
"

CFLAGS += "${HOST_CC_ARCH}${TOOLCHAIN_OPTIONS}"


PKCS_DEMO_BINS = "pkcs-keyop hse-encrypt hse-sysimg pkcs-key-provision hse-secboot \
                  pkcs-cipher pkcs-msg-digest pkcs-sig"

python () {
    # Skip the recipe if HSE_LOCAL_FIRMWARE_DIR is NULL:
    firmware_dir = d.getVar('HSE_LOCAL_FIRMWARE_DIR')
    if not firmware_dir:
        raise bb.parse.SkipRecipe("Skip the recipe since HSE_LOCAL_FIRMWARE_DIR is not set")

    # The post installation script for pkcs11-hse-examples
    postinst = """
    if [ -z "$D" ]; then
        if grep -q "s32g3" /sys/firmware/devicetree/base/compatible ; then
            plat="s32g3"
        else
            plat="s32g2"
        fi
    else
        [ -z "${S32G_SOC_VARIANT}" ] && exit -1
        plat=${S32G_SOC_VARIANT}
    fi

    for bin in ${PKCS_DEMO_BINS}; do
        if [ -f "$D/usr/bin/$bin" ]; then
            continue
        fi

        cp $D/usr/bin/$plat/$bin $D/usr/bin/$bin
    done

    # remove the unneeded directories
    rm -rf $D/usr/bin/s32g2
    rm -rf $D/usr/bin/s32g3
"""

    pn = d.getVar('PN');

    if bb.utils.contains('IMAGE_FEATURES', 'read-only-rootfs', True, False, d):
        if d.getVar('S32G_SOC_VARIANT') is None:
            bb.fatal("You have to set S32G_SOC_VARIANT for a read only rootfs")

        d.setVar('pkg_postinst:%s-examples' % pn, postinst)
    else:
        d.setVar('pkg_postinst_ontarget:%s-examples' % pn, postinst)
}

do_compile() {


    plats="s32g2 s32g3"
    for plat in $plats; do

        mkdir -p ${S}/hse-fw/${plat}
        if [ "$plat" = "s32g2" ]; then
            fw_version="${HSE_FW_VERSION_S32G2}"
        else
            fw_version="${HSE_FW_VERSION_S32G3}"
        fi

        cp -r ${HSE_LOCAL_FIRMWARE_DIR}/${fw_version}/interface ${S}/hse-fw/${plat}/
        # compile share libraries(libhse and libpkcs) firstly, they are all same either S32G2 or S32G3
        oe_runmake HSE_FWDIR=${S}/hse-fw/${plat}  CFLAGS="${CFLAGS} -shared -fPIC -Wall -fno-builtin"

        # clean the example binaries because they are needed to be compiled with different options
        oe_runmake -C examples clean
        # compile demo apps which may not be same between S32G2 and S32G3
        oe_runmake -C examples HSE_FWDIR=${S}/hse-fw/${plat} PKCS11HSE_DIR=${S} LIBS="-L${STAGING_LIBDIR}/" INCLUDE="-I${STAGING_INCDIR}" LDFLAGS="${LDFLAGS} -lcrypto -lp11"

        #copy result files to related dir
        mkdir -p ${S}/examples/${plat}

        for bin in ${PKCS_DEMO_BINS}; do
            cp ${S}/examples/${bin}/${bin} ${S}/examples/${plat}
        done

    done
}

do_install() {

    install -d ${D}${libdir}
    install -m 0755 ${S}/libpkcs-hse.so.1.0 ${D}${libdir}/libpkcs-hse.so.1.0
    install -m 0755 ${S}/libhse.so.2.1 ${D}${libdir}/libhse.so.2.1
    ln -s libhse.so.2.1 ${D}${libdir}/libhse.so.2

    install -d ${D}${includedir}
    install -m 0644 ${S}/libhse/*.h ${D}${includedir}
    install -m 0644 ${S}/libpkcs/*.h ${D}${includedir}

    plats="s32g2 s32g3"
    for plat in $plats; do

        install -d ${D}${bindir}/${plat}/
        for bin in ${PKCS_DEMO_BINS}; do
            install -m 0755 ${S}/examples/${plat}/${bin} ${D}${bindir}/${plat}/
        done

    done
}

PACKAGES =+ "${PN}-examples "
FILES:${PN}-examples += "${bindir}"
COMPATIBLE_MACHINE = "^$"
COMPATIBLE_MACHINE:nxp-s32g = "nxp-s32g"
