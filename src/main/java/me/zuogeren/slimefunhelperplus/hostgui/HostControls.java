package me.zuogeren.slimefunhelperplus.hostgui;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import me.matl114.gui.Constants;
import me.matl114.gui.FilterService;
import me.matl114.gui.basic.ButtonAction;
import me.matl114.gui.basic.DrawableWidget;
import me.matl114.gui.basic.ElementHandler;
import me.matl114.gui.basic.ExecutableWidget;
import me.matl114.gui.basic.SubScreenWidget;
import me.matl114.gui.elements.IconElement;
import me.matl114.managers.config.Ref;
import me.matl114.managers.input.KeyCode;
import me.matl114.renderer.TextRenderers;
import me.matl114.renderer.text.TextDrawer;
import me.matl114.utils.config.AttrKeyValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * 宿主控件与 UI 原语的统一入口：插件侧不直接零散构造宿主控件，一律走这里。
 *
 * <p>宿主升级时只改本类与同包门面，不用在二十来个文件里找漏。这里只做「创建 + 统一配置 + 图标/字体桥接」，
 * 不接管绘制与命中——外观仍归各风格自己画。
 */
public final class HostControls {
    private HostControls() {}

    /**
     * 原版输入框：只负责收字（输入法、退格、粘贴、光标），框体由调用方自绘。
     *
     * <p>无边框是自绘界面的前提：边框、聚焦环、底色都在外部绘制，留着原版边框会与自绘框错开一像素。
     */
    public static EditBox editBox(int x, int y, int w, int maxLength, String hint) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, w, 9, Component.empty());
        box.setBordered(false);
        box.setMaxLength(maxLength);
        if (hint != null && !hint.isEmpty()) {
            box.setHint(Component.literal(hint));
        }
        return box;
    }

    /** 同上，但提示文案已经是 {@link Component}（走翻译键时用这个）。 */
    public static EditBox editBoxText(int x, int y, int w, int maxLength, Component hint) {
        EditBox box = new EditBox(Minecraft.getInstance().font, x, y, w, 9, Component.empty());
        box.setBordered(false);
        box.setMaxLength(maxLength);
        if (hint != null) {
            box.setHint(hint);
        }
        return box;
    }

    /**
     * 支持按 Enter 提交的原版输入框：同样无边框，框体由调用方自绘。
     *
     * <p>原版 {@link EditBox} 不接 Enter，而按键只派给拿到焦点的那一个控件，所以提交回调必须在输入框这一层拦截。
     * Esc 不拦截，仍交给所属屏幕按原样关闭。
     */
    public static EditBox submitEditBox(int x, int y, int w, int maxLength, Runnable onSubmit) {
        EditBox box = new SubmitBox(Minecraft.getInstance().font, x, y, w, 9, Component.empty(), onSubmit);
        box.setBordered(false);
        box.setMaxLength(maxLength);
        return box;
    }

    /**
     * 程序写值：{@code setValue} 会同步回调 responder，写回配置时会二次触发。
     *
     * <p>先摘 responder、文本确实变化时再写回、最后无论如何都挂回 responder：
     * 这样不会打断光标位置，也能避免「切到另一格时旧值误写新目标」。
     */
    public static void setValueQuietly(EditBox box, String text, Consumer<String> responder) {
        String next = text == null ? "" : text;
        box.setResponder(null);
        try {
            if (!next.equals(box.getValue())) {
                box.setValue(next);
            }
        } finally {
            if (responder != null) {
                box.setResponder(responder);
            }
        }
    }

    /**
     * 拼音搜索：宿主那套支持全拼、首字母、带空格三种写法，各模块搜索栏共享同一套匹配规则。
     */
    public static boolean nameMatch(String text, String needle) {
        return FilterService.nameMatch(text, needle);
    }

    /** 配置项的「对象 ↔ 文本/属性」转换工厂。 */
    public static AttrKeyValue<?> keyValue(Ref<?> ref, String keyName) {
        return ref.createKeyValue(keyName);
    }

    /** 行尾重置按钮的图标。 */
    public static Identifier resetIcon() {
        return Constants.RESET_BUTTON_SPRITE;
    }

    /** 列表入口按钮图标。 */
    public static Identifier listTagIcon() {
        return Constants.LIST_TAG_SPRITE;
    }

    /** 格式化参数提示图标。 */
    public static Identifier formattingIcon() {
        return Constants.FORMATTING_TEXTURE_SPRITE;
    }

    /** 编辑器图标。 */
    public static Identifier editorIcon() {
        return Constants.EDITOR_SPRITE;
    }

    /** 宿主列表编辑按钮的默认悬停说明。 */
    public static List<Component> openListEditTooltips() {
        return Constants.openListEditTooltips();
    }

    /**
     * 把宿主通过 {@code WidgetUtils} 直接构造的绿色列表图标按钮替换成指定风格的渲染处理器。
     *
     * <p>只替换纹理命中 {@link Constants#LIST_TAG_SPRITE} 的「图标 + 点击」按钮，其它语义图标保持原样。
     */
    public static void restyleListButtons(
            List<DrawableWidget> widgets, Function<ButtonAction, ElementHandler> factory) {
        if (widgets == null || factory == null) {
            return;
        }
        for (DrawableWidget widget : widgets) {
            if (!(widget instanceof SubScreenWidget sub)) {
                continue;
            }
            for (DrawableWidget child : sub.childrenRenderOrder()) {
                if (!(child instanceof ExecutableWidget exec) || !(exec.getHandler() instanceof IconElement icon)) {
                    continue;
                }
                if (!Constants.LIST_TAG_SPRITE.equals(icon.getTextureId(null, child, false))) {
                    continue;
                }
                exec.setRenderHandler(
                        factory.apply((element, target, mouseButton) -> icon.onClick(target, 0, 0, mouseButton)));
            }
        }
    }

    /** 当前生效的宿主文字绘制器。 */
    public static TextDrawer textDrawer() {
        return TextRenderers.get();
    }

    /** 穿透委托层取回底层真实文字绘制器实例，供字体切换失效检测用。 */
    public static TextDrawer resolveTextDrawer(TextDrawer drawer) {
        return TextRenderers.resolve(drawer);
    }

    /** 原生 tooltip 使用的 Minecraft {@link Font}；自定义贴图字体不支持原生 tooltip 时退回原版字体。 */
    public static Font tooltipFont() {
        Font font = TextRenderers.get().unwrap();
        return font != null ? font : Minecraft.getInstance().font;
    }

    private static final class SubmitBox extends EditBox {
        private final Runnable submit;

        SubmitBox(Font font, int x, int y, int width, int height, Component message, Runnable submit) {
            super(font, x, y, width, height, message);
            this.submit = submit == null ? () -> {} : submit;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int key = event.key();
            if (key == KeyCode.KEY_ENTER || key == KeyCode.KEY_KP_ENTER) {
                this.submit.run();
                return true;
            }
            return super.keyPressed(event);
        }
    }
}
