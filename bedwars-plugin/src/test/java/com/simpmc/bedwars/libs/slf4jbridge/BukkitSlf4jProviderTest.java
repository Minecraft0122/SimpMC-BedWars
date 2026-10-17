package com.simpmc.bedwars.libs.slf4jbridge;

import com.andrei1058.bedwars.BedWars;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 重定位 HikariCP 的日志必须经桥接输出，不得写入 System.err。 */
class BukkitSlf4jProviderTest {

    @Test
    void providesLoggerFactoryAndApiVersionWithoutPluginInitialization() {
        assertNull(BedWars.plugin, "单元测试环境不应初始化插件主类");
        BukkitSlf4jServiceProvider provider = new BukkitSlf4jServiceProvider();
        provider.initialize();
        assertEquals("2.0.99", provider.getRequestedApiVersion());
        assertNotNull(provider.getLoggerFactory());
        assertNotNull(provider.getMarkerFactory());
        assertNotNull(provider.getMDCAdapter());
    }

    @Test
    void cachesLoggersByNameAndKeepsWorkingWithoutPluginLogger() {
        BukkitSlf4jServiceProvider provider = new BukkitSlf4jServiceProvider();
        Logger logger = provider.getLoggerFactory().getLogger("SimpMC-BedWars-MySQLPool");
        assertSame(logger, provider.getLoggerFactory().getLogger("SimpMC-BedWars-MySQLPool"));
        assertNotSame(logger, provider.getLoggerFactory().getLogger("other-lib"));
        assertTrue(logger.isInfoEnabled());
        // 冒烟输出：INFO 级别应经 java.util.logging 正常打印，而不是 System.err。
        logger.info("SimpMC-BedWars-MySQLPool - Starting...");
    }
}
