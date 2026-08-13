package com.andrei1058.bedwars.shop.listeners;

final class RecallScrollCountdown {

    private int secondsRemaining;

    RecallScrollCountdown(int seconds) {
        if (seconds < 1) throw new IllegalArgumentException("seconds must be positive");
        this.secondsRemaining = seconds;
    }

    int secondsRemaining() {
        return secondsRemaining;
    }

    boolean advance() {
        if (secondsRemaining < 1) throw new IllegalStateException("countdown is already complete");
        return --secondsRemaining == 0;
    }
}
