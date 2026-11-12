package com.qasimha.sdk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionTest {

  @Test
  void versionConstantMatchesThePom() {
    String pomVersion = System.getProperty("qasimha.sdk.pomVersion");
    assertNotNull(pomVersion, "surefire passes the pom version as qasimha.sdk.pomVersion");
    assertEquals(pomVersion, Version.VERSION);
  }

  @Test
  void userAgentCarriesTheExactVersion() {
    QasimhaClient client = QasimhaClient.builder().apiKey(Fixtures.TEST_KEY).build();
    assertTrue(client.getUserAgent().startsWith("qasimha-sdk-java/" + Version.VERSION + " (java/"));
  }
}
