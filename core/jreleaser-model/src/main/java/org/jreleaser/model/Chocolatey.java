/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2021 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.model;

import org.jreleaser.util.Env;
import org.jreleaser.util.PlatformUtils;

import java.util.Map;

import static org.jreleaser.util.Constants.HIDE;
import static org.jreleaser.util.Constants.UNSET;
import static org.jreleaser.util.StringUtils.isBlank;
import static org.jreleaser.util.StringUtils.isNotBlank;

/**
 * @author Andres Almiray
 * @since 0.1.0
 */
public class Chocolatey extends AbstractRepositoryTool {
    public static final String NAME = "chocolatey";
    public static final String CHOCOLATEY_API_KEY = "CHOCOLATEY_API_KEY";

    private final ChocolateyBucket bucket = new ChocolateyBucket();
    private String username;
    private String apiKey;
    private Boolean remoteBuild;

    public Chocolatey() {
        super(NAME);
    }

    void setAll(Chocolatey choco) {
        super.setAll(choco);
        this.username = choco.username;
        this.apiKey = choco.apiKey;
        this.remoteBuild = choco.remoteBuild;
        setBucket(choco.bucket);
    }

    public String getResolvedApiKey() {
        return Env.resolve(CHOCOLATEY_API_KEY, apiKey);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isRemoteBuild() {
        return remoteBuild != null && remoteBuild;
    }

    public void setRemoteBuild(Boolean remoteBuild) {
        this.remoteBuild = remoteBuild;
    }

    public boolean isRemoteBuildSet() {
        return remoteBuild != null;
    }

    public ChocolateyBucket getBucket() {
        return bucket;
    }

    public void setBucket(ChocolateyBucket bucket) {
        this.bucket.setAll(bucket);
    }

    @Override
    protected void asMap(boolean full, Map<String, Object> props) {
        super.asMap(full, props);
        props.put("username", username);
        props.put("apiKey", isNotBlank(getResolvedApiKey()) ? HIDE : UNSET);
        props.put("remoteBuild", isRemoteBuild());
        props.put("bucket", bucket.asMap(full));
    }

    @Override
    public RepositoryTap getRepositoryTap() {
        return bucket;
    }

    @Override
    public boolean supportsPlatform(String platform) {
        return isBlank(platform) || PlatformUtils.isWindows(platform);
    }

    @Override
    public boolean supportsDistribution(Distribution distribution) {
        return distribution.getType() != Distribution.DistributionType.SINGLE_JAR &&
            distribution.getType() != Distribution.DistributionType.NATIVE_IMAGE &&
            distribution.getType() != Distribution.DistributionType.NATIVE_PACKAGE &&
            distribution.getType() != Distribution.DistributionType.BINARY;
    }
}
