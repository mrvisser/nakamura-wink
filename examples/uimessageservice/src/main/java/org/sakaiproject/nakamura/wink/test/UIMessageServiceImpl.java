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
package org.sakaiproject.nakamura.wink.test;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.wink.AuthenticationHelper;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 *
 */
@Service({UIMessageService.class, Object.class})
@Component
@Property(name="javax.ws.rs", boolValue=true)
@Path("messageService")
@Produces({MediaType.APPLICATION_JSON})
public class UIMessageServiceImpl implements UIMessageService {

  @Reference
  AuthenticationHelper auth;

  public UIMessageServiceImpl() {  
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.wink.test.UIMessageService#createUIMessage(java.lang.String, java.lang.String)
   */
  @Override
  @GET
  @Path("createMessage/{to}")
  public UIMessage createUIMessage(@PathParam("to") String to, @QueryParam("body") String body) {
    return new UIMessage(auth.getCurrentUserId(), to, body);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.wink.test.UIMessageService#createUIMessages(java.lang.String[], java.lang.String)
   */
  @Override
  @GET
  @Path("createMessages")
  public List<UIMessage> createUIMessages(@QueryParam("to[]") String[] tos, @QueryParam("body") String body) {
    List<UIMessage> messages = new LinkedList<UIMessage>();
    
    if (tos != null) {
      for (String to : tos) {
        messages.add(createUIMessage(to, body));
      }
    }
    
    return messages;
  }

}
