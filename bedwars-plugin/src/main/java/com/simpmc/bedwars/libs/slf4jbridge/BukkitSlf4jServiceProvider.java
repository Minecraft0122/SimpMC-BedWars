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
 * <p>
 * 当插件 JAR 被直接放到应用类路径（例如 Paper 运行契约探针）时，
 * 服务器的 SLF4J 可能同时发现两个提供器；此时本提供器声明较低的
 * API 版本让服务器自带提供器赢得绑定，插件自己的重定位库仍按
 * 重定位接口找到本桥接。
 */
public final class BukkitSlf4jServiceProvider implements SLF4JServiceProvider {

    /** 插件类加载环境下的正常 API 版本，确保重定位库绑定成功。 */
    private static final String PLUGIN_CLASSPATH_API_VERSION = "2.0.99";
    /**
     * 插件 JAR 被直接放到应用类路径时（例如运行契约探针），声明较低版本，
     * 让服务器自带的日志提供器赢得绑定，避免桥接接管整个服务器。
     */
    private static final String SERVER_CLASSPATH_API_VERSION = "2.0.0";

    private final String requestedApiVersion =
            BukkitSlf4jServiceProvider.class.getClassLoader() == ClassLoader.getSystemClassLoader()
                    ? SERVER_CLASSPATH_API_VERSION
                    : PLUGIN_CLASSPATH_API_VERSION;
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
        // 与 slf4j-simple / slf4j-jdk14 一致表示兼容当前 2.0.x API；
        // 应用类路径环境下自动降低，让位于服务器自带提供器。
        return requestedApiVersion;
    }
}
