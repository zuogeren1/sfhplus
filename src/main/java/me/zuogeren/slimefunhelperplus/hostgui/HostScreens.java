package me.zuogeren.slimefunhelperplus.hostgui;

import me.matl114.accessors.gui.ScreenAccess;
import me.matl114.utils.ClientUtils;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

/**
 * 宿主屏幕跳转与运行期子控件挂载的统一出口。
 *
 * <p>集中收拢 {@link ScreenAccess#of(Screen)}，宿主升级屏幕访问接口时只需改本类。
 */
public final class HostScreens {
    private HostScreens() {}

    /** 当前打开的屏幕。 */
    public static Screen current() {
        return ClientUtils.getScreen();
    }

    /** 从当前屏幕打开新屏，并由宿主记录来路以便关闭返回。 */
    public static void open(Screen screen) {
        if (screen != null) {
            ScreenAccess.of(screen).openFromCurrent();
        }
    }

    /** 在同一个导航层级里替换当前屏幕（不额外压一层返回栈）。 */
    public static void switchTo(Screen current, Screen next) {
        if (current != null && next != null) {
            ScreenAccess.of(current).switchToScreen(next);
        }
    }

    /** 运行期向屏幕追加可绘制且可交互的原生子控件（如临时输入框）。 */
    public static <T extends GuiEventListener & Renderable & NarratableEntry> T addChild(Screen screen, T child) {
        return ScreenAccess.of(screen).addDrawableChildTo(child);
    }

    /** 从屏幕卸载原生子控件。 */
    public static void removeChild(Screen screen, GuiEventListener child) {
        if (screen != null && child != null) {
            ScreenAccess.of(screen).removeChildFrom(child);
        }
    }
}
