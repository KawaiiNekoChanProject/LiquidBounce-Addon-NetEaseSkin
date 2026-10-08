package net.kawaii_neko_chan.netease_skin

import net.kawaii_neko_chan.netease_skin.modules.ModuleNetEaseSkin
import net.ccbluex.liquidbounce.features.addon.LiquidBounceAddon

class NetEaseSkinAddon : LiquidBounceAddon() {

    override fun onInitialize() {
        registerModules(ModuleNetEaseSkin)
    }

    /**
     * Settings have been restored from disk by this point.
     */
    override fun onStarted() {
        logger.info("Example module is ${if (ModuleNetEaseSkin.enabled) "enabled" else "disabled"}")
    }

    override fun onStopping() {
        logger.info("Example add-on shutting down")
    }

}
