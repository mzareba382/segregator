package com.mzareba.segregator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

@Slf4j
@Component
public class Segregator {

    @Value("${segregator.dir}")
    String segregatorDir;

    @Value("${home.dir}")
    String homeDir;

    @Value("${dev.dir}")
    String devDir;

    @Value("${test.dir}")
    String testDir;

    @PostConstruct
    @Scheduled(fixedDelay = Long.MAX_VALUE)
    public void test() {
        try {
            ensureSegregatorDirectoriesExist();
            log.info("Segregator directories existence has been ensured");

            WatchService watcher = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(segregatorDir + "/HOME");
            dir.register(watcher, ENTRY_CREATE);

            log.info("WatchService registered for dir: " + dir.getFileName());
            log.info("DEV directory = " + devDir);
            log.info("TEST directory = " + testDir);

            moveAllFilesFromSourceAccordingToBusinessLogic(dir);

            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    return;
                }
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;

                    Path fileFullPath = dir.resolve(ev.context());

                    if (kind == ENTRY_CREATE) {
                        log.info(String.format("%s: New file detected in directory: %s", kind.name(), fileFullPath));
                        moveSingleFileAccordingToBusinessLogic(fileFullPath);
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private void ensureSegregatorDirectoriesExist() throws IOException {
        Path homeDir = Path.of(this.homeDir);
        Path devDir = Path.of(this.devDir);
        Path testDir = Path.of(this.testDir);
        List<Path> directoriesList = Arrays.asList(homeDir, devDir, testDir);

        for (Path path : directoriesList) {
            if (!Files.isDirectory(path)) {
                Files.createDirectory(path);
            }
        }
    }

    private void moveAllFilesFromSourceAccordingToBusinessLogic(Path sourceDir) {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(sourceDir)) {
            for (Path path : directoryStream) {
                moveSingleFileAccordingToBusinessLogic(path);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // there could be problems with some temp files of some extensions (f.e. microsoft excel files),
    // needs additional testing
    private void moveSingleFileAccordingToBusinessLogic(Path fileFullPath) {
        try {
            Path fileName = fileFullPath.getFileName();
            String fileExtension = FilenameUtils.getExtension(fileName.toString()); // platform independent

            BasicFileAttributes attr = Files.readAttributes(fileFullPath, BasicFileAttributes.class);
            ZonedDateTime creationDate = attr.creationTime().toInstant().atZone(ZoneId.systemDefault()); // by default, .creationTime returns UTC time
            int creationHour = creationDate.getHour();

            if (fileExtension.equals("jar") && (creationHour % 2 == 0)) {  // if creationHour is even
                Path destination = Paths.get(devDir, fileName.toString());
                Files.move(fileFullPath, destination, StandardCopyOption.ATOMIC_MOVE);
                log.info(String.format("File has been moved to %s", destination));
            } else if (fileExtension.equals("jar")) { // if creationHour is odd
                Path destination = Paths.get(testDir, fileName.toString());
                Files.move(fileFullPath, destination, StandardCopyOption.ATOMIC_MOVE);
                log.info(String.format("File has been moved to %s", destination));
            } else if (fileExtension.equals("xml")) {
                Path destination = Paths.get(devDir, fileName.toString());
                Files.move(fileFullPath, destination, StandardCopyOption.ATOMIC_MOVE);
                log.info(String.format("File has been moved to %s", destination));
            }
        } catch (IOException e) {

        }
    }
}
