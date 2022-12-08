package handson.handson55;

import jade.core.Agent;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

//(X'X)-1 X'Y
public class Multilinear extends Agent {
    private List<double[]> dataset = new ArrayList<double[]>();
    private List<Double> whys = new ArrayList<Double>();
    private double[] beta = new double[3];

    private MultilinearGui myGui;

    protected void setup() {
        System.out.println("Inicializando agente .... ");

        myGui = new MultilinearGui(this);
        myGui.showGui();

        addBehaviour(new Reader());
        addBehaviour(new Procesar());

    }

    private class Reader extends Behaviour {
        public void action() {
            String line = "";
            try {
                BufferedReader br = new BufferedReader(new FileReader("50_Startups.csv"));
                line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] row = line.split(",");
                    double[] instance = new double[3];
                    instance[0] = Double.parseDouble("1");
                    instance[1] = Double.parseDouble(row[0]);
                    instance[2] = Double.parseDouble(row[1]);
                    Multilinear.this.dataset.add(instance);
                    Multilinear.this.whys.add(Double.parseDouble(row[4]));
                }
                br.close();
            } catch (IOException e) {
                System.out.print("No se encontró o no se pudo leer el archivo especificado.\n\nLeyendo dataset hardcodeado......\n\n");
                int[][] rawdataset = {
                    {5, 7, 11},
                    {3, 3, 23},
                    {7, 22, 12},
                    {2, 11, 22},
                    {7, 3, 33},
                    {8, 5, 23},
                    {3, 6, 11},
                    {4, 3, 21},
                    {8, 7, 25}
                };
                for(int i = 0; i < rawdataset.length; i++){
                        double[] instance = new double[3];
                        instance[0] = Double.parseDouble("1");
                        instance[1] = rawdataset[i][0];
                        instance[2] = rawdataset[i][1];
                        Multilinear.this.dataset.add(instance);
                        Multilinear.this.whys.add(Double.valueOf(rawdataset[i][2]));
                }
                //e.printStackTrace();
            }
        }

        public boolean done() {
            return true;
        }
        /*
         * public int onEnd() {
         * myAgent.doDelete();
         * return super.onEnd();
         * }
         */
    }

    private class Procesar extends Behaviour {
        public void action() {
            double[][] x = new double[Multilinear.this.dataset.get(0).length][Multilinear.this.dataset.size()];
            x = Multilinear.this.dataset.toArray(x);

            double[][] y = new double[Multilinear.this.whys.size()][1];
            for (int i = 0; i < y.length; i++) {
                y[i][0] = Multilinear.this.whys.get(i);
            }

            double[][] inv = (inversa(matrMult(traspuesta(x), x)));

            double[][] betas = matrMult(inv, traspuesta(x));
            betas = matrMult(betas, y);

            for (int i = 0; i < betas.length; i++) {
                System.out.print("Beta_" + i + ": " + betas[i][0] + ", ");
                Multilinear.this.beta[i] = betas[i][0];
            }
        }

        public boolean done() {
            return true;
        }
    }

    public double[][] matrMult(double[][] a, double[][] b) {
        int arows = a.length, acols = a[0].length, brows = b.length, bcols = b[0].length;
        double[][] c = new double[arows][bcols];

        double sum = 0;
        for (int i = 0; i < c.length; i++) {
            for (int j = 0; j < c[0].length; j++) {
                sum = 0;
                for (int k = 0; k < acols; k++) {
                    sum += a[i][k] * b[k][j];
                }
                c[i][j] = sum;
            }
        }

        return c;
    }

    public double[][] traspuesta(double[][] input) {

        double[][] output = new double[input[0].length][input.length];

        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < input[0].length; j++) {
                output[j][i] = input[i][j];
            }
        }
        return output;
    }

    public double det(double[][] input) {
        double dete = 0;
        if (input.length == 1) {
            dete = input[0][0];
        } else if (input.length == 2) {
            double posDiag = input[0][0] * input[1][1];
            double negDiag = input[1][0] * input[0][1];
            dete = posDiag - negDiag;
        } else if (input.length == 3) {
            double posDiag = (input[0][0] * input[1][1] * input[2][2]) +
                    (input[0][1] * input[1][2] * input[2][0]) +
                    (input[0][2] * input[1][0] * input[2][1]);

            double negDiag = (input[2][0] * input[1][1] * input[0][2]) +
                    (input[2][1] * input[1][2] * input[0][0]) +
                    (input[2][2] * input[1][0] * input[0][1]);
            dete = posDiag - negDiag;
        }
        return dete;
    }

    public double[][] getMenor(double[][] input, int row, int col) {
        double[][] menor = new double[input.length - 1][input[0].length - 1];
        int x = 0, y = 0;
        for (int i = 0; i < menor.length; i++) {
            for (int j = 0; j < menor[0].length; j++) {
                if (i >= row) {
                    x = i + 1;
                } else {
                    x = i;
                }
                if (j >= col) {
                    y = j + 1;
                } else {
                    y = j;
                }
                menor[i][j] = input[x][y];
            }
        }
        return menor;
    }

    public double[][] adjunta(double[][] input) {
        double[][] output = new double[input.length][input[0].length];
        double signo = 0;
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output[0].length; j++) {
                signo = ((i + j) % 2 == 1) ? -1 : 1;

                output[i][j] = signo * det(getMenor(input, i, j));
            }
        }
        return output;
    }

    public double[][] matrizDiv(double[][] numerador, double denominador) {
        double[][] cociente = new double[numerador.length][numerador[0].length];
        for (int i = 0; i < cociente.length; i++) {
            for (int j = 0; j < cociente[0].length; j++) {
                cociente[i][j] = numerador[i][j] / denominador;
            }
        }
        return cociente;
    }

    //A-1 = Adj(At) / det(A)
    public double[][] inversa(double[][] input) {
        double[][] output = new double[input.length][input[0].length];
        output = adjunta(traspuesta(input));
        output = matrizDiv(output, det(input));
        return output;
    }

    public double predecir(double x1, double x2) {
        return x1 * beta[1] + x2 * beta[2] + beta[0];
    }
}
