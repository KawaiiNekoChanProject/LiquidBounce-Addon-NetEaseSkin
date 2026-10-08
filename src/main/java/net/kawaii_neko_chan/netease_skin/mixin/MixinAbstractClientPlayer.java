package net.kawaii_neko_chan.netease_skin.mixin;

import com.mojang.authlib.GameProfile;
import net.kawaii_neko_chan.netease_skin.modules.ModuleNetEaseSkin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends Player {

    public MixinAbstractClientPlayer(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
    private void injectCustomSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        if (!ModuleNetEaseSkin.INSTANCE.getEnabled()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        if (this.getUUID().equals(client.player.getUUID()) && !ModuleNetEaseSkin.INSTANCE.getSelf()) return;

        final PlayerSkin replace = ModuleNetEaseSkin.getPlayerSkin(this);
        if (replace != ModuleNetEaseSkin.defaultSkin) cir.setReturnValue(replace);
    }

}
