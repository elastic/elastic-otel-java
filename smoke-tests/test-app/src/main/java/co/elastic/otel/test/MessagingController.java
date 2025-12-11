/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.otel.test;

import java.util.Enumeration;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessagingController {

  @Autowired
  public MessagingController(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  private final JmsTemplate jmsTemplate;

  @RequestMapping("/send/{destination}")
  public String send(
      @PathVariable(name = "destination") String destination,
      @RequestParam(name = "headerName", required = false) String headerName,
      @RequestParam(name = "headerValue", required = false) String headerValue) {
    jmsTemplate.send(
        destination,
        session -> {
          TextMessage message = session.createTextMessage("Hello World");
          if (headerName != null && headerValue != null) {
            message.setStringProperty(headerName, headerValue);
          }
          return message;
        });
    return null;
  }

  @RequestMapping("/receive/{destination}")
  public String receive(@PathVariable(name = "destination") String destination) throws JMSException {
    Message received = jmsTemplate.receive(destination);
    if (received instanceof TextMessage) {
      TextMessage textMessage = (TextMessage) received;
      StringBuilder sb = new StringBuilder();
      sb.append("message: [").append(textMessage.getText()).append("]");

      Enumeration<?> propertyNames = textMessage.getPropertyNames();
      if (propertyNames.hasMoreElements()) {
        sb.append(", headers: [");
        int count = 0;
        while (propertyNames.hasMoreElements()) {
          String propertyName = (String) propertyNames.nextElement();
          sb.append(count++ > 0 ? ", " : "")
              .append(propertyName)
              .append(" = ")
              .append(textMessage.getStringProperty(propertyName));
        }
        sb.append("]");
      }

      return sb.toString();
    } else {
      return "nothing received";
    }
  }
}
