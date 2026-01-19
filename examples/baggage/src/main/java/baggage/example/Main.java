package baggage.example;

import co.elastic.otel.agent.attach.RuntimeAttach;

public class Main {
  public static void main(String[] args) throws Exception {

    RuntimeAttach.attachJavaagentToCurrentJvm();

    String arg = args.length > 0 ? args[0] : null;
    if (arg == null) {
      throw new RuntimeException("missing argument: 'backend' or 'gateway' expected");
    }

    SimpleServer server;
    switch (arg) {
      case "backend":
        server = SimpleServer.createBackend(9000);
        break;
      case "gateway":
        boolean useBaggageApi = !(args.length > 1 && args[1].equals("no-baggage-api"));
        String backendUrl = args.length > 2 ? args[2] : "http://localhost:9000/backend/";
        System.out.printf("gateway server %s baggage API for backend %s%n",
            useBaggageApi ? "using" : "not using", backendUrl);
        server = SimpleServer.createGateway(8000, useBaggageApi, backendUrl);
        break;
      default:
        throw new RuntimeException("unsupported argument value: " + arg);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    System.out.printf("%s server started on url %s, hit ctrl-c to terminate%n", arg, server.getUrl());
    server.start();


  }
}
