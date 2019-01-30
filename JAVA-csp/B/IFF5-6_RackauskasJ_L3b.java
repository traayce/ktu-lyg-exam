import org.jcsp.lang.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        //Printerio kanalai
        One2AnyChannel printerioOut = Channel.one2any();
        One2AnyChannel printerioOut2 = Channel.one2any();

        //One2one kanalai tarprocesinei komunikacijai
        ArrayList<One2OneChannel> skaitytojaiList = new ArrayList<One2OneChannel>();
        ArrayList<One2OneChannel> rasytojaiList = new ArrayList<One2OneChannel>();
        ArrayList<AltingChannelInput> skaitytojaiListIn = new ArrayList<AltingChannelInput>();
        ArrayList<AltingChannelInput> rasytojaiListIn = new ArrayList<AltingChannelInput>();

        //in kanalu kurimas
        for (int i = 0; i < 4; i++) {
            skaitytojaiList.add(Channel.one2one());
            skaitytojaiListIn.add(skaitytojaiList.get(i).in());
        }
        // out kanalu kurimas
        for (int i = 0; i < 5; i++) {
            rasytojaiList.add(Channel.one2one());
            rasytojaiListIn.add(rasytojaiList.get(i).in());
        }

        Any2OneChannelInt valdiklisprinter = Channel.any2oneInt();
        //statusas readeriams
        One2AnyChannelInt statusas = Channel.one2anyInt();

        One2OneChannel toPrint = Channel.one2one();
        Printeris p00 = new Printeris(printerioOut.out(), printerioOut2.out(), valdiklisprinter.out(), toPrint.in());
        Controller controller = new Controller(skaitytojaiListIn, rasytojaiListIn, valdiklisprinter.in(), statusas.out(), toPrint.out() );

        Parallel visi = new Parallel();
        visi.addProcess(p00);
        visi.addProcess(controller);
        //out kanalu generavimas
        for (int i = 0; i < 4; i++) {
            visi.addProcess(new Reader(skaitytojaiList.get(i).out(), printerioOut2.in(), statusas.in()));
        }

        for (int i = 0; i < 5; i++) {
            visi.addProcess(new Writer(printerioOut.in(), rasytojaiList.get(i).out() ));
        }

        visi.run();
    }
}
class Student {
    public String Name;
    public String Faculcy;
    public int NumberOfGrades;
    public double Average;

    public Student(String name, int num, double avrg)
    {
        Name = name;
        NumberOfGrades = num;
        Average = avrg;
    }
    public String getFaculcy() {
        return Faculcy;
    }
}
//Rasytojo klase
class Writer implements CSProcess {
    private Student[] P;
    private ChannelInput perdavimas;
    private ChannelOutput rasymas;
    public Writer(ChannelInput in, ChannelOutput out) {
        this.perdavimas = in;
        this.rasymas = out;
    }

    @Override
    public void run() {
        //Laukiam duomenu is printerio
        while (P == null) {
            P = (Student[])perdavimas.read();
        }
        System.out.println(P[0].getFaculcy());
        //Rasom duomenis
        for (Student a : P) {
            rasymas.write(a.Average);
        }
    }
}
//Skaitytojo klase
class Reader implements CSProcess {

    private Bdata[] V;
    private ChannelOutput vartoti;
    private ChannelInput perdavimas;
    private ChannelInputInt statusas;
    private int galimiPasikeitimai;
    private boolean baigti;

    public Reader(ChannelOutput vartoti, ChannelInput perdavimas, ChannelInputInt statusas) {
        this.vartoti = vartoti;
        this.perdavimas = perdavimas;
        this.statusas = statusas;
        galimiPasikeitimai = 1;
        baigti = false;
    }

    @Override
    public void run() {
        //Laukiam duomenu
        while (V == null) {
            V = (Bdata[]) perdavimas.read();
        }
        //Bandome ieskoti duomenu ir juos tirnti
        while (!baigti) {
            vartoti.write(V);
            galimiPasikeitimai = statusas.read();
            if (galimiPasikeitimai == 0)
                baigti = true;
        }

        vartoti.write(V);


    }
}
//Spausdintuvo klase
class Printeris implements CSProcess{
    private Student[][] P;
    private Bdata[][] Pb;
    private Bdata[] B;
    private int maxDuom;
    private int maxRead;
    private ChannelOutput kanw;
    private ChannelOutput kanr;
    private ChannelOutputInt toController;
    private ChannelInput toPrint;
    public Printeris(ChannelOutput kanw, ChannelOutput kanr, ChannelOutputInt toController, ChannelInput toPrint) {
        this.kanr = kanr;
        this.kanw = kanw;
        this.toController = toController;
        this.toPrint = toPrint;
        P = new Student[5][];
        Pb = new Bdata[4][];
        maxDuom = 0;
        maxRead = 0;
    }

    @Override
    public void run() {
        try {
            readData();
        }
        catch (Exception e) {e.printStackTrace();}
        toController.write(maxDuom);
        toController.write(maxRead);
        for (Student[] a : P) {
            kanw.write(a);
        }

        for (Bdata[] a : Pb) {
            kanr.write(a);
        }

        //Rezultatu spausdinimas
        B = (Bdata[])toPrint.read();
        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3b - onefile\\IFF5-6_RackauskasJ_L3b_rez.txt", true));
            bf.newLine();
            bf.write("*******************B masyvas*******************");
            bf.newLine();
            bf.write(String.format("%-9s %-9s", "Laukas", "Kiekis"));
            bf.newLine();
            for (Bdata a : B) {
                if (a != null)
                    bf.write(String.format("%-9f %-9d", a.field, a.kiekis));
                bf.newLine();
            }
            bf.flush();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    //Nuskaitymas is failo
    private void readData() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3b - onefile\\IFF5-6_RackauskasJ_L3b_dat1.txt"))) {
            BufferedWriter bf = new BufferedWriter(new FileWriter("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3b - onefile\\IFF5-6_RackauskasJ_L3b_rez.txt"));

            String line;
            for (int i = 0; i < 5; i++) {
                String header = br.readLine();
                String[] headerA = header.split("\\s+");
                int numOfRecords = Integer.parseInt(headerA[1]);
                maxDuom += numOfRecords;
                String miestas = headerA[0];

                Student[] temparr = new Student[numOfRecords];

                bf.write("**** P" + (i+1) + " ****");
                bf.newLine();

                bf.write(String.format(" %-9s  %-9s   %-9s", "Vardas", "Vid", "Suma"));
                bf.newLine();

                for (int j = 0; j < numOfRecords; j++) {
                    String data = br.readLine();
                    String[] datasplit = data.split("\\s+");
                    temparr[j] = new Student(datasplit[0], Integer.parseInt(datasplit[1]), Double.parseDouble(datasplit[2]));
                    temparr[j].Faculcy = miestas;
                    bf.write(String.format(j + ") %-9s  %-9d  %-9f", temparr[j].Name, temparr[j].NumberOfGrades, temparr[j].Average));
                    bf.newLine();
                }
                P[i] = temparr;
            }

            for (int i = 0; i < 4; i++) {
                String header = br.readLine();
                String[] headerA = header.split("\\s+");
                int numOfRecords = Integer.parseInt(headerA[1]);
                maxRead += numOfRecords;
                String pav = headerA[0];
                Bdata[] temparr = new Bdata[numOfRecords];

                bf.write("**** V" + (i + 1) + " ****");
                bf.newLine();
                bf.write( String.format("%-16s %-9s", "Laukas", "Kiekis"));
                bf.newLine();

                for (int j = 0; j < numOfRecords; j++) {
                    String data = br.readLine();
                    String[] datasplit = data.split("\\s+");
                    temparr[j] = new Bdata(Double.parseDouble(datasplit[0]), Integer.parseInt(datasplit[1]));
                    bf.write(String.format(j + ")  %-16f    %-9d", temparr[j].field, temparr[j].kiekis));
                    bf.newLine();
                }
                Pb[i] = temparr;
            }
            bf.flush();
            bf.close();
        }
    }

}
class Controller implements CSProcess {
    private Bdata[] B;
    private Boolean galimiPasik;
    //Kanalu sarasai
    private List<AltingChannelInput> kwriters;
    private List<AltingChannelInput> kreaders;
    private ChannelInputInt isPrinterio;
    private ChannelOutputInt statusas;
    private ChannelOutput toPrint;
    private List<Bdata> buveDuom;
    private int duomKiekis; //B masyve esanciu duomenu kiekis
    private int maxData; //Kiek duomenu yra faile
    private int actionCount;
    private int remCounter;
    private int maxRead;
    private int warningCounter;

    public Controller(List<AltingChannelInput> kreader, List<AltingChannelInput> kwriter, ChannelInputInt isPrinterio, ChannelOutputInt statusas, ChannelOutput toPrint) {
        this.kreaders = kreader;
        this.kwriters = kwriter;
        this.isPrinterio = isPrinterio;
        this.statusas = statusas;
        this.toPrint = toPrint;
        buveDuom = new ArrayList<>();
        duomKiekis = 0;
        actionCount = 0;
        remCounter = 0;
        warningCounter = 0;
        B = new Bdata[100];
        galimiPasik = true;
    }

    @Override
    public void run() {
        boolean[] galima = new boolean[9]; //Galimu kanalu salygu masyvas

        maxData = isPrinterio.read();
        maxRead = isPrinterio.read();

        Skip skip = new Skip();
        //Nustatome guard masyva alternatyvoms
        Guard[] g = new Guard[] {kwriters.get(0), kwriters.get(1),kwriters.get(2),kwriters.get(3),kwriters.get(4), kreaders.get(0),kreaders.get(1),kreaders.get(2),kreaders.get(3)};
        Alternative alt = new Alternative(g);
        while (actionCount < maxData) { //Sukame cikla kol yra duomenu
            galimiPasik = false;

            //Salyginiai guardai
            for (int i = 0; i < 5; i++)
                galima[i] = (actionCount < maxData);
            for (int i = 0; i < 4; i++)
                galima[i+5] = (duomKiekis > 0);

            //Pagal gauta skaiciu is alternatyvos, atliekame titinkama veiksma
            int kanalas = alt.fairSelect(galima);
            //Rasyotojo procesas
            if ( kanalas < 5) {
                Insert((double) kwriters.get(kanalas).read());
            }
            else { //Skaitytojo procesas
                readChanel(kanalas - 5);
                if (actionCount < maxData)
                {
                    statusas.write(1);
                    warningCounter++;
                }
            }

        }

        //Pasibaigus darbui, dar karta prasukame cikla
        for (int i = 0; i < 4; i++) {
            if(duomKiekis == 0)
                break;
            readChanel(i);
        }

        toPrint.write(B);
    }

    private void readChanel(int channelId){
        Bdata[] skaitoma = (Bdata[]) kreaders.get(channelId).read();
        for (Bdata a : skaitoma) {
            if (!compareTo(a)) {
                int f = Remove(a.field, a.kiekis);
                if (f != -1)
                    buveDuom.add(a);
            }
        }
    }

    private void Insert(double a) {
        if (duomKiekis == 0) {
            B[0] = new Bdata(a, 1);
            duomKiekis++;
        } else {
            Boolean found = false;
            for (int i = 0; i < duomKiekis && !found; i++) {
                if (B[i] == null) {
                    B[i] = new Bdata(a, 1);
                    duomKiekis++;
                    found = true;

                } else if (a == B[i].field && !found) {
                    B[i].kiekis++;
                    found = true;

                } else if (a < B[i].field) {
                    for (int j = duomKiekis - 1; j != i - 1; j--) {
                        B[j + 1] = B[j];

                    }
                    B[i] = new Bdata(a, 1);
                    duomKiekis++;
                    found = true;

                } else if ((i + 1) == duomKiekis) {
                    B[i + 1] = new Bdata(a, 1);
                    duomKiekis++;
                    found = true;

                }
            }
        }
        actionCount++;
        galimiPasik = true;
    }

    private int Remove(double a, int kiek) {
        int index = -1;
        for (int i = 0; i < duomKiekis; i++) {
            if (B[i].field == a && B[i].kiekis >= kiek)
                index = i;
        }

        if (index != -1) {

            double ats;
            if (B[index].kiekis - kiek > 0) {
                B[index].kiekis -= kiek;
                ats = B[index].field;
            } else {
                ats = B[index].field;

                for (int j = index; j < duomKiekis; j++) {
                    B[j] = B[j + 1];
                }
                duomKiekis--;
            }
        }


        return index;

    }

    private boolean compareTo(Bdata lyg) {
        for (Bdata a : buveDuom) {
            if (a.kiekis == lyg.kiekis && a.field == lyg.field)
                return true;
        }
        return false;
    }
}


class Bdata {

    public double field;
    public int kiekis;

    public Bdata(double field, int kiekis) {
        this.field = field;
        this.kiekis = kiekis;
    }
}

