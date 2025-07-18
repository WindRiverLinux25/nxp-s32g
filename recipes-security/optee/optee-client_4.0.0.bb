require recipes-security/optee/optee-client.inc
require optee-nxp.inc

SRCREV = "acb0885c117e73cb6c5c9b1dd9054cb3f93507ee"

inherit pkgconfig
DEPENDS += "util-linux"
EXTRA_OEMAKE += "PKG_CONFIG=pkg-config"

FILESEXTRAPATHS:prepend := "${LAYER_PATH_meta-arm}/recipes-security/optee/${PN}:${THISDIR}/${PN}:"
SRC_URI:append:nxp-s32g = " \
    file://tee-supplicant.service \
"

EXTRA_OECMAKE:nxp-s32g = " \
    -DBUILD_SHARED_LIBS=ON \
    -DCFG_TEE_FS_PARENT_PATH='${localstatedir}/lib/tee' \
"

SYSTEMD_SERVICE:${PN}:nxp-s32g = "tee-supplicant.service"

do_install:append:nxp-s32g() {
    install -D -p -m0644 ${UNPACKDIR}/tee-supplicant.service ${D}${systemd_system_unitdir}/tee-supplicant.service
    install -D -p -m0755 ${UNPACKDIR}/tee-supplicant.sh ${D}${sysconfdir}/init.d/tee-supplicant

    sed -i -e s:@sysconfdir@:${sysconfdir}:g \
           -e s:@sbindir@:${sbindir}:g \
              ${D}${systemd_system_unitdir}/tee-supplicant.service \
              ${D}${sysconfdir}/init.d/tee-supplicant
}
