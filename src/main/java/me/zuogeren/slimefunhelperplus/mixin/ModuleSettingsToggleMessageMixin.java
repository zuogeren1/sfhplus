package me.zuogeren.slimefunhelperplus.mixin;

import me.matl114.hacks.modules.task.ModuleSettings;
import me.zuogeren.slimefunhelperplus.module.settings.PluginModuleNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 宿主 {@code HotKeyUtils#wrapFlagAsToggle} 传入的是配置项路径（如 {@code carbon-render.enable}），
 * 这里仅在开关播报时将其替换为插件模块名，避免影响配置页里该布尔项本身的中文名称。
 */
@Mixin(ModuleSettings.class)
public class ModuleSettingsToggleMessageMixin {

    @ModifyVariable(
            method = "sendToggleMessage(Ljava/lang/String;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            require = 0)
    private String sfhplus$resolvePluginToggleMessage(String message) {
        String resolved = PluginModuleNames.toggleDisplayNameByKeyOrPath(message);
        return resolved != null ? resolved : message;
    }
}
