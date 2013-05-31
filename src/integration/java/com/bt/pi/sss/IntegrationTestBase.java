package com.bt.pi.sss;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.sss.entities.ObjectMetaData;
import com.bt.pi.sss.util.FileSystemBucketUtils;
import com.ragstorooks.testrr.cli.CommandExecutor;

public abstract class IntegrationTestBase {
    private static ClassPathXmlApplicationContext classPathXmlApplicationContext = null;
    protected static final String SECRET_KEY = "sIL03bvLbAa__Oa7Oe5Ssuhjalg6m-cd0RDwOg";
    protected static final String BAD_SECRET_KEY = "xxxxxxxxxxx__xxxxxxxxxxxxxxxx-xxxxxxxx";
    protected static final String ACCESS_KEY = "Y7HqdJgHC7aYIbnxPRNU1A";
    protected static final String BAD_ACCESS_KEY = "xxxxxxxxxxxxxxxxxxxxxx";
    protected CommandExecutor commandExecutor;
    protected static final String PORT = "9090";
    protected static String s3Url = "http://localhost:" + PORT;

    protected static final String BUCKET_ROOT = "var/buckets";
    protected static final String USER_NAME = "bob";
    protected String bucketName = "test1";
    protected String objectName = "testFile1";
    protected String testData = "this is a test file\n line 2";
    protected static final String bucketLocation = "UK";
    private static KoalaNode koalaNode;
    private static Properties properties;
    protected static PiIdBuilder piIdbuildler;
    private static DhtClientFactory dhtClientFactory;
    protected static BlockingDhtCache dhtCache;
    protected static final String ruyLopezRegionS3Url = "http://RuyLopez:8080";

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        FileUtils.deleteQuietly(new File("var"));
        FileUtils.forceMkdir(new File(BUCKET_ROOT));
        try {
            classPathXmlApplicationContext = new ClassPathXmlApplicationContext(new String[] { "commonApplicationContext.xml" });
            FileSystemBucketUtils bucketUtils = (FileSystemBucketUtils) classPathXmlApplicationContext.getBean("fileSystemBucketUtils");
            bucketUtils.setBucketRootDirectory(BUCKET_ROOT);
            dhtCache = (BlockingDhtCache) classPathXmlApplicationContext.getBean("generalBlockingCache");
            properties = (Properties) classPathXmlApplicationContext.getBean("properties");
            koalaNode = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
            koalaNode.start();

            seedUsers();
            seedRegions();
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static void seedUsers() {
        piIdbuildler = (PiIdBuilder) classPathXmlApplicationContext.getBean("piIdBuilder");
        dhtClientFactory = (DhtClientFactory) classPathXmlApplicationContext.getBean("dhtClientFactory");

        User[] users = new User[] { new User(USER_NAME, ACCESS_KEY, SECRET_KEY), new User("fred", "aaaaaaaaaaaYIbnxPRNU1A", "aaaaaaaaaaa__Oa7Oe5Ssuhjalg6m-cd0RDwOg") };

        for (User user : users) {
            BlockingDhtWriter writer1 = dhtClientFactory.createBlockingWriter();
            writer1.put(piIdbuildler.getPId(user), user);
            BlockingDhtWriter writer2 = dhtClientFactory.createBlockingWriter();
            UserAccessKey key = new UserAccessKey(user.getUsername(), user.getApiAccessKey());
            writer2.put(piIdbuildler.getPId(key), key);
        }
    }

    private static void seedRegions() {
        Region defaultRegion = new Region(bucketLocation, 1, "", "");
        Regions regions = new Regions();
        regions.addRegion(defaultRegion);
        regions.addRegion(new Region("RuyLopez", 2, "", ""));

        dhtClientFactory.createBlockingWriter().put(piIdbuildler.getRegionsId(), regions);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        File nodeIdFile = new File(properties.getProperty("nodeIdFile"));
        String nodeId = FileUtils.readFileToString(nodeIdFile);

        File file = new File(String.format("storage%s", nodeId));
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(nodeIdFile);

        koalaNode.stop();
        classPathXmlApplicationContext.destroy();
        FileUtils.deleteDirectory(new File(BUCKET_ROOT));
    }

    @Before
    public void before() {
        commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));
    }

    @After
    public void after() throws Exception {
        FileUtils.cleanDirectory(new File(BUCKET_ROOT));
        removeBucketsFromUsers();
    }

    protected void runCommand(String command) throws Exception {
        String[] commands = new String[] { "/bin/bash", "-c", command };
        commandExecutor.executeScript(commands, Runtime.getRuntime());
    }

    protected String getXmlFromLines(List<String> lines) {
        StringBuffer buffer = new StringBuffer();
        for (String line : lines) {
            if (line.trim().startsWith("<"))
                buffer.append(line.trim());
        }
        return buffer.toString();
    }

    protected void assertResponse(List<String> lines, String target) {
        for (String line : lines)
            if (line.contains(target))
                return;
        fail();
    }

    protected void assertDate(Date d) throws ParseException {
        long date = d.getTime();
        long now = Calendar.getInstance().getTimeInMillis();
        assertTrue(Math.abs(date - now) < 15000);
    }

    protected void assertDate(String s) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date d = sdf.parse(s);
        assertDate(d);
    }

    protected String writeToTmpFile(String data) throws Exception {
        File tmpFile = File.createTempFile("unittesting", null);
        // tmpFile.deleteOnExit();
        FileUtils.writeStringToFile(tmpFile, data);

        return tmpFile.getAbsolutePath();
    }

    protected void setupDefaultRegions() {
        Regions regions = new Regions();
        regions.addRegion(new Region("RuyLopez", 1, "", ""));

        BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
        writer.writeIfAbsent(piIdbuildler.getRegionsId(), regions);
    }

    protected void setupBucketMetaData(final String userName, final String... bucketNames) {

        for (String bucketName : bucketNames) {
            createBucketMetaDataInRegion(userName, bucketName, "UK");
        }

        PId id = piIdbuildler.getPId(User.getUrl(userName));
        dhtCache.update(id, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                for (String bucketName : bucketNames) {
                    existingEntity.getBucketNames().add(bucketName);
                }
                return existingEntity;
            }
        });
    }

    protected void createBucketMetaDataInRegion(final String userName, final String bucketName, final String bucketRegion) {
        final BucketMetaData fromName = BucketMetaData.fromName(bucketName);
        fromName.setLocation(bucketRegion == null ? bucketLocation : bucketRegion);
        PId id = piIdbuildler.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        // writer.put(id, fromName);
        dhtCache.update(id, new UpdateResolver<BucketMetaData>() {
            @Override
            public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                if (null == existingEntity)
                    return fromName;

                existingEntity.setCannedAccessPolicy(fromName.getCannedAclPolicy());
                existingEntity.setLocation(fromName.getLocation());
                existingEntity.setDeleted(false);
                existingEntity.resetCreationDate();

                return existingEntity;
            }
        });

    }

    private void removeBucketsFromUsers() {

        final Set<String> bucketNamesToDelete = new HashSet<String>();

        PId id = piIdbuildler.getPId(User.getUrl(USER_NAME));
        dhtCache.update(id, new UpdateResolver<User>() {
            @Override
            public User update(User existingEntity, User requestedEntity) {
                System.err.println("About to delete buckets from user: " + USER_NAME + ", buckets: " + existingEntity.getBucketNames());
                bucketNamesToDelete.addAll(existingEntity.getBucketNames());
                existingEntity.getBucketNames().clear();
                return existingEntity;
            }
        });

        for (String bucketName : bucketNamesToDelete) {
            PId bucketId = piIdbuildler.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

            dhtCache.update(bucketId, new UpdateResolver<BucketMetaData>() {

                @Override
                public BucketMetaData update(BucketMetaData existingEntity, BucketMetaData requestedEntity) {
                    existingEntity.setDeleted(true);
                    return existingEntity;
                }
            });
        }
    }

    protected String findLine(List<String> lines, String target) {
        for (String line : lines)
            if (line.contains(target))
                return line;
        return null;
    }

    protected void createFilePair() throws Exception {
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s", BUCKET_ROOT, bucketName, objectName)), testData);
        FileUtils.writeStringToFile(new File(String.format("%s/%s/%s%s", BUCKET_ROOT, bucketName, objectName, ObjectMetaData.FILE_SUFFIX)), "{}");
    }

    protected String createBucketConfigurationStringForRegion(String regionName) {
        return "<CreateBucketConfiguration xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><LocationConstraint>" + regionName + "</LocationConstraint></CreateBucketConfiguration>";
    }
}
