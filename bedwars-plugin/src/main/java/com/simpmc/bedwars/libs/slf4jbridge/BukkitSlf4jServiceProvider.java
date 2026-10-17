package com.simpmc.bedwars.libs.slf4jbridge;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.NOPMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

/**
 * 插件内置的 SLF4J 提供器，仅供重定位后的第三方库（HikariCP 等）使用。
 * <p>
 * 重定位后的连接池如果找不到可用的日志实现，会把启动和关机日志写入
 * System.err，在 Paper 上表现为 {@code [STDERR]} 错误行并触发插件作者
 * 提醒；这个提供器把这些日志桥接到插件自身的 logger，让控制台按正常
 * 等级显示。由于 {@code org.slf4j} 在打包时被重定位，它只会被本插件
 * 内的重定位库发现，不影响服务器上的其他插件。
 */
public final class BukkitSlf4jServiceProvider implements SLF4JServiceProvider {

    private final ILoggerFactory loggerFactory = new BukkitLoggerFactory();
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdcAdapter = new NOPMDCAdapter();

    @Override
    public void initialize() {
        // 桥接不注册额外资源，初始化无需做任何事。
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        // 与 slf4j-simple / slf4j-jdk14 一致，表示兼容当前 2.0.x API。
        return "2.0.99";
    }
}
