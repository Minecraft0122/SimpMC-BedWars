package com.andrei1058.bedwars.configuration;

import com.andrei1058.bedwars.BedWars;
import com.andrei1058.bedwars.api.configuration.ConfigManager;

public class MoneyConfig extends ConfigManager {

    public static MoneyConfig money;

    private MoneyConfig() {
        super ( BedWars.plugin, "rewards", BedWars.plugin.getDataFolder ().toString () );
    }

    /**
     * Initialize money config.
     */
    public static void init() {
        money = new MoneyConfig ();
        money.getYml().options().header("SimpMC-BedWars Vault 经济奖励配置。\n需要同时安装 Vault 桥接层和向 Vault 注册的经济服务提供者。");
        money.getYml ().options ().copyDefaults ( true );
        money.getYml ().addDefault ( "money-rewards.per-minute", 5 );
        money.getYml ().addDefault ( "money-rewards.per-teammate", 30 );
        money.getYml ().addDefault ( "money-rewards.game-win", 90 );
        money.getYml ().addDefault ( "money-rewards.bed-destroyed", 60 );
        money.getYml ().addDefault ( "money-rewards.final-kill", 40 );
        money.getYml ().addDefault ( "money-rewards.regular-kill", 10 );
        money.setComments("money-rewards", "Vault 桥接层与经济服务提供者均可用时，各类游戏行为奖励的金币数量。");
        ChineseConfigDocumentation.rewards(money);
        money.updateToLatestVersion(3);
    }
}
