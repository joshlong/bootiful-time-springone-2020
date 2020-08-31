package com.example.integration;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.test.context.MockIntegrationContext;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.test.context.event.annotation.AfterTestMethod;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@SpringBootTest
@SpringIntegrationTest
//@LongRunningTest // <-- only activate if the RUN_LONG_INTEGRATION_TESTS env var == true
class IntegrationApplicationTests {

    @Autowired
    private PublishSubscribeChannel output;

    @Autowired
    private MockIntegrationContext mockIntegrationContext;

    @Autowired
    private ApplicationContext context;

    static final String SOURCE_POLLING_CHANNEL_ADAPTER_ID =
            "file-to-string-flow.org.springframework.integration.config.SourcePollingChannelAdapterFactoryBean#0";

    @SneakyThrows
    private static File init(String message) {
        var temp = Files.createTempFile("temp", ".txt").toFile();
        try (var fw = new FileWriter(temp)) {
            FileCopyUtils.copy(message, fw);
        }
        return temp;
    }

    @Test
    public void integration() throws Exception {
        //enumerateBeanDefinitionNames();
        var atomicReference = new AtomicReference<String>();
        var countDownLatch = new CountDownLatch(1);
        var testMessage = "test @ " + Instant.now().toString();
        var mockMessageSource = MockIntegration.mockMessageSource(init(testMessage));
        this.mockIntegrationContext.substituteMessageSourceFor(SOURCE_POLLING_CHANNEL_ADAPTER_ID, mockMessageSource);
        this.output.subscribe(message -> {
            Assert.assertNotNull(message.getPayload());
            Object payload = message.getPayload();
            log.info("payload: " + payload);
            Assert.assertTrue("payload is a String", message.getPayload() instanceof String);
            atomicReference.set((String) message.getPayload());
            countDownLatch.countDown();
        });
        countDownLatch.await();
        Assert.assertNotNull(atomicReference.get());
    }

    @AfterTestMethod
    public void tearDown() {
        this.mockIntegrationContext.resetBeans();
    }

    private void enumerateBeanDefinitionNames() {
        String[] beanDefinitionNames = this.context.getBeanDefinitionNames();
        for (var beanDefinitionName : beanDefinitionNames)
            log.info(beanDefinitionName);
    }
}
