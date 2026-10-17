package com.simpmc.bedwars.libs.slf4jbridge;

import com.andrei1058.bedwars.BedWars;
import org.slf4j.Marker;
import org.slf4j.helpers.MessageFormatter;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 把重定位后的 SLF4J 调用转发到插件的 java.util.logging logger。
 * 日志等级映射：TRACE→FINEST、DEBUG→FINE、INFO→INFO、WARN→WARNING、ERROR→SEVERE。
 */
final class JulBridgeLogger implements org.slf4j.Logger {

    private static final Level TRACE = Level.FINEST;
    private static final Level DEBUG = Level.FINE;
    private static final Level INFO = Level.INFO;
    private static final Level WARN = Level.WARNING;
    private static final Level ERROR = Level.SEVERE;

    private final Logger target;
    private final String name;

    JulBridgeLogger(String name) {
        BedWars plugin = BedWars.plugin;
        // 插件主类尚未初始化时（例如单元测试环境），退回到按名称解析的 JUL logger。
        this.target = plugin != null ? plugin.getLogger() : Logger.getLogger(name);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return target.isLoggable(TRACE);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return isTraceEnabled();
    }

    @Override
    public void trace(String message) {
        logMessage(TRACE, message, null);
    }

    @Override
    public void trace(String format, Object argument) {
        logArgs(TRACE, format, new Object[]{argument});
    }

    @Override
    public void trace(String format, Object first, Object second) {
        logArgs(TRACE, format, new Object[]{first, second});
    }

    @Override
    public void trace(String format, Object... arguments) {
        logArgs(TRACE, format, arguments);
    }

    @Override
    public void trace(String message, Throwable throwable) {
        logMessage(TRACE, message, throwable);
    }

    @Override
    public void trace(Marker marker, String message) {
        logMessage(TRACE, message, null);
    }

    @Override
    public void trace(Marker marker, String format, Object argument) {
        logArgs(TRACE, format, new Object[]{argument});
    }

    @Override
    public void trace(Marker marker, String format, Object first, Object second) {
        logArgs(TRACE, format, new Object[]{first, second});
    }

    @Override
    public void trace(Marker marker, String format, Object... arguments) {
        logArgs(TRACE, format, arguments);
    }

    @Override
    public void trace(Marker marker, String message, Throwable throwable) {
        logMessage(TRACE, message, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return target.isLoggable(DEBUG);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return isDebugEnabled();
    }

    @Override
    public void debug(String message) {
        logMessage(DEBUG, message, null);
    }

    @Override
    public void debug(String format, Object argument) {
        logArgs(DEBUG, format, new Object[]{argument});
    }

    @Override
    public void debug(String format, Object first, Object second) {
        logArgs(DEBUG, format, new Object[]{first, second});
    }

    @Override
    public void debug(String format, Object... arguments) {
        logArgs(DEBUG, format, arguments);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        logMessage(DEBUG, message, throwable);
    }

    @Override
    public void debug(Marker marker, String message) {
        logMessage(DEBUG, message, null);
    }

    @Override
    public void debug(Marker marker, String format, Object argument) {
        logArgs(DEBUG, format, new Object[]{argument});
    }

    @Override
    public void debug(Marker marker, String format, Object first, Object second) {
        logArgs(DEBUG, format, new Object[]{first, second});
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        logArgs(DEBUG, format, arguments);
    }

    @Override
    public void debug(Marker marker, String message, Throwable throwable) {
        logMessage(DEBUG, message, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return target.isLoggable(INFO);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return isInfoEnabled();
    }

    @Override
    public void info(String message) {
        logMessage(INFO, message, null);
    }

    @Override
    public void info(String format, Object argument) {
        logArgs(INFO, format, new Object[]{argument});
    }

    @Override
    public void info(String format, Object first, Object second) {
        logArgs(INFO, format, new Object[]{first, second});
    }

    @Override
    public void info(String format, Object... arguments) {
        logArgs(INFO, format, arguments);
    }

    @Override
    public void info(String message, Throwable throwable) {
        logMessage(INFO, message, throwable);
    }

    @Override
    public void info(Marker marker, String message) {
        logMessage(INFO, message, null);
    }

    @Override
    public void info(Marker marker, String format, Object argument) {
        logArgs(INFO, format, new Object[]{argument});
    }

    @Override
    public void info(Marker marker, String format, Object first, Object second) {
        logArgs(INFO, format, new Object[]{first, second});
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        logArgs(INFO, format, arguments);
    }

    @Override
    public void info(Marker marker, String message, Throwable throwable) {
        logMessage(INFO, message, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return target.isLoggable(WARN);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return isWarnEnabled();
    }

    @Override
    public void warn(String message) {
        logMessage(WARN, message, null);
    }

    @Override
    public void warn(String format, Object argument) {
        logArgs(WARN, format, new Object[]{argument});
    }

    @Override
    public void warn(String format, Object first, Object second) {
        logArgs(WARN, format, new Object[]{first, second});
    }

    @Override
    public void warn(String format, Object... arguments) {
        logArgs(WARN, format, arguments);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        logMessage(WARN, message, throwable);
    }

    @Override
    public void warn(Marker marker, String message) {
        logMessage(WARN, message, null);
    }

    @Override
    public void warn(Marker marker, String format, Object argument) {
        logArgs(WARN, format, new Object[]{argument});
    }

    @Override
    public void warn(Marker marker, String format, Object first, Object second) {
        logArgs(WARN, format, new Object[]{first, second});
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        logArgs(WARN, format, arguments);
    }

    @Override
    public void warn(Marker marker, String message, Throwable throwable) {
        logMessage(WARN, message, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return target.isLoggable(ERROR);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return isErrorEnabled();
    }

    @Override
    public void error(String message) {
        logMessage(ERROR, message, null);
    }

    @Override
    public void error(String format, Object argument) {
        logArgs(ERROR, format, new Object[]{argument});
    }

    @Override
    public void error(String format, Object first, Object second) {
        logArgs(ERROR, format, new Object[]{first, second});
    }

    @Override
    public void error(String format, Object... arguments) {
        logArgs(ERROR, format, arguments);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logMessage(ERROR, message, throwable);
    }

    @Override
    public void error(Marker marker, String message) {
        logMessage(ERROR, message, null);
    }

    @Override
    public void error(Marker marker, String format, Object argument) {
        logArgs(ERROR, format, new Object[]{argument});
    }

    @Override
    public void error(Marker marker, String format, Object first, Object second) {
        logArgs(ERROR, format, new Object[]{first, second});
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        logArgs(ERROR, format, arguments);
    }

    @Override
    public void error(Marker marker, String message, Throwable throwable) {
        logMessage(ERROR, message, throwable);
    }

    private void logArgs(Level level, String format, Object[] arguments) {
        if (format == null) format = "null";
        var tuple = MessageFormatter.arrayFormat(format, arguments);
        Throwable throwable = tuple.getThrowable();
        if (throwable != null) target.log(level, tuple.getMessage(), throwable);
        else target.log(level, tuple.getMessage());
    }

    private void logMessage(Level level, String message, Throwable throwable) {
        if (message == null) message = "null";
        if (throwable != null) target.log(level, message, throwable);
        else target.log(level, message);
    }
}
