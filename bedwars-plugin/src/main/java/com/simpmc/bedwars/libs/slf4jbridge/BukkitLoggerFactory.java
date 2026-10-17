package com.simpmc.bedwars.libs.slf4jbridge;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 按名称缓存重定位库的桥接 logger。 */
final class BukkitLoggerFactory implements ILoggerFactory {

    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(String name) {
        return loggers.computeIfAbsent(name, JulBridgeLogger::new);
    }
}
