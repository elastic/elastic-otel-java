package co.elastic.otel.test;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Enumeration;

@RestController
@RequestMapping("/messages")
public class MessagingController {

  private static final String DESTINATION = "messages-destination";

  @Autowired
  public MessagingController(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  private final JmsTemplate jmsTemplate;

  @RequestMapping("/send")
  public String send(
      @RequestParam(name = "headerName", required = false) String headerName,
      @RequestParam(name= "headerValue", required = false) String headerValue) {
    jmsTemplate.send(DESTINATION, session -> {
      TextMessage message = session.createTextMessage("Hello World");
      if (headerName != null && headerValue != null) {
        message.setStringProperty(headerName, headerValue);
      }
      return message;
    });
    return null;
  }

  @RequestMapping("/receive")
  public String receive() throws JMSException {
    Message received = jmsTemplate.receive(DESTINATION);
    if (received instanceof TextMessage) {
      TextMessage textMessage = (TextMessage) received;
      StringBuilder sb = new StringBuilder();
      sb.append("message: [")
          .append(textMessage.getText())
          .append("]");

      Enumeration<?> propertyNames = textMessage.getPropertyNames();
      if(propertyNames.hasMoreElements()) {
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
