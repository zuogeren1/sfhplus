package me.zuogeren.slimefunhelperplus.mixin;

import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.modules.task.ClickGui;
import me.zuogeren.slimefunhelperplus.module.settings.PluginModuleNames;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让宿主 ClickGui（含搜索、经典界面与滑杆界面）对插件模块名跟随 {@code Settings} 的语言切换。 */
@Mixin(ClickGui.class)
public class ClickGuiModuleNameMixin {

    @Inject(
            method = "getModuleName(Lme/matl114/hacks/api/BaseModule;)Lnet/minecraft/network/chat/Component;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void sfhplus$resolvePluginModuleName(
            BaseModule baseModule, CallbackInfoReturnable<Component> callbackInfo) {
        String resolved = PluginModuleNames.displayNameIfPlugin(baseModule);
        if (resolved != null) {
            callbackInfo.setReturnValue(Component.literal(resolved));
        }
    }
}
