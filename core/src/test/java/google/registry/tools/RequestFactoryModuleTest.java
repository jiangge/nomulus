// Copyright 2017 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.tools;

import static com.google.common.truth.Truth.assertThat;
import static google.registry.tools.RequestFactoryModule.REQUEST_TIMEOUT_MS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import google.registry.config.RegistryConfig;
import google.registry.testing.SystemPropertyRule;
import google.registry.util.GoogleCredentialsBundle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class RequestFactoryModuleTest {

  @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public final SystemPropertyRule systemPropertyRule = new SystemPropertyRule();

  @Mock public GoogleCredentialsBundle credentialsBundle;
  @Mock public HttpRequestInitializer httpRequestInitializer;

  @Before
  public void setUp() {
    RegistryToolEnvironment.UNITTEST.setup(systemPropertyRule);
    when(credentialsBundle.getHttpRequestInitializer()).thenReturn(httpRequestInitializer);
  }

  @Test
  public void test_provideHttpRequestFactory_localhost() throws Exception {
    // Make sure that localhost creates a request factory with an initializer.
    boolean origIsLocal = RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal;
    RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal = true;
    try {
      HttpRequestFactory factory =
          RequestFactoryModule.provideHttpRequestFactory(credentialsBundle);
      HttpRequestInitializer initializer = factory.getInitializer();
      assertThat(initializer).isNotNull();
      HttpRequest request = factory.buildGetRequest(new GenericUrl("http://localhost"));
      initializer.initialize(request);
      verifyZeroInteractions(httpRequestInitializer);
    } finally {
      RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal = origIsLocal;
    }
  }

  @Test
  public void test_provideHttpRequestFactory_remote() throws Exception {
    // Make sure that example.com creates a request factory with the UNITTEST client id but no
    boolean origIsLocal = RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal;
    RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal = false;
    try {
      HttpRequestFactory factory =
          RequestFactoryModule.provideHttpRequestFactory(credentialsBundle);
      HttpRequestInitializer initializer = factory.getInitializer();
      assertThat(initializer).isNotNull();
      // HttpRequestFactory#buildGetRequest() calls initialize() once.
      HttpRequest request = factory.buildGetRequest(new GenericUrl("http://localhost"));
      verify(httpRequestInitializer).initialize(request);
      assertThat(request.getConnectTimeout()).isEqualTo(REQUEST_TIMEOUT_MS);
      assertThat(request.getReadTimeout()).isEqualTo(REQUEST_TIMEOUT_MS);
      verifyNoMoreInteractions(httpRequestInitializer);
    } finally {
      RegistryConfig.CONFIG_SETTINGS.get().appEngine.isLocal = origIsLocal;
    }
  }
}
