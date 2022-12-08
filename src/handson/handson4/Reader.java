package handson.handson4;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.io.*;

public class Reader extends Agent {
    String data = "";

    protected void setup() {
        System.out.println("Hello World! My name is " + getLocalName());

        // Register in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("reader");
        sd.setName("JADE-SLR");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        String filename = "50_Startups2.csv";
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            filename = (String) args[0];
        }

        String line = "";

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            line = br.readLine();
            while ((line = br.readLine()) != null) // returns a Boolean value
            {
                String[] row = line.split(",");
                // System.out.printf("Instancia: %.2f ,,,, %.2f", instance[0], instance[1]);
                data += row[0];
                data += ",";
                data += row[4];
                data += "|";
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new GiveData());

    }

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent("si lo tengo");

                myAgent.send(reply);
            } else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer

    private class GiveData extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(data);

                myAgent.send(reply);
                myAgent.doDelete();
            } else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer
}
