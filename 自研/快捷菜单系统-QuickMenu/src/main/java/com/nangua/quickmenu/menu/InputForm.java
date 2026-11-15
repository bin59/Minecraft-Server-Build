package com.nangua.quickmenu.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * 基岩端「参数输入表单」定义（config.yml 的 {@code input-forms:} 段）。
 *
 * <p>原先需要参数的指令只能做成「关闭界面 + 聊天栏打一行用法」，
 * 玩家还得自己手打。这里把参数做成 CustomForm 里的输入框 / 下拉框，
 * 玩家点按钮 → 填参数 → 提交即执行命令，全程不打字。
 *
 * <p>命令模板用 {@code {字段key}} 占位，例如
 * {@code eco give {player} {amount}}。Java 玩家（或表单不可用）点击时
 * 自动降级为 {@code fallback} 文本提示，行为与旧版一致。
 */
public final class InputForm {

    /** 字段类型：文本框 / 固定下拉 / 在线玩家下拉 */
    public enum FieldType {
        INPUT,
        DROPDOWN,
        PLAYER;

        public static FieldType parse(String raw) {
            if (raw == null) {
                return INPUT;
            }
            switch (raw.trim().toLowerCase()) {
                case "dropdown":
                    return DROPDOWN;
                case "player":
                    return PLAYER;
                default:
                    return INPUT;
            }
        }
    }

    public static final class Field {
        private final String key;
        private final FieldType type;
        private final String label;
        private final String placeholder;
        private final String defaultValue;
        private final List<String> options;

        public Field(String key, FieldType type, String label,
                     String placeholder, String defaultValue, List<String> options) {
            this.key = key == null ? "" : key;
            this.type = type == null ? FieldType.INPUT : type;
            this.label = label == null ? "" : label;
            this.placeholder = placeholder == null ? "" : placeholder;
            this.defaultValue = defaultValue == null ? "" : defaultValue;
            this.options = options == null ? new ArrayList<>() : options;
        }

        public String getKey() {
            return key;
        }

        public FieldType getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public List<String> getOptions() {
            return options;
        }
    }

    private final String id;
    private String title = "";
    private String content = "";
    private String command = "";
    /** Java 端 / 表单不可用时逐行发送的降级提示（与旧版 [message] 完全一致） */
    private List<String> fallback = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();

    public InputForm(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content == null ? "" : content;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command == null ? "" : command;
    }

    public List<String> getFallback() {
        return fallback;
    }

    public void setFallback(List<String> fallback) {
        this.fallback = fallback == null ? new ArrayList<>() : fallback;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void addField(Field field) {
        if (field != null) {
            fields.add(field);
        }
    }
}
