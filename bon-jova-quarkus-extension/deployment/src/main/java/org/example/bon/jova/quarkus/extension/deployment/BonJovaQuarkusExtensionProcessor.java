package org.example.bon.jova.quarkus.extension.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class BonJovaQuarkusExtensionProcessor {

    private static final String FEATURE = "bon-jova-quarkus-extension";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
