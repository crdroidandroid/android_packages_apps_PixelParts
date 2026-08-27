/*
 * Copyright (C) 2023-2024 The Evolution X Project
 *               2024 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.evolution.pixelparts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

import org.evolution.pixelparts.autohbm.AutoHbmActivity;
import org.evolution.pixelparts.autohbm.AutoHbmController;
import org.evolution.pixelparts.autohbm.AutoHbmTileService;
import org.evolution.pixelparts.ims.ImsActivity;
import org.evolution.pixelparts.ims.ImsController;
import org.evolution.pixelparts.utils.ComponentUtils;

public class Startup extends BroadcastReceiver {

    /** Read by init, which binds the matching index over the stock one. */
    private static final String PROP_MBN_PROFILE = "persist.vendor.pixelparts.mbn";
    private static final String MBN_STOCK = "stock";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            reselectModemConfiguration(context);
            return;
        }

        // Auto hbm
        ComponentUtils.toggleComponent(
                context,
                AutoHbmActivity.class,
                AutoHbmController.isSupported()
        );

        ComponentUtils.toggleComponent(
                context,
                AutoHbmTileService.class,
                AutoHbmController.isSupported()
        );

        // IMS
        ComponentUtils.toggleComponent(
                context,
                ImsActivity.class,
                ImsController.isSupported(context)
        );
    }

    /**
     * init binds the chosen carrier configuration index a moment after the
     * modem has already started, so on a cold boot the modem can still be
     * running on the stock index. Restarting it makes it select again against
     * what is actually mounted.
     *
     * <p>Only when a non-stock profile is chosen, so a device that never
     * touches this setting never restarts its modem.
     */
    private void reselectModemConfiguration(Context context) {
        final String profile = SystemProperties.get(PROP_MBN_PROFILE, MBN_STOCK);
        if (MBN_STOCK.equals(profile) || !ImsController.isSupported(context)) {
            return;
        }
        ImsController.getInstance(context).restartModem();
    }
}
