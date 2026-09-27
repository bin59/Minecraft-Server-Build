package com.nangua.quickmenu.menu;

/**
 * 菜单项可执行的动作类型。
 *
 * <p>前四项为无条件动作，任何客户端都会执行。
 * 后四项为<b>平台条件动作</b>：仅当玩家属于对应客户端时才执行，
 * 用于解决「同一菜单项在两端需要调用不同指令」的场景。
 *
 * <p>典型用例：基岩端有 BedrockPlayerSupport 提供的原生表单指令（如 {@code /warpgui}），
 * Java 端则应调用 EssentialsX 的普通指令（如 {@code /warp}）。
 * 若两端都配成 {@code /warpgui}，Java 玩家点击后会因指令不存在而无反应。
 */
public enum ActionType {

    // ==================== 无条件动作 ====================

    /** 以玩家身份执行命令（value 不含斜杠） */
    PLAYER_COMMAND,

    /** 以控制台身份执行命令（value 不含斜杠） */
    CONSOLE_COMMAND,

    /** 打开另一个菜单（value 为菜单 id） */
    OPEN_MENU,

    /** 向玩家发送消息（value 为已完成颜色码转换的文本） */
    MESSAGE,

    /** 关闭当前界面 / 表单 */
    CLOSE,

    /**
     * 打开在线玩家选择器（动态列出在线玩家，点击后执行命令模板）。
     * value 为命令模板，其中 {@code {target}} 会被替换为玩家点击的玩家名。
     * 例：{@code [player-selector] tpa {target}}
     */
    PLAYER_SELECTOR,

    /**
     * 打开背包物品选择器（动态列出玩家背包中的物品，点击后执行命令模板）。
     * value 为命令模板，其中 {@code {item}} 会被替换为所点物品的材质名（小写）。
     * 例：{@code [item-selector] worth {item}}
     */
    ITEM_SELECTOR,

    /**
     * 拍卖行上架指定物品（value 为 "价格 材质名"，空格分隔）。
     * 插件自动在玩家背包（含快捷栏/盔甲槽）中查找该材质物品，临时换到主手
     * 执行 {@code /ah sell 价格}，再还原主手——解决「菜单触发物品占着主手，
     * 无法直接 /ah sell 其他物品」的问题。
     * 例：{@code [ah-sell] 5000 elytra}
     */
    AH_SELL,

    // ==================== 平台条件动作 ====================

    /** 仅基岩玩家：以玩家身份执行命令 */
    BEDROCK_PLAYER_COMMAND,

    /** 仅 Java 玩家：以玩家身份执行命令 */
    JAVA_PLAYER_COMMAND,

    /** 仅基岩玩家：以控制台身份执行命令 */
    BEDROCK_CONSOLE_COMMAND,

    /** 仅 Java 玩家：以控制台身份执行命令 */
    JAVA_CONSOLE_COMMAND;

    /**
     * 该动作是否只在特定平台生效。
     *
     * <p>用于配置校验：若一个菜单项的所有动作都是平台条件动作，
     * 且未覆盖两端，则另一端的玩家点击后不会有任何反应。
     */
    public boolean isPlatformSpecific() {
        return this == BEDROCK_PLAYER_COMMAND
                || this == JAVA_PLAYER_COMMAND
                || this == BEDROCK_CONSOLE_COMMAND
                || this == JAVA_CONSOLE_COMMAND;
    }

    /**
     * 该动作是否仅面向基岩玩家。
     */
    public boolean isBedrockOnly() {
        return this == BEDROCK_PLAYER_COMMAND || this == BEDROCK_CONSOLE_COMMAND;
    }

    /**
     * 该动作是否仅面向 Java 玩家。
     */
    public boolean isJavaOnly() {
        return this == JAVA_PLAYER_COMMAND || this == JAVA_CONSOLE_COMMAND;
    }

    /**
     * 该动作是否以控制台身份执行（无视权限系统，需谨慎使用）。
     */
    public boolean isConsoleAction() {
        return this == CONSOLE_COMMAND
                || this == BEDROCK_CONSOLE_COMMAND
                || this == JAVA_CONSOLE_COMMAND;
    }
}
