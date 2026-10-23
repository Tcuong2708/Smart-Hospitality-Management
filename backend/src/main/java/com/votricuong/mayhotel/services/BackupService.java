package com.votricuong.mayhotel.services;

import com.votricuong.mayhotel.documents.BackupHistory;
import com.votricuong.mayhotel.documents.BackupSettings;
import com.votricuong.mayhotel.repositories.BackupHistoryRepository;
import com.votricuong.mayhotel.repositories.BackupSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BackupService {
    private final BackupHistoryRepository historyRepository;
    private final BackupSettingsRepository settingsRepository;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final Path backupRoot;
    private final String mongoUri;
    private final String database;
    private final boolean backupEnabled;
    private final boolean restoreEnabled;

    public BackupService(BackupHistoryRepository historyRepository,
                         BackupSettingsRepository settingsRepository,
                         SequenceGeneratorService sequenceGeneratorService,
                         @Value("${app.backup.directory:./backups}") String backupDirectory,
                         @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/MayHotelDB}") String mongoUri,
                         @Value("${app.backup.database:MayHotelDB}") String database,
                         @Value("${app.features.backup.enabled:false}") boolean backupEnabled,
                         @Value("${app.features.backup.restore-enabled:false}") boolean restoreEnabled) {
        this.historyRepository = historyRepository;
        this.settingsRepository = settingsRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.backupRoot = Paths.get(backupDirectory).toAbsolutePath().normalize();
        this.mongoUri = mongoUri;
        this.database = database;
        this.backupEnabled = backupEnabled;
        this.restoreEnabled = restoreEnabled;
    }

    public List<BackupHistory> history() {
        return historyRepository.findAll().stream()
                .sorted(Comparator.comparing(BackupHistory::getBackupDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public BackupSettings settings() {
        return settingsRepository.findById("singleton").orElseGet(BackupSettings::new);
    }

    public synchronized BackupHistory create(Long staffId) {
        requireBackupEnabled();
        BackupHistory history = new BackupHistory();
        history.setId(sequenceGeneratorService.generateSequence("backup_histories_sequence"));
        history.setStaffId(staffId);
        history.setBackupDate(new Date());
        history.setOperation("BACKUP");
        history.setStatus("RUNNING");
        history.setFileName("backup-" + System.currentTimeMillis());
        historyRepository.save(history);
        try {
            Files.createDirectories(backupRoot);
            Path target = backupRoot.resolve(history.getFileName()).normalize();
            if (!target.startsWith(backupRoot)) throw new IOException("Đường dẫn backup không hợp lệ.");
            runMongoTool("mongodump", target);
            long bytes;
            try (Stream<Path> files = Files.walk(target)) {
                bytes = files.filter(Files::isRegularFile).mapToLong(this::size).sum();
            }
            history.setSizeMb(bytes / 1024d / 1024d);
            history.setStatus("SUCCESS");
            history.setCompletedAt(new Date());
        } catch (Exception e) {
            history.setStatus("FAILED");
            history.setMessage(safeMessage(e));
            history.setCompletedAt(new Date());
        }
        return historyRepository.save(history);
    }

    public synchronized BackupHistory restore(String name, Long staffId) {
        if (!backupEnabled || !restoreEnabled) {
            throw new IllegalStateException("Chức năng phục hồi dữ liệu đang bị khóa bởi cấu hình an toàn.");
        }
        Path source = safePath(name);
        if (source == null || !Files.isDirectory(source)) throw new IllegalArgumentException("Bản backup không hợp lệ.");
        create(staffId);
        BackupHistory history = new BackupHistory();
        history.setId(sequenceGeneratorService.generateSequence("backup_histories_sequence"));
        history.setStaffId(staffId);
        history.setBackupDate(new Date());
        history.setOperation("RESTORE");
        history.setFileName(source.getFileName().toString());
        history.setStatus("RUNNING");
        historyRepository.save(history);
        try {
            runMongoTool("mongorestore", source);
            history.setStatus("SUCCESS");
        } catch (Exception e) {
            history.setStatus("FAILED");
            history.setMessage(safeMessage(e));
        }
        history.setCompletedAt(new Date());
        return historyRepository.save(history);
    }

    public BackupSettings saveSettings(boolean enabled, String frequency, Integer hours) {
        requireBackupEnabled();
        if (hours == null || hours < 1 || hours > 720) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        BackupSettings settings = settings();
        settings.setEnabled(enabled);
        settings.setFrequency(frequency == null ? "DAILY" : frequency);
        settings.setIntervalHours(hours);
        settings.setNextRunAt(enabled ? new Date(System.currentTimeMillis() + hours * 3600000L) : null);
        return settingsRepository.save(settings);
    }

    @Scheduled(fixedDelay = 300000)
    public void scheduledBackup() {
        if (!backupEnabled) return;
        BackupSettings settings = settings();
        if (Boolean.TRUE.equals(settings.getEnabled()) && settings.getNextRunAt() != null
                && !settings.getNextRunAt().after(new Date())) {
            create(null);
            settings.setNextRunAt(new Date(System.currentTimeMillis()
                    + (settings.getIntervalHours() == null ? 24 : settings.getIntervalHours()) * 3600000L));
            settingsRepository.save(settings);
        }
    }

    private void runMongoTool(String tool, Path path) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(tool);
        command.add("--uri");
        command.add(mongoUri);
        command.add("--db");
        command.add(database);
        command.add(tool.equals("mongodump") ? "--out" : "--dir");
        command.add(path.toString());
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(30, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Thao tác MongoDB vượt quá thời gian cho phép.");
        }
        if (process.exitValue() != 0) throw new IOException("Công cụ MongoDB trả về lỗi " + process.exitValue() + ".");
    }

    private Path safePath(String name) {
        if (name == null || name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) return null;
        Path path = backupRoot.resolve(name).normalize();
        return path.startsWith(backupRoot) ? path : null;
    }

    private long size(Path path) {
        try { return Files.size(path); } catch (IOException ignored) { return 0; }
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null ? error.getClass().getSimpleName() : message.replaceAll("(?i)mongodb://[^\\s]+", "mongodb://***");
    }

    public boolean isBackupEnabled() {
        return backupEnabled;
    }

    public boolean isRestoreEnabled() {
        return backupEnabled && restoreEnabled;
    }

    private void requireBackupEnabled() {
        if (!backupEnabled) {
            throw new IllegalStateException("Chức năng sao lưu đang bị khóa bởi cấu hình an toàn.");
        }
    }
}
