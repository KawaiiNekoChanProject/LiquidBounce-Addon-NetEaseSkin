package net.kawaii_neko_chan.netease_skin.modules

import com.mojang.blaze3d.platform.NativeImage
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.render.readNativeImage
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.core.ClientAsset
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.player.PlayerSkin
import net.nekocurit.x19.WPLauncherAPI
import net.nekocurit.x19.WPLauncherAccountAPI
import net.nekocurit.x19.api.getSelfDetail
import net.nekocurit.x19.data.cookie.WPLauncherCookieRaw
import net.nekocurit.x19.data.skin.X19DefaultSkins
import net.nekocurit.x19.data.skin.X19Skin
import net.nekocurit.x19.extensions.getSkinFromUUID
import net.nekocurit.x19.extensions.login
import java.util.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ModuleNetEaseSkin : ClientModule("NetEaseSkin", ModuleCategories.MISC) {

    private val cookie by text("Cookie", "")
    val self by boolean("Self", false)

    @JvmField
    val defaultSkin = DefaultPlayerSkin.getDefaultSkin()

    var session: WPLauncherAccountAPI? = null
    val skins = hashMapOf<ULong, NativeImage>()
    val playerSkins = hashMapOf<UUID, PlayerSkin>()
    val queue = Channel<UUID>(Channel.UNLIMITED)

    override fun onEnabled() {
        ioScope.launch {
            runCatching {
                runCatching { session?.getSelfDetail() }.onFailure { session = null }
                if (session == null) session = WPLauncherAPI.newInstance().login(WPLauncherCookieRaw(cookie))
            }
                .onSuccess {
                    val session = session ?: return@onSuccess

                    // 心跳线程
                    launch {
                        while (enabled) {
                            delay(10.minutes)
                            session.refresh()
                            logger.info("刷新启动器账号凭据成功")
                        }
                    }
                    // 获取皮肤线程
                    launch {
                        while (enabled) {
                            val next = queue.receive()
                            logger.info("开始获取 玩家[UUID=$next] 的 皮肤数据")
                            runCatching {
                                val entity = session.getSkinFromUUID(next.toString()).java

                                // 玩家是默认皮肤就跳过获取
                                if (entity.id != X19DefaultSkins.STEVE) {
                                    val image = skins.getOrPut(entity.id) {
                                        session.client.get(entity.url)
                                            .bodyAsChannel()
                                            .toInputStream()
                                            .readNativeImage()
                                    }

                                    val id = LiquidBounce.identifier("netease-skin-$next")
                                    withContext(Dispatchers.Main) { image.registerTexture(id) }

                                    playerSkins[next] = PlayerSkin(ClientAsset.DownloadedTexture(LiquidBounce.identifier("netease-skin-$next"), entity.url), null, null, when (entity.mode) {
                                        X19Skin.SkinMode.DEFAULT -> PlayerModelType.WIDE
                                        X19Skin.SkinMode.SLIM -> PlayerModelType.SLIM
                                    }, false)
                                }

                                entity
                            }
                                .onFailure { e ->
                                    logger.warn("未能成功获取 玩家[UUID=$next] 的 皮肤数据, 将在稍后重试", e)
                                    launch {
                                        delay(15.seconds)
                                        playerSkins.remove(next)
                                    }
                                }
                                .onSuccess { entity ->
                                    logger.info("成功获取 玩家[UUID=$next] 的 皮肤数据[id=${entity.id}, url=${entity.url}, mode=${entity.mode}]")
                                }

                        }
                    }
                }
                .onFailure { e ->
                    logger.error("登陆启动器账号失败", e)
                    chat(regular(message("cookieInvalid", e.message ?: "未知错误")))
                    enabled = false
                }
        }
    }

    @JvmStatic
    fun getPlayerSkin(player: Player) = playerSkins.getOrPut(player.gameProfile.id) {
        queue.trySend(player.gameProfile.id)
        defaultSkin
    }

}
