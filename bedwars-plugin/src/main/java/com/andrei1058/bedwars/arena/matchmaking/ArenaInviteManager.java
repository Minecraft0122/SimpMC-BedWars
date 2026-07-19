package com.andrei1058.bedwars.arena.matchmaking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stores short-lived invitations from an arena player to a lobby player. */
public final class ArenaInviteManager {

    public static final long INVITE_LIFETIME_MILLIS = 30_000L;
    private static final ArenaInviteManager INSTANCE = new ArenaInviteManager();

    private final Map<UUID, Map<UUID, Invitation>> invitationsByTarget = new HashMap<>();

    private ArenaInviteManager() {
    }

    public static ArenaInviteManager getInstance() {
        return INSTANCE;
    }

    public Invitation create(UUID inviter, UUID target, String arenaName, long nowMillis) {
        Invitation invitation = new Invitation(
                Objects.requireNonNull(inviter, "inviter"),
                Objects.requireNonNull(target, "target"),
                Objects.requireNonNull(arenaName, "arenaName"),
                nowMillis + INVITE_LIFETIME_MILLIS
        );
        invitationsByTarget.computeIfAbsent(target, ignored -> new HashMap<>()).put(inviter, invitation);
        return invitation;
    }

    public Optional<Invitation> find(UUID target, UUID inviter, long nowMillis) {
        Map<UUID, Invitation> targetInvitations = invitationsByTarget.get(target);
        if (targetInvitations == null) return Optional.empty();
        Invitation invitation = targetInvitations.get(inviter);
        if (invitation == null) return Optional.empty();
        if (invitation.expiresAtMillis() <= nowMillis) {
            remove(target, inviter);
            return Optional.empty();
        }
        return Optional.of(invitation);
    }

    public List<Invitation> findAll(UUID target, long nowMillis) {
        Map<UUID, Invitation> targetInvitations = invitationsByTarget.get(target);
        if (targetInvitations == null) return List.of();
        List<Invitation> active = new ArrayList<>();
        for (Invitation invitation : new ArrayList<>(targetInvitations.values())) {
            if (invitation.expiresAtMillis() <= nowMillis) {
                remove(target, invitation.inviter());
            } else {
                active.add(invitation);
            }
        }
        return List.copyOf(active);
    }

    public void remove(UUID target, UUID inviter) {
        Map<UUID, Invitation> targetInvitations = invitationsByTarget.get(target);
        if (targetInvitations == null) return;
        targetInvitations.remove(inviter);
        if (targetInvitations.isEmpty()) invitationsByTarget.remove(target);
    }

    public void clearPlayer(UUID player) {
        invitationsByTarget.remove(player);
        for (UUID target : new ArrayList<>(invitationsByTarget.keySet())) {
            remove(target, player);
        }
    }

    public record Invitation(UUID inviter, UUID target, String arenaName, long expiresAtMillis) {
    }
}
