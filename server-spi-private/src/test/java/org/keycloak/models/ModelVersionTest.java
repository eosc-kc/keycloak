/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import org.keycloak.migration.ModelVersion;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ModelVersionTest {

    @Test
    public void testVersion() {
        ModelVersion version_100Beta1 = new ModelVersion("1.0.0.Beta1-SNAPSHOT");
        Assert.assertEquals(1, version_100Beta1.getMajor());
        Assert.assertEquals(0, version_100Beta1.getMinor());
        Assert.assertEquals(0, version_100Beta1.getMicro());

        ModelVersion version_100CR1 = new ModelVersion("1.0.0.CR1");
        ModelVersion version_100 = new ModelVersion("1.0.0");
        ModelVersion version_110Beta1 = new ModelVersion("1.1.0.Beta1");
        ModelVersion version_110CR1 = new ModelVersion("1.1.0.CR1");
        ModelVersion version_110 = new ModelVersion("1.1.0");

        ModelVersion version_120CR1 = new ModelVersion("1.2.0.CR1");
        ModelVersion version_130Beta1 = new ModelVersion("1.3.0.Beta1");
        ModelVersion version_130 = new ModelVersion("1.3.0");
        ModelVersion version_140 = new ModelVersion("1.4.0");

        ModelVersion version_211CR1 = new ModelVersion("2.1.1.CR1");
        Assert.assertEquals(2, version_211CR1.getMajor());
        Assert.assertEquals(1, version_211CR1.getMinor());
        Assert.assertEquals(1, version_211CR1.getMicro());
        Assert.assertEquals("CR1", version_211CR1.getQualifier());

        ModelVersion version_211 = new ModelVersion("2.1.1");

        ModelVersion version50Snapshot = new ModelVersion("5.0.0-SNAPSHOT");
        Assert.assertEquals(5, version50Snapshot.getMajor());
        Assert.assertEquals(0, version50Snapshot.getMinor());
        Assert.assertEquals(0, version50Snapshot.getMicro());
        Assert.assertNull(version50Snapshot.getQualifier());
    }

}
