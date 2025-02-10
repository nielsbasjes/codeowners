/*
 * CodeOwners Tools
 * Copyright (C) 2023-2025 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.maven.enforcer.codeowners;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import wiremock.org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@WireMockTest
public class TestGitlabUsers {

    private static final String GET_ALL_MEMBERS = "/api/v4/projects/1/members/all"; // FIXME: Project '1'
    private static final String VALID_GITLAB_TOKEN = "glpat-validtoken";
    private static final String BAD_GITLAB_TOKEN = "glpat-bad";
    private static final String EMPTY_GITLAB_TOKEN = "";

    @Test
    public void getAllUsers(WireMockRuntimeInfo wmRuntimeInfo) throws IOException, InterruptedException, URISyntaxException {

        // Instance DSL can be obtained from the runtime info parameter
        WireMock wireMock = wmRuntimeInfo.getWireMock();
        String httpBaseUrl = wmRuntimeInfo.getHttpBaseUrl();

        URIBuilder b = new URIBuilder(httpBaseUrl);
        b.appendPath(GET_ALL_MEMBERS);
        b.addParameter("per_page", "1");
        b.addParameter("page", "1");

        URI uri = b.build();

        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .setHeader("PRIVATE-TOKEN", VALID_GITLAB_TOKEN)
            .build();
        HttpResponse<String> response =
            client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}
