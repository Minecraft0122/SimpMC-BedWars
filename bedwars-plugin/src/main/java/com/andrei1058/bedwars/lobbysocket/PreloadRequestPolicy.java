/*
 * BedWars1058 - A bed wars mini-game.
 * Copyright (C) 2021 Andrei Dascălu
 */

package com.andrei1058.bedwars.lobbysocket;

final class PreloadRequestPolicy {

    private PreloadRequestPolicy() {
    }

    /**
     * 旧版消息没有 request_id。空 token 保留兼容性，非空 token 必须精确匹配，
     * 这样延迟到达的旧取消消息不会删除新预加载。
     */
    static boolean matches(String activeRequestId, String cancellationRequestId) {
        String active = activeRequestId == null ? "" : activeRequestId.trim();
        String cancellation = cancellationRequestId == null ? "" : cancellationRequestId.trim();
        return active.isEmpty() || cancellation.isEmpty() || active.equals(cancellation);
    }
}
