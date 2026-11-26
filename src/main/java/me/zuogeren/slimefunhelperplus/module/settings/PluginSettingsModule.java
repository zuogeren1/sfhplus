package me.zuogeren.slimefunhelperplus.module.settings;

import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.api.ModulePath;
import me.matl114.managers.config.Config;
import me.matl114.managers.config.ConfigEnum;
import me.matl114.managers.config.EnumRef;
import me.zuogeren.slimefunhelperplus.config.PluginConfig;

/**
 * SlimeFunHelper+ 插件的统一设置模块。
 *
 * <p>集中存放跨模块公共配置（不绑总开关，样式与 {@code NewClickGui}、宿主 {@code ClientExtra} 一致），
 * 目前先提供插件模块显示名称的语言切换开关（默认英文，与宿主未挂载汉化包时的风格保持一致）。
 */
public class PluginSettingsModule extends BaseModule {
    public static PluginSettingsModule INSTANCE;

    private static final Config CONFIG = PluginConfig.get();

    public final ModulePath root = makePath(CONFIG, "settings");

    /** 插件模块名语言：仅切换插件模块显示名称，介绍、tooltip 与配置项保持中文不动。 */
    public final EnumRef<LanguageMode> language = builder(root.add("language"), LanguageMode.class)
            .defaultValue(LanguageMode.ENGLISH)
            .build();

    public PluginSettingsModule() {
        super("Settings");
        INSTANCE = this;
    }

    /** 当前是否应将插件模块显示名称切换为中文（默认英文）。 */
    public static boolean isChineseModuleNames() {
        PluginSettingsModule module = INSTANCE;
        return module != null && module.language != null && module.language.get() == LanguageMode.CHINESE;
    }

    public enum LanguageMode implements ConfigEnum {
        ENGLISH,
        CHINESE;

        @Override
        public String getConfigEnumType() {
            return "sfhplus_module_language";
        }
    }
}
