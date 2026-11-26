package me.zuogeren.slimefunhelperplus.mixin;

import me.matl114.hacks.api.ModuleEntry;
import me.zuogeren.slimefunhelperplus.module.settings.PluginModuleNames;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让 {@link ModuleEntry#getDisplay()}（如 {@code ModuleListHud}）里的插件模块名跟随 {@code Settings} 语言切换。 */
@Mixin(ModuleEntry.class)
public abstract class ModuleEntryDisplayMixin {

    @Inject(
            method = "getDisplay()Lnet/minecraft/network/chat/MutableComponent;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void sfhplus$resolvePluginEntryDisplay(CallbackInfoReturnable<MutableComponent> callbackInfo) {
        String key = ((ModuleEntry) (Object) this).getTranslationKey();
        String resolved = PluginModuleNames.toggleDisplayNameIfPlugin(key);
        if (resolved != null) {
            callbackInfo.setReturnValue(Component.literal(resolved));
        }
    }
}
