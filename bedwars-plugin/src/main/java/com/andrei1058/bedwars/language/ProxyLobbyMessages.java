package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import com.andrei1058.bedwars.api.util.AdventureText;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 代理大厅返回失败提示的语言文件键与统一发送入口。 */
public final class ProxyLobbyMessages {
    private ProxyLobbyMessages() { }

    public enum Key {
        UNAVAILABLE("misc-proxy-lobby-unavailable",
                "&cProxy lobby is temporarily unavailable, please contact an administrator.",
                "&c代理大厅暂不可用，请联系管理员。");

        private final String path;
        private final String english;
        private final String chinese;

        Key(String path, String english, String chinese) {
            this.path = path;
            this.english = english;
            this.chinese = chinese;
        }

        public String path() { return path; }
    }

    /** 默认语言尚未初始化时（例如单元测试环境）使用的后备提示文本。 */
    public static final String UNAVAILABLE_FALLBACK = "§c代理大厅暂不可用，请联系管理员。";

    /** 同一玩家两次提示之间的最短间隔，避免站在门内或反复重试时刷屏。 */
    private static final long PROMPT_INTERVAL_MILLIS = 4000L;

    private static final Map<UUID, Long> LAST_PROMPT = new ConcurrentHashMap<>();

    /** 为语言文件补默认值：中文服务器使用中文模板，其他语言使用英文后备文本。 */
    public static void install(Language language) {
        boolean chinese = Language.isSimplifiedChineseIso(language.getIso());
        for (Key key : Key.values()) {
            language.getYml().addDefault(key.path, chinese ? key.chinese : key.english);
        }
        language.setComments(Key.UNAVAILABLE.path, "代理大厅返回失败时发送给玩家的提示；控制台会保留详细诊断。");
    }

    /** 发送“代理大厅暂不可用”提示；同一玩家在冷却窗口内不会重复收到。 */
    public static void sendUnavailable(Player player) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        pruneExpired(now);
        UUID playerId = player.getUniqueId();
        Long previous = LAST_PROMPT.get(playerId);
        if (previous != null && now - previous < PROMPT_INTERVAL_MILLIS) return;
        LAST_PROMPT.put(playerId, now);
        Language defaultLanguage = Language.getDefaultLanguage();
        AdventureText.send(player, defaultLanguage == null
                ? UNAVAILABLE_FALLBACK
                : Language.getMsg(player, Key.UNAVAILABLE.path()));
    }

    private static void pruneExpired(long now) {
        if (LAST_PROMPT.isEmpty()) return;
        Iterator<Map.Entry<UUID, Long>> iterator = LAST_PROMPT.entrySet().iterator();
        while (iterator.hasNext()) {
            Long timestamp = iterator.next().getValue();
            if (timestamp == null || now - timestamp >= PROMPT_INTERVAL_MILLIS) iterator.remove();
        }
    }
}
