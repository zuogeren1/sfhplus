package me.zuogeren.slimefunhelperplus.module.newclickgui.ui;

import java.util.function.Function;
import me.matl114.gui.interfaces.ElementFactory;
import me.matl114.hacks.api.BaseModule;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * 一套界面风格：一个 id，加上它自己的主屏、分类页、模块配置页与可选弹窗工厂。
 *
 * <p>跨风格契约完全不感知具体主题的颜色字段或几何参数，切角、圆角、调色板等视觉定义全部收敛在
 * 各自 {@code style/<名称>/} 包内。宿主预设弹窗的外观工厂由风格按需提供，未提供时退回宿主默认工厂。
 */
public record GuiStyle(
        Identifier id,
        Function<Settings, Screen> mainScreen,
        PageFactory pages,
        ModuleConfigFactory moduleConfig,
        Function<Settings, ElementFactory> popupElementFactory) {

    public GuiStyle(
            Identifier id,
            Function<Settings, Screen> mainScreen,
            PageFactory pages,
            ModuleConfigFactory moduleConfig) {
        this(id, mainScreen, pages, moduleConfig, null);
    }

    /** 顶部分类 → 该风格下的一页；没做的分类返回 {@code null}，由调用方退回宿主的旧页面。 */
    @FunctionalInterface
    public interface PageFactory {
        Screen create(String key, Settings settings);
    }

    /** 模块配置页的工厂。 */
    @FunctionalInterface
    public interface ModuleConfigFactory {
        Screen create(BaseModule module, Settings settings);
    }

    /** 打开这一套风格的主屏。 */
    public Screen openMain(Settings settings) {
        return mainScreen.apply(settings);
    }

    /** 打开这一套风格下的某个分类页。 */
    public Screen openPage(String key, Settings settings) {
        return pages == null ? null : pages.create(key, settings);
    }

    /** 打开某个模块的配置页：控件外观同样归风格。 */
    public Screen openModuleConfig(BaseModule module, Settings settings) {
        return moduleConfig == null ? null : moduleConfig.create(module, settings);
    }

    /** 取这一套风格给宿主二级弹窗用的外观工厂；未显式提供时退回调用方传入的默认工厂。 */
    public ElementFactory elementFactory(Settings settings, ElementFactory fallback) {
        ElementFactory factory = popupElementFactory == null ? null : popupElementFactory.apply(settings);
        return factory != null ? factory : fallback;
    }
}