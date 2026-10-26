package net.silentclient.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.silentclient.SilentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    /** Ζωγραφίζουμε τα HUD mods αφού ζωγραφίσει το παιχνίδι το δικό του HUD. */
    @Inject(method = "render(F)V", at = @At("TAIL"))
    private void silent$onRender(float tickDelta, CallbackInfo ci) {
        if (SilentClient.INSTANCE != null) SilentClient.INSTANCE.onRenderHud();
    }
}
