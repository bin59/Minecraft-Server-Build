import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * plugin.yml 语法与字段校验探针。
 * 仅用于本地构建验证，不随插件打包。
 */
public class PluginYmlProbe {

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = new FileInputStream(file)) {
            root = yaml.load(in);
        }
        if (root == null) {
            System.out.println("FATAL: plugin.yml 为空");
            System.exit(2);
            return;
        }

        int errors = 0;

        String name = str(root.get("name"));
        String main = str(root.get("main"));
        String version = str(root.get("version"));
        String apiVersion = str(root.get("api-version"));

        System.out.println("name      = " + name);
        System.out.println("main      = " + main);
        System.out.println("version   = " + version);
        System.out.println("api-version = " + apiVersion);

        if (name.isEmpty()) { System.out.println("[X] 缺少 name"); errors++; }
        if (main.isEmpty()) { System.out.println("[X] 缺少 main"); errors++; }
        if (version.isEmpty()) { System.out.println("[X] 缺少 version"); errors++; }
        if (apiVersion.isEmpty()) { System.out.println("[X] 缺少 api-version"); errors++; }

        // main 类名格式校验
        if (!main.isEmpty() && !main.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*")) {
            System.out.println("[X] main 不是合法的类全限定名: " + main);
            errors++;
        }

        // 验证 main 类是否真的存在于 jar 中
        String classPath = main.replace('.', '/') + ".class";
        boolean found = false;
        java.util.zip.ZipFile zip = new java.util.zip.ZipFile(new File(args[1]));
        java.util.Enumeration<? extends java.util.zip.ZipEntry> en = zip.entries();
        while (en.hasMoreElements()) {
            if (en.nextElement().getName().equals(classPath)) { found = true; break; }
        }
        zip.close();
        if (!found) {
            System.out.println("[X] 主类未在 jar 中找到: " + classPath);
            errors++;
        } else {
            System.out.println("[OK] 主类存在于 jar: " + classPath);
        }

        // commands 校验
        Object cmds = root.get("commands");
        if (cmds instanceof Map) {
            Map<?, ?> cmdMap = (Map<?, ?>) cmds;
            System.out.println("commands  = " + cmdMap.keySet());
            for (Object key : cmdMap.keySet()) {
                String cmd = String.valueOf(key);
                if (cmd.contains(":")) {
                    System.out.println("[X] 命令名含非法字符 ':' -> " + cmd);
                    errors++;
                }
                Object val = cmdMap.get(key);
                if (val instanceof Map) {
                    Map<?, ?> opts = (Map<?, ?>) val;
                    if (opts.containsKey("aliases")) {
                        Object aliases = opts.get("aliases");
                        if (aliases instanceof List) {
                            System.out.println("  /" + cmd + " 别名: " + aliases);
                        }
                    }
                }
            }
        } else {
            System.out.println("[!] 未定义 commands");
        }

        // permissions 校验
        Object perms = root.get("permissions");
        if (perms instanceof Map) {
            Map<?, ?> permMap = (Map<?, ?>) perms;
            System.out.println("permissions = " + permMap.keySet());
            for (Object key : permMap.keySet()) {
                Object val = permMap.get(key);
                if (val instanceof Map) {
                    Object def = ((Map<?, ?>) val).get("default");
                    if (def != null) {
                        String d = String.valueOf(def).toLowerCase();
                        if (!d.equals("true") && !d.equals("false") && !d.equals("op") && !d.equals("not op")) {
                            System.out.println("[X] 权限 " + key + " 的 default 值非法: " + def);
                            errors++;
                        }
                    }
                }
            }
        }

        // softdepend 校验
        Object soft = root.get("softdepend");
        if (soft instanceof List) {
            System.out.println("softdepend = " + soft);
        }

        System.out.println("\n=== plugin.yml 校验结果: " + (errors == 0 ? "全部通过" : errors + " 个错误") + " ===");
        if (errors > 0) System.exit(1);
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
