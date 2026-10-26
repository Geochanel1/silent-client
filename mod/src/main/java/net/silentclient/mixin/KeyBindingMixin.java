package net.silentclient.mixin;

import net.minecraft.client.option.KeyBinding;
import net.silentclient.util.ClickTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {

    /** Καλείται σε κάθε πάτημα (και των κουμπιών του ποντικιού) -> για το CPS. */
    @Inject(method = "onKeyPressed", at = @At("HEAD"))
    private static void silent$onPress(int code, CallbackInfo ci) {
        ClickTracker.onPress(code);
    }
}
