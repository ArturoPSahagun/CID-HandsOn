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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PredictorGUI extends Agent {
    private Gui miGui;
    private AID procesador;
    Double mipendiente, miintercepcion;

    protected void setup() {

        miGui = new Gui(this);
        miGui.showGui();

        // look for servicce
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("procesor");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);

            procesador = new AID();
            procesador = result[0].getName();
            System.out.println(procesador.getName());
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // fin look for services

        // Register in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        sd = new ServiceDescription();
        sd.setType("predictor");
        sd.setName("JADE-SLR");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new RequestPerformer());

    }

    class RequestPerformer extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to procesador
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(procesador);
                    cfp.setContent("DameLasBetas");
                    cfp.setConversationId("pasar-betas");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("pasar-betas"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from procesador
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            System.out.println("El procesador me dijo que : " + reply.getContent());
                        }
                        step = 2;

                    } else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage confirmation = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    confirmation.addReceiver(procesador);
                    confirmation.setContent("si se pudo");
                    confirmation.setConversationId("pasar-betas");
                    confirmation.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(confirmation);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("pasar-betas"),
                            MessageTemplate.MatchInReplyTo(confirmation.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);

                    if (reply != null) {

                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            String[] rows = reply.getContent().split(",");

                            mipendiente = Double.parseDouble(rows[0]);
                            miintercepcion = Double.parseDouble(rows[1]);

                        } else {

                            System.out.println("Algo malio sal");
                        }

                        System.out.println("He recibido la siguiente ecuacion : " + Double.toString(mipendiente)
                                + "x + " + Double.toString(miintercepcion));

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

    private class Gui extends JFrame {
        private JTextField input, output;

        Gui(PredictorGUI myAgent) {
            JPanel p = new JPanel();
            p.setLayout(new GridLayout(2, 2));

            p.add(new JLabel("Input:"));
            input = new JTextField(15);
            p.add(input);

            p.add(new JLabel("Output:"));
            output = new JTextField(15);
            p.add(output);

            getContentPane().add(p, BorderLayout.CENTER);

            JButton aberButton = new JButton("aber");
            aberButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ev) {
                    try {
                        Double inp = Double.parseDouble(input.getText().trim());

                        Double out = inp * mipendiente;
                        out += miintercepcion;
                        output.setText(Double.toString(out));

                    } catch (Exception e) {
                        // JOptionPane.showMessageDialog("Invalid values. "+e.getMessage(), "Error",
                        // JOptionPane.ERROR_MESSAGE);
                        System.out.println("Algo salio mal");

                    }
                }
            });
            p = new JPanel();
            p.add(aberButton);
            getContentPane().add(p, BorderLayout.SOUTH);
            setResizable(false);

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    myAgent.doDelete();
                }
            });

        }

        public void showGui() {
            pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int centerX = (int) screenSize.getWidth() / 2;
            int centerY = (int) screenSize.getHeight() / 2;
            setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
            super.setVisible(true);
        }
    }
}
