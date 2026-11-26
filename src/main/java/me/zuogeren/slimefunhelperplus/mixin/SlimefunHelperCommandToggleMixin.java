package me.zuogeren.slimefunhelperplus.mixin;

import java.util.stream.Stream;
import me.matl114.hacks.ChatTasks;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModuleEntry;
import me.matl114.hacks.api.ModuleGroup;
import me.matl114.hacks.modules.HackModules;
import me.matl114.hacks.utils.HotKeyUtils;
import me.matl114.utils.ChatUtils;
import me.matl114.utils.Debug;
import me.matl114.utils.commands.params.ArgumentInputStream;
import me.zuogeren.slimefunhelperplus.module.settings.PluginModuleNames;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 让 {@code /sfh toggle} 的补全与匹配支持插件 {@code Settings} 的模块命名语言切换。 */
@Mixin(ChatTasks.SlimefunHelperCommand.class)
public class SlimefunHelperCommandToggleMixin {

    @Inject(
            method = "supplyModule()Ljava/util/stream/Stream;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void sfhplus$supplyModuleNames(CallbackInfoReturnable<Stream<String>> callbackInfo) {
        Stream<String> stream = HackModules.getModuleGroups().stream()
                .flatMap(group -> group.getModules().stream())
                .flatMap(module -> module.getModuleEntries()
                        .map(ModuleEntry::getTranslationKey)
                        .map(PluginModuleNames::resolveToggleCommandDisplay));
        callbackInfo.setReturnValue(stream);
    }

    @Inject(
            method = "onToggle(Lme/matl114/utils/commands/params/ArgumentInputStream;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private void sfhplus$onToggle(ArgumentInputStream re, CallbackInfo callbackInfo) {
        String moduleName = re.nextNonnullString();
        for (ModuleGroup moduleGroup : HackModules.getModuleGroups()) {
            for (BaseModule module : moduleGroup.getModules()) {
                for (ModuleEntry moduleEntry : module.getModuleEntries().toList()) {
                    if (PluginModuleNames.matchesToggleCommand(moduleEntry.getTranslationKey(), moduleName)) {
                        HotKeyUtils.wrapFlagAsToggle(moduleEntry.getPath(), moduleEntry.getFlagRef()).run();
                        callbackInfo.cancel();
                        return;
                    }
                }
            }
        }
        Debug.chat(ChatUtils.stringToText("&e找不到模块项: " + moduleName));
        callbackInfo.cancel();
    }
}
