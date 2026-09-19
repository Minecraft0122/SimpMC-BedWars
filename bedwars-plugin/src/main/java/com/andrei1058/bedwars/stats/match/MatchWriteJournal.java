package com.andrei1058.bedwars.stats.match;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * 保存暂时无法提交的关键写入。只在数据库线程或关闭时访问文件。
 * 正常入队不触盘，因此强制杀进程仍可能丢失尚未转存的内存队列。
 */
final class MatchWriteJournal {
    private static final int MAGIC = 0x42575031;
    private static final int MAX_ITEMS = 100_000;
    private final Path directory;
    private final String identity;
    private final Map<UUID, Path> files = new LinkedHashMap<>();
    private boolean initialized;
    private long nextSequence;

    MatchWriteJournal(Path directory, String identity) {
        this.directory = directory;
        this.identity = Objects.requireNonNull(identity, "identity");
    }

    synchronized List<PendingMatchWrite> readAll() throws IOException {
        initialize();
        List<PendingMatchWrite> writes = new ArrayList<>();
        for (Path file : files.values()) writes.add(read(file));
        return writes;
    }

    synchronized void save(PendingMatchWrite write) throws IOException {
        initialize();
        if (files.containsKey(write.operationId())) return;
        if (directory == null) throw new IOException("未配置对局待写记录目录");
        Files.createDirectories(directory);
        Path target = directory.resolve(String.format(Locale.ROOT, "%020d-%s.pending", ++nextSequence, write.operationId()));
        Path temporary = directory.resolve(target.getFileName() + ".tmp");
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             DataOutputStream output = new DataOutputStream(java.nio.channels.Channels.newOutputStream(channel))) {
            output.writeInt(MAGIC);
            output.writeUTF(identity);
            writeUuid(output, write.operationId());
            output.writeUTF(write.kind().name());
            switch (write.kind()) {
                case START, FINISH -> writeMatch(output, write.match());
                case EVENT -> writeEvent(output, write.event());
                case RESET -> { writeUuid(output, write.resetPlayer()); writeInstant(output, write.resetAt()); }
            }
            output.writeInt(write.punishedPlayers().size());
            for (UUID player : write.punishedPlayers()) writeUuid(output, player);
            output.flush();
            channel.force(true);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            // 同目录重命名仍避免覆盖已有待写文件；不删除原有记录。
            Files.move(temporary, target);
        }
        files.put(write.operationId(), target);
    }

    synchronized void remove(UUID operationId) throws IOException {
        initialize();
        Path file = files.get(operationId);
        if (file != null) {
            Files.deleteIfExists(file);
            files.remove(operationId);
        }
    }

    private void initialize() throws IOException {
        if (initialized) return;
        if (directory != null && Files.exists(directory)) {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".pending")).sorted().toList()) {
                    String name = path.getFileName().toString();
                    try {
                        nextSequence = Math.max(nextSequence, Long.parseLong(name.substring(0, 20)));
                        UUID id = UUID.fromString(name.substring(21, name.length() - ".pending".length()));
                        if (files.putIfAbsent(id, path) != null) throw new IOException("重复的对局待写记录：" + path);
                    } catch (IllegalArgumentException | IndexOutOfBoundsException exception) {
                        throw new IOException("无效的对局待写文件名：" + path, exception);
                    }
                }
            }
        }
        initialized = true;
    }

    private PendingMatchWrite read(Path path) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC) throw new IOException("无法识别对局待写格式：" + path);
            if (!identity.equals(input.readUTF())) throw new IOException("对局待写记录与当前数据库目标不一致：" + path);
            UUID id = readUuid(input);
            PendingMatchWrite.Kind kind = PendingMatchWrite.Kind.valueOf(input.readUTF());
            MatchRecordSnapshot match = null;
            MatchEventSnapshot event = null;
            UUID player = null;
            Instant at = null;
            switch (kind) {
                case START, FINISH -> match = readMatch(input);
                case EVENT -> event = readEvent(input);
                case RESET -> { player = readUuid(input); at = readInstant(input); }
            }
            int count = readCount(input);
            List<UUID> punished = new ArrayList<>(count);
            for (int index = 0; index < count; index++) punished.add(readUuid(input));
            if (input.read() != -1) throw new IOException("对局待写记录包含额外数据：" + path);
            if (!files.getOrDefault(id, path.resolveSibling("missing")).equals(path)) {
                throw new IOException("对局待写记录编号与文件名不一致：" + path);
            }
            return new PendingMatchWrite(id, kind, match, event, player, at, punished);
        } catch (RuntimeException exception) {
            throw new IOException("无法读取对局待写记录：" + path, exception);
        }
    }

    private static void writeMatch(DataOutputStream out, MatchRecordSnapshot match) throws IOException {
        writeUuid(out, match.matchUuid());
        for (String value : List.of(match.serverId(), match.templateName(), match.runtimeArenaName(), match.arenaGroup(), match.timezone(), match.status())) out.writeUTF(value);
        writeText(out, match.winnerTeam()); writeText(out, match.endReason());
        writeInstant(out, match.startedAt()); writeInstant(out, match.endedAt()); writeInstant(out, match.capturedAt());
        out.writeInt(match.reportNumber()); out.writeLong(match.lastEventSequence());
        out.writeInt(match.playerStats().players().size());
        for (MatchPlayerSnapshot player : match.playerStats().players()) {
            writeUuid(out, player.playerUuid()); writeText(out, player.playerName()); writeText(out, player.teamId());
            out.writeInt(player.kills()); out.writeInt(player.finalKills()); out.writeInt(player.deaths()); out.writeInt(player.bedsDestroyed());
            out.writeInt(player.illegalTeamVl()); out.writeInt(player.killBoostingVl()); out.writeInt(player.evidenceAdjustment());
            out.writeInt(player.reconnects()); out.writeInt(player.disconnects()); out.writeUTF(player.outcome().name());
        }
    }

    private static MatchRecordSnapshot readMatch(DataInputStream in) throws IOException {
        UUID id = readUuid(in);
        String server = in.readUTF(), template = in.readUTF(), runtime = in.readUTF(), group = in.readUTF(), zone = in.readUTF(), status = in.readUTF();
        String winner = readText(in), reason = readText(in);
        Instant start = readInstant(in), end = readInstant(in), captured = readInstant(in);
        int report = in.readInt(); long sequence = in.readLong();
        int count = readCount(in);
        List<MatchPlayerSnapshot> players = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            players.add(new MatchPlayerSnapshot(readUuid(in), readText(in), readText(in), in.readInt(), in.readInt(), in.readInt(), in.readInt(),
                    in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), MatchPlayerOutcome.valueOf(in.readUTF())));
        }
        return new MatchRecordSnapshot(id, server, template, runtime, group, zone, status, winner, reason, start, end, captured, report, sequence, new MatchStatsSnapshot(players));
    }

    private static void writeEvent(DataOutputStream out, MatchEventSnapshot event) throws IOException {
        writeUuid(out, event.eventId()); writeUuid(out, event.matchUuid()); out.writeLong(event.sequence()); out.writeUTF(event.eventType());
        writeUuid(out, event.actorUuid()); writeUuid(out, event.targetUuid()); writeText(out, event.details()); writeInstant(out, event.occurredAt());
    }

    private static MatchEventSnapshot readEvent(DataInputStream in) throws IOException {
        return new MatchEventSnapshot(readUuid(in), readUuid(in), in.readLong(), in.readUTF(), readUuid(in), readUuid(in), readText(in), readInstant(in));
    }

    private static int readCount(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_ITEMS) throw new IOException("对局待写记录数量无效");
        return count;
    }

    private static void writeText(DataOutputStream out, String value) throws IOException { out.writeBoolean(value != null); if (value != null) out.writeUTF(value); }
    private static String readText(DataInputStream in) throws IOException { return in.readBoolean() ? in.readUTF() : null; }
    private static void writeUuid(DataOutputStream out, UUID id) throws IOException { out.writeBoolean(id != null); if (id != null) { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); } }
    private static UUID readUuid(DataInputStream in) throws IOException { return in.readBoolean() ? new UUID(in.readLong(), in.readLong()) : null; }
    private static void writeInstant(DataOutputStream out, Instant instant) throws IOException { out.writeBoolean(instant != null); if (instant != null) { out.writeLong(instant.getEpochSecond()); out.writeInt(instant.getNano()); } }
    private static Instant readInstant(DataInputStream in) throws IOException { return in.readBoolean() ? Instant.ofEpochSecond(in.readLong(), in.readInt()) : null; }
}
