/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.wink;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.wink.common.internal.runtime.RuntimeDelegateImpl;
import org.apache.wink.server.internal.DeploymentConfiguration;
import org.apache.wink.server.internal.RequestProcessor;
import org.apache.wink.server.internal.servlet.RestServlet;
import org.apache.wink.server.utils.RegistrationUtils.InnerApplication;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Servlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 *
 */
@Service(Servlet.class)
@Component(immediate=true, metatype=true)
@Property(name="alias", label="API Context Path", value=NakamuraWinkServlet.DEFAULT_API_CONTEXT,
  description="The top-level context from which to access the Nakamura APIs. Must start with a '/'")
public class NakamuraWinkServlet extends RestServlet {
  private static final long serialVersionUID = 1L;

  public static final String DEFAULT_API_CONTEXT = "/api";

  private static final MessageBodyWriter<Object> PROVIDER_JSON = createJsonProvider();
  
  /**
   * This static block is necessary to ensure that the JAX-RS API package does not try
   * and load the com.sun...RuntimeDelegateImpl implementation that is not on our
   * classpath. We must first put in a dummy that will ensure no additional classes
   * will be loaded by the bundle, then we can swap in the actual ApacheWink delegate.
   * 
   * The reason we don't just put in the Wink one to start with is because it will cause
   * additional classes to load that will try and access the RuntimeDelegate statically
   * before the Wink one actually has time to set.
   * 
   * :(
   */
  static {
    RuntimeDelegate.setInstance(new RuntimeDelegate() {
      public <T> T createEndpoint(Application arg0, Class<T> arg1) throws IllegalArgumentException, UnsupportedOperationException {return null;}
      public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> arg0) {return null;}
      public ResponseBuilder createResponseBuilder() {return null;}
      public UriBuilder createUriBuilder() {return null;}
      public VariantListBuilder createVariantListBuilder() {return null;}
    });
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
  }
  
  private final Object configLock = new Object();
  
  protected DeploymentConfiguration deploymentConfiguration;
  
  @Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE, policy=ReferencePolicy.DYNAMIC,
      referenceInterface=Object.class, target="(javax.ws.rs=true)", bind="bindApplication",
      unbind="unbindApplication")
  protected List<Object> applications = new LinkedList<Object>();

  @Override
  protected DeploymentConfiguration getDeploymentConfiguration() {
    // note: we can't bootstrap this in the activate method because it needs
    // the servlet context, which isn't created until after jetty executes
    // init() (after activation).
    if (deploymentConfiguration == null) {
      synchronized (configLock) {
        if (deploymentConfiguration == null) {
          deploymentConfiguration = internalCreateDeploymentConfiguration();
        }
      }
    }
    return deploymentConfiguration;
  }
  
  protected void bindApplication(Object obj) {
    applications.add(obj);
    getDeploymentConfiguration().addApplication(createApplication(obj), false);
  }
  
  protected void unbindApplication(Object obj) throws ClassNotFoundException,
      InstantiationException, IllegalAccessException, IOException {
    applications.remove(obj);
    
    // swap in a new requestProcessor with a rebuilt configuration
    deploymentConfiguration = internalCreateDeploymentConfiguration();
    RequestProcessor requestProcessor = createRequestProcessor();
    storeRequestProcessorOnServletContext(requestProcessor);
  }
  
  protected Application createApplication(Object obj) {
    return new InnerApplication(obj);
  }
  
  private static MessageBodyWriter<Object> createJsonProvider() {
    ObjectMapper mapper = new ObjectMapper();
    AnnotationIntrospector primary = new JaxbAnnotationIntrospector();
    AnnotationIntrospector secondary = new JacksonAnnotationIntrospector();
    AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
    mapper.getDeserializationConfig().withAnnotationIntrospector(pair);
    mapper.getSerializationConfig().withAnnotationIntrospector(pair);
    
    // Set up the JAX-RS provider
    JacksonJaxbJsonProvider jaxbProvider = new JacksonJaxbJsonProvider();
    jaxbProvider.setMapper(mapper);
    return jaxbProvider;
  }
  
  private DeploymentConfiguration internalCreateDeploymentConfiguration() {
    try {
      DeploymentConfiguration result = super.getDeploymentConfiguration();
      
      // add the jackson provider that has jaxb compatibility
      result.getProvidersRegistry().addProvider(PROVIDER_JSON, 100);
      
      for (Object app : applications) {
        result.addApplication(createApplication(app), false);
      }
      
      return result;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
