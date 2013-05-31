package com.bt.pi.sss.robustness;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.entities.UserAccessKey;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.management.ApplicationSeeder;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.node.KoalaNode;
import com.bt.pi.sss.PisssApplicationManager;
import com.bt.pi.sss.robustness.scenario.PisssScenarioBase;
import com.ragstorooks.testrr.Runner;
import com.ragstorooks.testrr.ScenarioResult;

public class Main {

    public static void main(String[] args) throws Exception {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("p2p");
        String root = resourceBundle.getString("bucketRootDirectory");
        if (new File(root).exists())
            FileUtils.cleanDirectory(new File(root));
        else
            new File(root).mkdirs();

        FileUtils.deleteQuietly(new File(resourceBundle.getString("nodeIdFile")));
        deleteAnyStorageDirs();

        String[] contexts = new String[] { "classpath:applicationContext-robustness.xml" };
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(contexts);

        KoalaNode koalaNode = (KoalaNode) applicationContext.getBean("koalaNode");
        koalaNode.start();

        seedApplicationRecord(applicationContext, koalaNode.getKoalaIdFactory().getRegion());
        seedUsers(applicationContext);
        seedRegions(applicationContext);

        initialiseScenarioBeans(applicationContext);

        Runner runner = (Runner) applicationContext.getBean("scenarioRunner");
        runner.run();

        destroyScenarioBeans(applicationContext);

        Thread.sleep(10000);

        Map<String, ScenarioResult> failures = runner.getScenarioFailures();
        Map<String, ScenarioResult> successes = runner.getScenarioSuccesses();

        long runtime = runner.getTotalRunTimeMilliSeconds();
        double successRate = runner.getSuccessRate();

        applicationContext.destroy();

        cleanUp(resourceBundle);

        Map<String, Integer> scenarios = new HashMap<String, Integer>();

        Iterator<?> iterator = failures.keySet().iterator();
        while (iterator.hasNext()) {
            String scenarioId = (String) iterator.next();
            ScenarioResult result = failures.get(scenarioId);
            String scenarioName = result.getScenarioType().getSimpleName();
            System.out.println(String.format("Scenario %s(%s) failed because: %s", scenarioName, scenarioId, result.getMessage()));
            addScenario(scenarios, scenarioName);
        }

        iterator = successes.keySet().iterator();
        while (iterator.hasNext()) {
            String scenarioName = successes.get(iterator.next()).getScenarioType().getSimpleName();
            addScenario(scenarios, scenarioName);
        }

        iterator = scenarios.keySet().iterator();
        while (iterator.hasNext()) {
            String scenarioName = (String) iterator.next();
            Integer scenarioCount = scenarios.get(scenarioName);
            System.out.println(String.format("Scenario %s was run %d times", scenarioName, scenarioCount.intValue()));
        }

        System.out.println(String.format("Total time taken to run scenarios: %d seconds", (runtime / 1000)));
        System.out.println(String.format("Success rate overall: %f percent", successRate));

        if (failures.size() > 0)
            System.exit(1);
        else
            System.exit(0);
    }

    private static void deleteAnyStorageDirs() {
        File currentDir = new File(".");
        File[] list = currentDir.listFiles();
        for (File f : list) {
            if (!f.isDirectory())
                continue;
            if (f.getName().startsWith("storage")) {
                FileUtils.deleteQuietly(f);
            }
        }
    }

    private static void cleanUp(ResourceBundle resourceBundle) throws Exception {
        FileUtils.deleteQuietly(new File("var"));
        File nodeIdFile = new File(resourceBundle.getString("nodeIdFile"));
        String nodeId = FileUtils.readFileToString(nodeIdFile);

        File file = new File(String.format("storage%s", nodeId));
        FileUtils.deleteQuietly(file);
        FileUtils.deleteQuietly(nodeIdFile);
    }

    private static void seedApplicationRecord(ClassPathXmlApplicationContext applicationContext, int regionCode) {
        ApplicationSeeder appSeeder = (ApplicationSeeder) applicationContext.getBean("applicationSeeder");
        appSeeder.createRegionScopedApplicationRecord(PisssApplicationManager.APPLICATION_NAME, regionCode, Arrays.asList(new String[] { "127.0.0.1" }));
    }

    private static void seedUsers(ClassPathXmlApplicationContext applicationContext) {
        String accessKey = System.getProperty("ACCESS_KEY");
        String secretKey = System.getProperty("SECRET_KEY");
        PiIdBuilder piIdbuildler = (PiIdBuilder) applicationContext.getBean("piIdBuilder");
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContext.getBean("dhtClientFactory");

        User user = new User("fred", accessKey, secretKey);

        BlockingDhtWriter writer1 = dhtClientFactory.createBlockingWriter();
        writer1.put(piIdbuildler.getPId(user), user);
        BlockingDhtWriter writer2 = dhtClientFactory.createBlockingWriter();
        UserAccessKey key = new UserAccessKey(user.getUsername(), user.getApiAccessKey());
        writer2.put(piIdbuildler.getPId(key), key);
    }

    private static void seedRegions(ClassPathXmlApplicationContext applicationContext) {
        DhtClientFactory dhtClientFactory = (DhtClientFactory) applicationContext.getBean("dhtClientFactory");
        PiIdBuilder piIdbuildler = (PiIdBuilder) applicationContext.getBean("piIdBuilder");
        Region defaultRegion = new Region("UK", 1, "", "");
        Regions regions = new Regions();
        regions.addRegion(defaultRegion);
        regions.addRegion(new Region("RuyLopez", 2, "", ""));

        dhtClientFactory.createBlockingWriter().put(piIdbuildler.getRegionsId(), regions);
    }

    private static void initialiseScenarioBeans(ClassPathXmlApplicationContext applicationContext) throws Exception {
        Map<String, PisssScenarioBase> beansOfType = applicationContext.getBeansOfType(PisssScenarioBase.class);
        for (Entry<String, PisssScenarioBase> entry : beansOfType.entrySet()) {
            entry.getValue().setup();
        }
    }

    private static void destroyScenarioBeans(ClassPathXmlApplicationContext applicationContext) throws Exception {
        Map<String, PisssScenarioBase> beansOfType = applicationContext.getBeansOfType(PisssScenarioBase.class);
        for (Entry<String, PisssScenarioBase> entry : beansOfType.entrySet()) {
            entry.getValue().destroy();
        }
    }

    private static void addScenario(Map<String, Integer> scenarios, String scenarioName) {
        if (scenarios.containsKey(scenarioName)) {
            scenarios.put(scenarioName, scenarios.get(scenarioName) + 1);
        } else {
            scenarios.put(scenarioName, 1);
        }
    }
}
