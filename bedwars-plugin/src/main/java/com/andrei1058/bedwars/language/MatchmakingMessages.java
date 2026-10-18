package com.andrei1058.bedwars.language;

import com.andrei1058.bedwars.api.language.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

/** Language-file defaults for server player lists and new-match controls. */
public final class MatchmakingMessages {
    private MatchmakingMessages() { }

    public enum Key {
        LIST_HEADER("cmd-list-header", "&6ONLINE &8» &fThere are {online} players online, with a maximum capacity of {max} players.", "&6在线玩家 &8» &f当前有 {online} 位玩家在线，可容纳最大在线人数为 {max} 位玩家。"),
        LIST_LOBBY("cmd-list-lobby", "&aLOBBY &8» &f{players}", "&a大厅 &8» &f{players}"),
        LIST_ARENA("cmd-list-arena", "&bARENA &8[&f{arena}&8] &8» &f{players}", "&b竞技场 &8[&f{arena}&8] &8» &f{players}"),
        LIST_EMPTY("cmd-list-empty", "&7None", "&7无人"),
        MATCHES_BLOCKED("cmd-matchmaking-blocked", "&cNew matches are currently disabled by an administrator.", "&c管理员已暂停新对局。"),
        MATCHES_ALREADY_BLOCKED("cmd-matchmaking-already-blocked", "&eNew matches are already disabled.", "&e新对局已经处于暂停状态。"),
        MATCHES_RESUMED("cmd-matchmaking-resumed", "&aNew matches are enabled again.", "&a已恢复新对局。"),
        MATCHES_ALREADY_RESUMED("cmd-matchmaking-already-resumed", "&eNew matches are already enabled.", "&e新对局已经处于开启状态。");

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

    public static void install(Language language) {
        boolean chinese = Language.isSimplifiedChineseIso(language.getIso());
        for (Key key : Key.values()) {
            language.getYml().addDefault(key.path, chinese ? key.chinese : key.english);
        }
        migrateLegacyListDefaults(language, chinese);
        language.setComments(Key.LIST_HEADER.path, "服务器 /list 玩家总数行；保留 {online} 和 {max}。");
        language.setComments(Key.LIST_LOBBY.path, "服务器 /list 大厅玩家行；保留 {players}。");
        language.setComments(Key.LIST_ARENA.path, "服务器 /list 竞技场玩家行；保留 {arena} 和 {players}。");
    }

    private static void migrateLegacyListDefaults(Language language, boolean chinese) {
        var yml = language.getYml();
        migrateIfBuiltin(yml, Key.LIST_HEADER.path, chinese ? "&6在线玩家 &7({online}/{max})" : "&6Online players &7({online}/{max})", chinese ? Key.LIST_HEADER.chinese : Key.LIST_HEADER.english);
        migrateIfBuiltin(yml, Key.LIST_LOBBY.path, chinese ? "&e大厅 &7({count})&f：{players}" : "&eLobby &7({count})&f: {players}", chinese ? Key.LIST_LOBBY.chinese : Key.LIST_LOBBY.english);
        migrateIfBuiltin(yml, Key.LIST_ARENA.path, chinese ? "&b竞技场 {arena} &7({count})&f：{players}" : "&bArena {arena} &7({count})&f: {players}", chinese ? Key.LIST_ARENA.chinese : Key.LIST_ARENA.english);
    }

    private static void migrateIfBuiltin(YamlConfiguration yml, String path, String oldValue, String newValue) {
        if (oldValue.equals(yml.getString(path))) yml.set(path, newValue);
    }

    public static Snapshot forSender(CommandSender sender) {
        Language language = sender instanceof Player player
                ? Language.getPlayerLanguage(player) : Language.getDefaultLanguage();
        EnumMap<Key, String> templates = new EnumMap<>(Key.class);
        for (Key key : Key.values()) templates.put(key, language.m(key.path));
        return new Snapshot(templates);
    }

    public static Snapshot defaults(boolean chinese) {
        EnumMap<Key, String> templates = new EnumMap<>(Key.class);
        for (Key key : Key.values()) templates.put(key, chinese ? key.chinese : key.english);
        return new Snapshot(templates);
    }

    public record Snapshot(Map<Key, String> templates) {
        public Snapshot {
            templates = Map.copyOf(templates);
        }

        public String text(Key key, String... replacements) {
            String line = templates.get(key);
            for (int index = 0; index < replacements.length; index += 2) {
                line = line.replace(replacements[index], replacements[index + 1]);
            }
            return line;
        }
    }
}
