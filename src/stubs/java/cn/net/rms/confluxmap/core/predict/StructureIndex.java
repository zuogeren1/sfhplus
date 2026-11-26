package cn.net.rms.confluxmap.core.predict;

/**
 * conflux-map 的结构点索引（编译期桩，不参与打包）。
 *
 * <p>本模块只用它当<b>签名里的类型</b>：0.1.18 起
 * {@code MapLocationMenuController#locationMenuButtonSpecs} 与
 * {@code MapLocationMenuController#runLocationAction} 都在 {@code Screen} 之前多带一个
 * {@code StructureIndex$Marker}（右键位置菜单命中的结构点，没命中时为 {@code null}），
 * mixin 注入要靠它凑出与真类完全一致的描述符。
 *
 * <p>javap 实测（0.1.16 与 0.1.18 一致，两类同在外层 jar 的嵌套
 * {@code META-INF/jars/common-x.y.z.jar} 里）：
 * <pre>
 *   public final class StructureIndex
 *   public record Marker(StructureType type, int blockX, int blockZ, int variant, State state)
 * </pre>
 * 真实嵌套类型是 record，这里只声明类型本身：本模块从不构造它，也不调用它的成员。
 */
public final class StructureIndex {
    private StructureIndex() {}

    /** 右键位置菜单里命中的结构点；菜单没命中结构点时该参数为 {@code null} */
    public static final class Marker {
        private Marker() {}
    }
}
