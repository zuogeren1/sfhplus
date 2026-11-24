package me.zuogeren.slimefunhelperplus.hostgui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 字符串型配置的候选值注册表：键是配置完整路径（如 {@code custom-model.model}），值是候选列表提供者。
 *
 * <p>枚举型配置自带 {@code getEnumConstants()}，但字符串型的候选往往来自运行时扫描，
 * 需要模块主动注册；宿主配置页注入与各个新界面风格共用这张表，避免任何业务模块反向耦合具体界面。
 */
public final class StringChoicesRegistry {
    private static final Map<String, Supplier<List<String>>> REGISTRY = new ConcurrentHashMap<>();

    private StringChoicesRegistry() {}

    /** 给指定路径注册候选值；传 {@code null} 取消注册。 */
    public static void register(String path, Supplier<List<String>> choices) {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (choices == null) {
            REGISTRY.remove(path);
        } else {
            REGISTRY.put(path, choices);
        }
    }

    /** 按配置路径取当前候选列表；未注册或返回为空时给空列表。 */
    public static List<String> ofPath(String path) {
        if (path == null || path.isEmpty()) {
            return List.of();
        }
        Supplier<List<String>> supplier = REGISTRY.get(path);
        if (supplier == null) {
            return List.of();
        }
        List<String> choices = supplier.get();
        return choices == null ? List.of() : choices;
    }
}
