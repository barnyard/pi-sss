package com.bt.pi.sss.util;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.core.conf.Property;
import com.bt.pi.core.util.common.CommandRunner;

@Component
public class ArchiveBucketHelper {
    private static final String S_SLASH_S = "%s/%s";
    private static final Log LOG = LogFactory.getLog(ArchiveBucketHelper.class);
    private static final String DEFAULT_BUCKET_ARCHIVE_DIRECTORY = "var/buckets_archive";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyyMMdd-hhmmss";
    private static final String DEFAULT_COPY_COMMAND = "cp -r %s %s";

    @Resource
    private CommandRunner commandRunner;

    private String archiveDir;
    private String timestampFormat;
    private DateFormat timestampFormatter;
    private String copyCommand;

    public ArchiveBucketHelper() {
        commandRunner = null;
        this.archiveDir = DEFAULT_BUCKET_ARCHIVE_DIRECTORY;
        this.timestampFormat = DEFAULT_TIMESTAMP_FORMAT;
        this.timestampFormatter = new SimpleDateFormat(timestampFormat);
        this.copyCommand = DEFAULT_COPY_COMMAND;
    }

    public String archiveBucket(String bucketName, String bucketPath) {
        LOG.debug(String.format("archiveBucket(%s)", bucketName));

        File archiveDirFile = new File(archiveDir);
        createArchiveDirIfItDoesNotExist(archiveDirFile);
        String archiveBucketName = String.format("%s-%s", bucketName, timestampFormatter.format(new Date()));
        String archivePath = String.format(S_SLASH_S, archiveDir, archiveBucketName);

        String copyDirCmd = String.format(copyCommand, bucketPath, archivePath);
        commandRunner.runNicely(copyDirCmd);
        return archiveBucketName;
    }

    private synchronized void createArchiveDirIfItDoesNotExist(File archiveDirFile) {
        if (!archiveDirFile.exists()) {
            boolean archiveDirCreated = archiveDirFile.mkdir();
            if (!archiveDirCreated)
                throw new RuntimeException("There was an error creating archive directory " + archiveDirFile.getAbsolutePath());
        }
    }

    @Property(key = "fileSystemBucketUtils.archiveDir", defaultValue = DEFAULT_BUCKET_ARCHIVE_DIRECTORY)
    public void setArchiveDir(String theArchiveDir) {
        LOG.debug(String.format("setArchiveDir(%s)", theArchiveDir));
        this.archiveDir = theArchiveDir;
    }

    @Property(key = "fileSystemBucketUtils.copyCommand", defaultValue = DEFAULT_COPY_COMMAND)
    public void setCopyCommand(String theCopyCommand) {
        LOG.debug(String.format("setCopyCommand(%s)", theCopyCommand));
        this.copyCommand = theCopyCommand;
    }

    @Property(key = "fileSystemBucketUtils.timestampFormat", defaultValue = DEFAULT_TIMESTAMP_FORMAT)
    public void setTimestampFormat(String theTimestampFormat) {
        LOG.debug(String.format("setTimestampFormat(%s)", theTimestampFormat));
        this.timestampFormat = theTimestampFormat;
        this.timestampFormatter = new SimpleDateFormat(timestampFormat);
    }

}
