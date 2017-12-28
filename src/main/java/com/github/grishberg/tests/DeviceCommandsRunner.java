package com.github.grishberg.tests;

import com.github.grishberg.tests.commands.DeviceCommand;
import com.github.grishberg.tests.commands.DeviceCommandProvider;
import com.github.grishberg.tests.commands.DeviceCommandResult;
import com.github.grishberg.tests.planner.InstrumentalTestPlanProvider;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * Created by grishberg on 26.10.17.
 */
public class DeviceCommandsRunner {
    private final InstrumentalTestPlanProvider testPlanProvider;
    private final DeviceCommandProvider commandProvider;
    private File coverageFilesDir;
    private File reportsDir;
    private final Logger logger;
    private boolean hasFailedTests;

    public DeviceCommandsRunner(InstrumentalTestPlanProvider testPlanProvider,
                                DeviceCommandProvider commandProvider,
                                File coverageFilesDir, File reportsDir, Logger logger) {
        this.testPlanProvider = testPlanProvider;
        this.commandProvider = commandProvider;
        this.coverageFilesDir = coverageFilesDir;
        this.reportsDir = reportsDir;
        this.logger = logger;
    }

    public boolean runCommands(DeviceWrapper[] devices) throws InterruptedException {
        final CountDownLatch deviceCounter = new CountDownLatch(devices.length);

        for (DeviceWrapper device : devices) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DeviceCommand[] commands = commandProvider.provideDeviceCommands(device,
                                testPlanProvider, coverageFilesDir, reportsDir);
                        for (DeviceCommand command : commands) {
                            logger.debug("[AITR] Before executing device = {} command ={}",
                                    device.toString(), command.toString());
                            DeviceCommandResult result = command.execute(device);
                            logger.debug("[AITR] After executing device = {} command ={}",
                                    device.toString(), command.toString());
                            if (result.isFailed()) {
                                hasFailedTests = true;
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Some Exception", e);
                    } finally {
                        deviceCounter.countDown();
                    }
                }
            }).start();
        }
        deviceCounter.await();
        return !hasFailedTests;
    }
}
