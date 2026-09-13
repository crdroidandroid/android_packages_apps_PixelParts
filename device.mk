#
# Copyright (C) 2023-2024 The Evolution X Project
#               2024 crDroid Android Project
#
# SPDX-License-Identifier: Apache-2.0
#

# PixelParts app
PRODUCT_PACKAGES += \
    PixelParts

# PixelParts init rc
PRODUCT_PACKAGES += \
    init.pixelparts.rc \
    init.pixelparts_mbn.rc

# PixelParts privileged permission allowlist. Required even though the app is
# platform signed; without it the first boot fails under
# ro.control_privapp_permissions=enforce.
PRODUCT_COPY_FILES += \
    packages/apps/PixelParts/permissions/privapp-permissions-pixelparts.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-permissions-pixelparts.xml

# PixelParts sepolicy. The app is system_ext, so its domain and its
# seapp_contexts entry have to be labeled by platform policy - vendor policy
# labeling a platform app is a Treble labeling violation. What stays on the
# vendor side is the one sysfs node the app writes, which only the device
# knows the path of.
SYSTEM_EXT_PUBLIC_SEPOLICY_DIRS += packages/apps/PixelParts/sepolicy/public
SYSTEM_EXT_PRIVATE_SEPOLICY_DIRS += packages/apps/PixelParts/sepolicy/private
BOARD_SEPOLICY_DIRS += packages/apps/PixelParts/sepolicy/vendor

# Extra modem carrier configurations. Stock's entries come through untouched.
# See mbn/README.md.
include packages/apps/PixelParts/mbn/mbn.mk
