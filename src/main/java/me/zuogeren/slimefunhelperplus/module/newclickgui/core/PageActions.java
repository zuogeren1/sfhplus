package me.zuogeren.slimefunhelperplus.module.newclickgui.core;

import java.util.List;
import me.matl114.gui.interfaces.ElementFactory;
import me.matl114.hacks.api.BaseModule;
import me.matl114.hacks.modules.task.ConfigManager;
import me.zuogeren.slimefunhelperplus.hostgui.HostEditors;
import me.zuogeren.slimefunhelperplus.module.custommodel.CustomModelManager;
import me.zuogeren.slimefunhelperplus.module.custommodel.CustomModelModule;
import me.zuogeren.slimefunhelperplus.module.custommodel.ModelCatalog;
import me.zuogeren.slimefunhelperplus.module.custommodel.model.ModelRuntime;

/**
 * 配置页动作行模型与模块动作供给：把「配置表之外的操作入口」从具体皮肤屏中拆出来。
 */
public final class PageActions {
    private PageActions() {}

    /**
     * 页面自己的动作：配置表之外的一个操作，整行可点。
     *
     * @param label  行名，写动作对应的对象
     * @param detail 右侧那句现状（人数、当前字体……），没有就传空串
     * @param chip   行尾可点块的文案，写动词
     * @param tip    悬停说明，空串表示不出说明框
     */
    public record Action(String label, String detail, String chip, String tip, Runnable run) {}

    /** 按宿主或自研模块补充页面专属动作行；没有则返回空表。 */
    public static List<Action> forModule(BaseModule module, ElementFactory popupFactory) {
        if (module instanceof ConfigManager manager) {
            return List.of(
                    new Action(
                            "保存配置快照",
                            "把当前配置写成一份快照",
                            "保存",
                            "选一个位置保存快照：当前所有配置一起写成文件，以后可以整份读回来。",
                            () -> HostEditors.openSnapshot(manager, popupFactory, true)),
                    new Action(
                            "读取配置快照",
                            "从快照文件整份恢复",
                            "读取",
                            "选一个快照文件读回来：文件里的配置会整份套到当前配置上。",
                            () -> HostEditors.openSnapshot(manager, popupFactory, false)));
        }
        if (module instanceof CustomModelModule custom) {
            String current = custom.model.get();
            return List.of(
                    new Action(
                            "模型：" + (current.isBlank() ? "未选择" : ModelCatalog.displayName(current)),
                            "点击切换到下一个",
                            "▸",
                            "在 config/slimefunhelper-plus/models 里循环切换；也可以直接在「模型」一行里填文件名。",
                            () -> custom.model.set(ModelCatalog.cycle(custom.model.get()))),
                    new Action(
                            "重新扫描模型",
                            "models 目录：" + ModelCatalog.names().size() + " 个",
                            "↻",
                            "重新扫描模型目录；扫描结果用于切换与选择。",
                            ModelCatalog::rescan),
                    new Action(
                            "重载当前模型",
                            current.isBlank() ? "还没有选择模型" : current,
                            "⟳",
                            "重新解密、解析并注册当前模型。",
                            () -> CustomModelManager.reloadAsync(custom.model.get())),
                    new Action(
                            "选择模型",
                            current.isBlank() ? "还没有选择模型" : ModelCatalog.displayName(current),
                            "选",
                            "打开模型选择页：点卡片即切换模型，预览区右上角的齿轮进动作预览；热键在「模型选择热键」一行绑定。",
                            custom::openModelPicker),
                    new Action(
                            "动画轮盘",
                            currentExtraLabel(),
                            "◎",
                            "打开轮盘切换服装预设与额外动作；热键在「动画轮盘热键」一行绑定。",
                            custom::openRoulette));
        }
        return List.of();
    }

    private static String currentExtraLabel() {
        ModelRuntime runtime = CustomModelManager.runtime();
        if (runtime == null) {
            return "模型未加载";
        }
        String variant = labelOf(runtime, runtime.activeVariantKey());
        String extra = labelOf(runtime, runtime.activeExtraKey());
        return (variant.isEmpty() ? "默认服装" : variant) + (extra.isEmpty() ? "" : " · " + extra);
    }

    private static String labelOf(ModelRuntime runtime, String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        for (ModelRuntime.ExtraOption option : runtime.extras()) {
            if (option.key().equals(key)) {
                return option.label();
            }
        }
        return key;
    }
}
