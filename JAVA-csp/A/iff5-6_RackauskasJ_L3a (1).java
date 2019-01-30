import org.jcsp.lang.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        //Spusdintuvo kanalai
        One2AnyChannel printOut1 = Channel.one2any();
        One2AnyChannel printOut2 = Channel.one2any();
        //Kanalai tarp procesu
        Any2OneChannel writers = Channel.any2one();
        Any2OneChannel consumers = Channel.any2one();
        //Kiti kanlai
        Any2OneChannelInt controllerPrinter = Channel.any2oneInt();
        One2AnyChannelInt status = Channel.one2anyInt();
        One2OneChannel toPrint = Channel.one2one();

        Printer p00 = new Printer(printOut1.out(), printOut2.out(), controllerPrinter.out(), toPrint.in());
        Controller controller = new Controller(consumers.in(), writers.in(), controllerPrinter.in(), status.out(), toPrint.out() );

        //Pridedame procesus ir paleidziame juos
        Parallel threads = new Parallel();
        threads.addProcess(p00);
        threads.addProcess(controller);

        Writer writerArray[] = new Writer[5];
        for(int i=0; i<5; i++){
            writerArray[i] = new Writer(printOut1.in(), writers.out(), i);
        }
        threads.addProcess(writerArray);

        Reader readerArray[] = new Reader[4];
        for(int i=0; i<4; i++){
            readerArray[i] = new Reader(consumers.out(), printOut2.in(), status.in(), i);
        }
        threads.addProcess(readerArray);

        threads.run();


    }
}
class Writer implements CSProcess {
    private Student[] P;
    private ChannelInput reader;
    private ChannelOutput writer;
    private int id;

    public Writer(ChannelInput in, ChannelOutput out, int id) {
        this.reader = in;
        this.writer = out;
        this.id = id;
    }

    @Override
    public void run() {
        //reading data from file
        while (P == null) {
            P = (Student[]) reader.read();
        }
        System.out.println(P[0].getFaculcy());
        //pushing to list
        for (Student a : P) {
            writer.write(a.Average);
        }
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

class Reader implements CSProcess {

    private Bdata[] V;
    private ChannelOutput consume;
    private ChannelInput transfer;
    private ChannelInputInt status;
    private int possibleChanges;
    private boolean over;
    private int id;
    private int timesCalled;

    public Reader(ChannelOutput consume, ChannelInput transfer, ChannelInputInt status, int id) {
        this.consume = consume;
        this.transfer = transfer;
        this.status = status;
        possibleChanges = 1;
        over = false;
        this.id = id;
        this.timesCalled = 0;
    }

    @Override
    public void run() {
        //Laukiam duomenu
        while (V == null) {
            V = (Bdata[]) transfer.read();
        }

        while (true) {
            timesCalled++;
            System.out.println("Writer id " + this.id + " times called -" + timesCalled);
            consume.write(V);
               /* possibleChanges = status.read();
                if (possibleChanges == 0)
                    over = true;*/
        }
       /* System.out.println("Writer id " + this.id + " IS FINISHED");
        consume.write(V);*/

    }
}

 class Controller implements CSProcess {

    private Bdata[] B; //B masyvas
    private AltingChannelInput writer; //Is rasymo procesu ateinantis kanalas
    private AltingChannelInput reader; //Is skaitymo procesu ateinantis kanalas
    private ChannelInputInt isPrinterio; //Spausdinimo proceso kanalas
    private ChannelOutputInt status;
    private ChannelOutput toPrint;
    private List<Bdata> buveDuom;
    private int dataCount; //Duomenu kiekis b masyve
    private int maxData; //Duomenu kiekis faile
    private int actionCount; //Kiek ivykiu

    public Controller(AltingChannelInput reader, AltingChannelInput writer, ChannelInputInt isPrinterio, ChannelOutputInt status, ChannelOutput toPrint) {
        this.reader = reader;
        this.writer = writer;
        this.isPrinterio = isPrinterio;
        this.status = status;
        this.toPrint = toPrint;
        buveDuom = new ArrayList<>();
        dataCount = 0;
        actionCount = 0;

        B = new Bdata[100];
    }

    @Override
    public void run() {
        boolean[] galima = new boolean[3]; //Salygu masyvas kanalu alternatyvoms
        maxData = isPrinterio.read();
        Skip skip = new Skip();
        Guard[] g = new Guard[3]; //Guard masyvas galimiems kanalams pasirinkti
        g[0] = writer;
        g[1] = reader;
        g[2] = skip;

        Alternative alt = new Alternative(g);
        int finishedWriting = 0;
        while (finishedWriting != 5 ) { //Sukame cikla kol neiraasyti visi duomenys
            if(finishedWriting > 0 && dataCount ==0) {
                finishedWriting = 5;
            }

            //Kanalu salygos
            galima[0] = (actionCount < maxData);
            galima[1] = (dataCount > 0);
            galima[2] = true;

            //Pasirenkame alternatyva
            switch (alt.fairSelect(galima)) {
                case 0: //Rasymo alternatyva
                    Insert((double) writer.read());
                    break;
                case 1:
                    consumerMethod();
                    if(actionCount >= maxData){
                        finishedWriting++;
                    }
                    break;
                case 2:
                    break;

            }
        }
        toPrint.write(B);
    }

    private void consumerMethod(){
        Bdata[] skaitoma = (Bdata[]) reader.read();
        for (Bdata a : skaitoma) {
            if (!DoesExist(a)) {
                int f = Remove(a.field, a.count);
                if (f != -1)
                    buveDuom.add(a);
            }
        }
    }
    private void Insert(double a) {
        if (dataCount == 0) {
            B[0] = new Bdata(a, 1);
            dataCount++;
        } else {
            Boolean found = false;
            for (int i = 0; i < dataCount && !found; i++) {
                if (B[i] == null) {
                    B[i] = new Bdata(a, 1);
                    dataCount++;
                    found = true;

                } else if (a == B[i].field && !found) {
                    B[i].count++;
                    found = true;

                } else if (a < B[i].field) {
                    for (int j = dataCount - 1; j != i - 1; j--) {
                        B[j + 1] = B[j];

                    }
                    B[i] = new Bdata(a, 1);
                    dataCount++;
                    found = true;

                } else if ((i + 1) == dataCount) {
                    B[i + 1] = new Bdata(a, 1);
                    dataCount++;
                    found = true;

                }
            }
        }
        actionCount++;
    }

    private int Remove(double a, int kiek) {
        int index = -1;
        for (int i = 0; i < dataCount; i++) {
            if (B[i].field == a && B[i].count >= kiek)
                index = i;
        }

        if (index != -1) {

            double ats;
            if (B[index].count - kiek > 0) {
                B[index].count -= kiek;
                ats = B[index].field;
            } else {
                ats = B[index].field;

                for (int j = index; j < dataCount; j++) {
                    B[j] = B[j + 1];
                }
                dataCount--;
            }
        }


        return index;

    }

    private boolean DoesExist(Bdata lyg) {
        for (Bdata a : buveDuom) {
            if (a.count == lyg.count && a.field == lyg.field)
                return true;
        }
        return false;
    }
}

 class Printer implements CSProcess{
    private Student[][] P;
    private Bdata[][] Pb;
    private Bdata[] B;
    private int maxDuom;

    //Kanalai i visas klases
    private ChannelOutput writerChannel;
    private ChannelOutput readerChannel;
    private ChannelOutputInt valdiklisChannel;
    private ChannelInput printChannel;
    public Printer(ChannelOutput writerChannel, ChannelOutput readerChannel, ChannelOutputInt valdiklisChannel, ChannelInput printChannel) {
        this.readerChannel = readerChannel;
        this.writerChannel = writerChannel;
        this.valdiklisChannel = valdiklisChannel;
        this.printChannel = printChannel;
        P = new Student[5][];
        Pb = new Bdata[4][];
        maxDuom = 0;
    }

    @Override
    public void run() {
        try {

            readData();
        }
        catch (Exception e) {e.printStackTrace();}
        valdiklisChannel.write(maxDuom);
        for (Student[] a : P) {
            writerChannel.write(a);
        }

        for (Bdata[] a : Pb) {
            readerChannel.write(a);
        }

        //Pirmoji rezultatu dalis
        B = (Bdata[]) printChannel.read();
        try {
            BufferedWriter bf = new BufferedWriter(new FileWriter("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3a\\IFF5-6_RackauskasJ_L3a_rez.txt", true));
            bf.newLine();
            bf.write("*******************B masyvas*******************");
            bf.newLine();
            bf.write(String.format("%-9s %-9s", "Laukas", "Kiekis"));
            bf.newLine();
            for (Bdata a : B) {
                if (a != null)
                    bf.write(String.format("%-9f %-9d", a.field, a.count));
                bf.newLine();
            }
            bf.flush();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    //Duomenu nuskaitymas
    private void readData() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3a\\IFF5-6_RackauskasJ_L3a_dat1.txt"))) {
            BufferedWriter bf = new BufferedWriter(new FileWriter("C:\\Users\\traayce\\Desktop\\Lygiagretus-3\\L3a\\IFF5-6_RackauskasJ_L3a_rez.txt"));

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
                    bf.write(String.format(j + ")  %-16f    %-9d", temparr[j].field, temparr[j].count));
                    bf.newLine();
                }
                Pb[i] = temparr;
            }
            bf.flush();
            bf.close();
        }
    }

}
class Bdata {

    public double field;
    public int count;

    public Bdata(double field, int count) {
        this.field = field;
        this.count = count;
    }
}

