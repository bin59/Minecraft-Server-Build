package com.nangua.quickmenu.menu;

/**
 * 一个具体动作：类型 + 参数。
 *
 * <p>动作与渲染方式无关，因此 Java 箱子界面与基岩原生表单点击后
 * 执行的是完全相同的行为。
 */
public final class Action {

    private final ActionType type;
    private final String value;

    public Action(ActionType type, String value) {
        this.type = type;
        this.value = value == null ? "" : value;
    }

    public ActionType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return type + "[" + value + "]";
    }
}
