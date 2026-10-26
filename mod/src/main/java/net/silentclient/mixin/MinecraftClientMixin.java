package net.silentclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.silentclient.SilentClient;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    /** Κάθε tick του παιχνιδιού (20 φορές το δευτερόλεπτο). */
    @Inject(method = "tick", at = @At("TAIL"))
    private void silent$onTick(CallbackInfo ci) {
        if (SilentClient.INSTANCE != null) SilentClient.INSTANCE.onTick();
    }

    /** Κάθε πάτημα πλήκτρου όταν παίζεις (όχι μέσα σε μενού). */
    @Inject(method = "handleKeyInput", at = @At("HEAD"))
    private void silent$onKey(CallbackInfo ci) {
        if (SilentClient.INSTANCE != null) {
            SilentClient.INSTANCE.onKey(Keyboard.getEventKey(), Keyboard.getEventKeyState());
        }
    }
}
