package me.zuogeren.slimefunhelperplus.module.settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.matl114.hacks.api.BaseModule;
import me.matl114.plugins.internal.PluginManager;
import me.matl114.utils.ChatUtils;
import me.zuogeren.slimefunhelperplus.PlusPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * 插件侧模块名称映射表：根据 {@link PluginSettingsModule#isChineseModuleNames()} 统一解析英文/中文模块名。
 *
 * <p>只控制模块自身的显示名称，不改写介绍、tooltip 与配置项文案；
 * {@code NewClickGui} 缺乏准确译名，即便切到中文模式仍保持英文不动。
 */
public final class PluginModuleNames {
    private record ModuleNameSpec(
            String moduleId,
            @Nullable String toggleKey,
            @Nullable String enablePath,
            String englishName,
            String chineseName) {
        String displayName(boolean chinese) {
            return chinese ? chineseName : englishName;
        }
    }

    private static final List<ModuleNameSpec> SPECS = List.of(
            new ModuleNameSpec(
                    "CarbonRender",
                    "module-toggle.carbon-render.enable",
                    "carbon-render.enable",
                    "CarbonRender",
                    "碳 · 物品实例化"),
            new ModuleNameSpec(
                    "CustomModel",
                    "module-toggle.custom-model.enable",
                    "custom-model.enable",
                    "CustomModel",
                    "自定义模型"),
            new ModuleNameSpec(
                    "FriendWine",
                    "module-toggle.friend-wine.enable",
                    "friend-wine.enable",
                    "FriendWine",
                    "朋友的酒"),
            new ModuleNameSpec(
                    "LocatorTrack",
                    "module-toggle.locator-bar.enable",
                    "locator-bar.enable",
                    "LocatorTrack",
                    "定位栏坐标反推"),
            new ModuleNameSpec("ConfluxMapHelper", null, null, "ConfluxMapHelper", "Conflux地图助手"),
            // NewClickGui 没有准确中文译名，中文模式下仍保持英文
            new ModuleNameSpec("NewClickGui", null, null, "NewClickGui", "NewClickGui"),
            new ModuleNameSpec("Settings", null, null, "Settings", "插件设置"));

    private static final Map<String, ModuleNameSpec> BY_MODULE_ID = new LinkedHashMap<>();
    private static final Map<String, ModuleNameSpec> BY_TOGGLE_KEY = new LinkedHashMap<>();
    private static final Map<String, ModuleNameSpec> BY_ENABLE_PATH = new LinkedHashMap<>();

    static {
        for (ModuleNameSpec spec : SPECS) {
            BY_MODULE_ID.put(spec.moduleId(), spec);
            if (spec.toggleKey() != null) {
                BY_TOGGLE_KEY.put(spec.toggleKey(), spec);
            }
            if (spec.enablePath() != null) {
                BY_ENABLE_PATH.put(spec.enablePath(), spec);
            }
        }
    }

    private PluginModuleNames() {}

    /** 若传入的是本插件注册的模块，返回按当前语言设置解析后的名称；否则返回 {@code null}。 */
    @Nullable
    public static String displayNameIfPlugin(@Nullable BaseModule module) {
        if (module == null || !PlusPlugin.PLUGIN_ID.equals(PluginManager.getInstance().ownerOf(module))) {
            return null;
        }
        ModuleNameSpec spec = BY_MODULE_ID.get(module.getName());
        return spec == null ? null : spec.displayName(PluginSettingsModule.isChineseModuleNames());
    }

    /** 若 {@code translationKey} 属于本插件的 {@code module-toggle.*} 条目，返回当前语言名称；否则返回 {@code null}。 */
    @Nullable
    public static String toggleDisplayNameIfPlugin(@Nullable String translationKey) {
        ModuleNameSpec spec = translationKey == null ? null : BY_TOGGLE_KEY.get(translationKey);
        return spec == null ? null : spec.displayName(PluginSettingsModule.isChineseModuleNames());
    }

    /**
     * 用于 {@code ModuleSettings#sendToggleMessage}：同时识别 {@code module-toggle.<path>} 与
     * {@code HotKeyUtils#wrapFlagAsToggle} 传入的原始 {@code <path>}。
     */
    @Nullable
    public static String toggleDisplayNameByKeyOrPath(@Nullable String keyOrPath) {
        if (keyOrPath == null) {
            return null;
        }
        ModuleNameSpec spec = BY_TOGGLE_KEY.get(keyOrPath);
        if (spec == null) {
            spec = BY_ENABLE_PATH.get(keyOrPath);
        }
        return spec == null ? null : spec.displayName(PluginSettingsModule.isChineseModuleNames());
    }

    /** 为 {@code /sfh toggle} 补全提供展示名：插件条目走本模块设置，宿主条目走原版语言解析。 */
    public static String resolveToggleCommandDisplay(String translationKey) {
        String pluginName = toggleDisplayNameIfPlugin(translationKey);
        return pluginName != null ? pluginName : ChatUtils.parseTranslation(translationKey);
    }

    /** 判定 {@code /sfh toggle <arg>} 是否命中条目：插件模块同时接受当前显示名与英文/中文双名，方便手敲。 */
    public static boolean matchesToggleCommand(String translationKey, String input) {
        ModuleNameSpec spec = translationKey == null ? null : BY_TOGGLE_KEY.get(translationKey);
        if (spec != null) {
            return Objects.equals(input, spec.englishName())
                    || Objects.equals(input, spec.chineseName())
                    || Objects.equals(input, ChatUtils.parseTranslation(translationKey));
        }
        return Objects.equals(input, ChatUtils.parseTranslation(translationKey));
    }
}
