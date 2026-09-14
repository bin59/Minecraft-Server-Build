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
