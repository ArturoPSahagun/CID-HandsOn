package handson.handson55;

//import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class MultilinearGui extends JFrame {
    private Multilinear myAgent;

    private JTextField x1, x2, y;

    MultilinearGui(Multilinear a) {
        super(a.getLocalName());

        myAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("R&D Spend :"));
        x1 = new JTextField(15);
        p.add(x1);
        p.add(new JLabel("\nAdministration :"));
        x2 = new JTextField(15);
        p.add(x2);

        getContentPane().add(p, BorderLayout.CENTER);

        JButton boton = new JButton("Predecir");
        boton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String beta_1 = x1.getText().trim();
                    String beta_2 = x2.getText().trim();
                    double resultado = myAgent.predecir(Double.parseDouble(beta_1), Double.parseDouble(beta_2));

                    y.setText(resultado + "");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(MultilinearGui.this, "Invalid values. " + e.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        p = new JPanel();
        p.add(boton);
        p.add(new JLabel("\nProfit :"));
        y = new JTextField(15);
        p.add(y);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        });

        setResizable(false);
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
