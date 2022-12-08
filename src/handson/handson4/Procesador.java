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
import java.lang.Math;

import java.util.ArrayList;
import java.util.List;

public class Procesador extends Agent {
    private AID lector;
    calculadora calc = new calculadora();

    Double mipendiente, miintercepcion;

    protected void setup() {
        // Register in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("procesor");
        sd.setName("JADE-SLR");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // fin register yellow pages

        addBehaviour(new OneShotBehaviour(this) {
            public void action() {

                // look for servicce
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("reader");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    lector = new AID();
                    lector = result[0].getName();
                    System.out.println(lector.getName());
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
                // fin look for services

                myAgent.addBehaviour(new RequestPerformer());
            }

            class RequestPerformer extends Behaviour {
                private List<Double[]> datos = new ArrayList<Double[]>();
                private int step = 0;
                private MessageTemplate mt;

                public void action() {
                    switch (step) {
                        case 0:
                            // Send the cfp to lector
                            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                            cfp.addReceiver(lector);
                            cfp.setContent("DameDatos");
                            cfp.setConversationId("pasar-datos");
                            cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                            myAgent.send(cfp);
                            // Prepare the template to get proposals
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("pasar-datos"),
                                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                            step = 1;
                            break;
                        case 1:
                            // Receive all proposals/refusals from lector
                            ACLMessage reply = myAgent.receive(mt);
                            if (reply != null) {
                                // Reply received
                                if (reply.getPerformative() == ACLMessage.PROPOSE) {
                                    // This is an offer
                                    System.out.println("El lector me dijo que : " + reply.getContent());
                                }
                                step = 2;

                            } else {
                                block();
                            }
                            break;
                        case 2:
                            // Send the purchase order to the seller that provided the best offer
                            ACLMessage confirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            confirmation.addReceiver(lector);
                            confirmation.setContent("si se pudo");
                            confirmation.setConversationId("pasar-datos");
                            confirmation.setReplyWith("order" + System.currentTimeMillis());
                            myAgent.send(confirmation);
                            // Prepare the template to get the purchase order reply
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("pasar-datos"),
                                    MessageTemplate.MatchInReplyTo(confirmation.getReplyWith()));
                            step = 3;
                            break;
                        case 3:
                            // Receive the purchase order reply
                            reply = myAgent.receive(mt);

                            if (reply != null) {

                                // Purchase order reply received
                                if (reply.getPerformative() == ACLMessage.INFORM) {
                                    String[] rows = reply.getContent().split("\\|");

                                    for (int i = 0; i < rows.length; i++) {

                                        // System.out.println(rows[i]);

                                        Double[] instance = new Double[2];
                                        String[] partido = rows[i].split(",");
                                        // instance[0] = Float.parseFloat(partido[0]);
                                        // instance[1] = Float.parseFloat(partido[1]);
                                        instance[0] = Double.parseDouble(partido[0]);
                                        instance[1] = Double.parseDouble(partido[1]);

                                        datos.add(instance);

                                    }

                                } else {

                                    System.out.println("Algo malio sal");
                                }
                                if (datos.size() > 0) {
                                    System.out.println("He recibido " + datos.size() + "instancias");

                                    Double[] xs = new Double[datos.size()];
                                    Double[] ys = new Double[datos.size()];

                                    for (int i = 0; i < datos.size(); i++) {
                                        xs[i] = datos.get(i)[0];
                                        ys[i] = datos.get(i)[1];

                                    }

                                    Double coefCorr = calc.covarianza(xs, ys) / (calc.desvStd(xs) * calc.desvStd(ys));
                                    Double pendiente = coefCorr * (calc.desvStd(ys) / calc.desvStd(xs));
                                    Double intercepcion = calc.promedio(ys) - (pendiente * calc.promedio(xs));

                                    miintercepcion = intercepcion;
                                    mipendiente = pendiente;
                                }
                                // System.out.println("Pendiente = " + mipendiente);
                                // System.out.println("Intercepcion = " + miintercepcion);
                                step = 4;
                            } else {
                                block();
                            }
                            break;
                    }
                }

                public boolean done() {

                    return (step == 4);

                }

            }
        });

        addBehaviour(new OfferRequestsServer());
        addBehaviour(new GiveBetas());
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
                reply.setContent("Si ya calcule las betas");

                myAgent.send(reply);
            } else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer

    private class GiveBetas extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.INFORM);
                String data = Double.toString(mipendiente) + "," + Double.toString(miintercepcion);

                reply.setContent(data);

                myAgent.send(reply);
                myAgent.doDelete();
            } else {
                block();
            }
        }
    } // End of inner class OfferRequestsServer

    class calculadora {
        public Double promedio(Double[] dataset) {
            int tam = dataset.length;
            Double total = (double) 0;
            for (int i = 0; i < tam; i++) {
                total += dataset[i];
            }
            return total / tam;
        }

        public Double varianza(Double[] xs) {
            int tam = xs.length;
            Double avg = this.promedio(xs);
            Double numerador = (double) 0;
            for (int i = 0; i < tam; i++) {
                Double a = xs[i] - avg;
                numerador += a * a;
            }
            return numerador / tam;
        }

        public Double desvStd(Double[] xs) {
            Double var = this.varianza(xs);
            return Math.sqrt((double) var);
        }

        public Double covarianza(Double[] xs, Double[] ys) {
            int tam = xs.length;
            Double avgx = this.promedio(xs);
            Double avgy = this.promedio(ys);
            Double numerador = (double) 0;
            for (int i = 0; i < tam; i++) {
                Double a = xs[i] - avgx;
                Double b = ys[i] - avgy;

                numerador += a * b;
            }
            return numerador / tam;
        }
    }

}
