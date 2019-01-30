using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace lab2a
{

    class BData
    {
        public double Field;
        public int Count;

        public BData(double field, int count)
        {
            this.Field = field;
            this.Count = count;
        }
    }
    class BArray
    {
        private BData[] b;
        private int DataAmmount;


        public BArray(int count)
        {
            b = new BData[count];
            DataAmmount = 0;
        }

        public BData[] Like()
        {
            lock (b)
            {
                BData[] ats = new BData[DataAmmount];
                Array.Copy(b, ats, DataAmmount);
                return ats;
            }
        }

        public void Insert(double a)
        {

            awaitNotFull();
            lock (b)
            {
                if (DataAmmount == 0) // jei sąrašas tuščias
                {
                    b[0] = new BData(a, 1);
                    DataAmmount++;
                }
                else
                {
                    var found = false;
                    for (int i = 0; i < DataAmmount && !found; i++)
                    {
                        if (b[i] == null)
                        { // randama tuščia vieta įterpimui
                            b[i] = new BData(a, 1);
                            DataAmmount++;
                            found = true;

                        }
                        else if (a == b[i].Field && !found) // tokia reikšmė jau yra, didinam count
                        {
                            b[i].Count++;
                            found = true;

                        }
                        else if (a < b[i].Field)
                        {
                            for (int j = DataAmmount - 1; j != i - 1; j--)
                            {
                                b[j + 1] = b[j]; // perstumiame elementus į priekį

                            }
                            b[i] = new BData(a, 1);
                            DataAmmount++;
                            found = true;

                        }
                        else if ((i + 1) == DataAmmount) // jei reikšmė didžiausia, dedame į sąrašo galą
                        {
                            b[i + 1] = new BData(a, 1);
                            DataAmmount++;
                            found = true;

                        }
                    }
                }
                //Console.WriteLine(DataAmmount);
                Monitor.PulseAll(b);
            }

        }

        public double Remove(int i)
        {
            awaitNotEmpty();
            lock (b) // įeiname į kritinę sekciją
            {
                double ats;
                if (b[i].Count > 1) // jei yra kelios šios reikšmės atitikmenys
                {
                    b[i].Count--;
                    ats = b[i].Field;
                }
                else
                {
                    ats = b[i].Field;

                    for (int j = i; j < DataAmmount; j++)
                    {
                        b[j] = b[j + 1]; // perstumiame reikšmes masyve
                    }
                    DataAmmount--;
                }
                Monitor.PulseAll(b); // pažadinam laukiančius threadus
                return ats;
            }
        }

        public int Find(double value, int count)
        {
            lock (b) // įeiname į kritinę sekciją
            {
                for (int i = 0; i < DataAmmount; i++)
                {
                    if (b[i].Field == value && b[i].Count >= count)
                    {
                        return i;
                    }
                }
                return -1;
            }

        }

        public void awaitNotFull()
        {
            while (DataAmmount == b.Length)
            {
                Monitor.Wait(b);
            }

        }
      
        public void awaitNotEmpty()
        {
            while (DataAmmount == 0)
            {
                Monitor.Wait(b);
            }
        }

    }
    class Student
    {
        public string Name { get; set; }
        public int GradeCount { get; set; }
        public double GradeAverage { get; set; }
        public int index { get; set; }

        public override string ToString()
        {
            return string.Format("{0,-2}{1,-15} {2, -10}  {3, -10}", index + ")", Name, GradeCount, GradeAverage);
        }
    }
    class Program
    {
        static List<Thread> formerThreads = new List<Thread>(); //Formavimo gijos
        static List<Thread> consumerThreads = new List<Thread>(); //Formavimo gijos
        static BArray B;
        private static bool Over = false;
        private static int dataCount = 0;
       
        static void Main(string[] args)
        {

            ReadFromFile("iff5-6_RackauskasJ_L2b_dat_3.txt");

            B = new BArray(dataCount - 1);

             foreach (Thread a in consumerThreads)
             {
                 a.Start();

             }
             foreach (Thread a in formerThreads)
             {
                 a.Start();
             }

            foreach (Thread a in formerThreads)
            {
                a.Join();

            }
            Over = true;
            foreach (Thread a in consumerThreads)
            {
                a.Join();

            }

            PrintToFile();
        }
        public static void ReadFromFile(string filename)
        {
            string[] lines = System.IO.File.ReadAllLines(filename);
            StreamWriter write = new StreamWriter("iff5-6_RackauskasJ_L2a_rez.txt");
            int faculcy = 0;
            int breakIndex = 0;
            for (int i = 0; i < lines.Length; i++)
            {
                string[] duom = lines[i].Split(' ');
                int linesCount = Int32.Parse(duom[1]);
                int count = linesCount + i + 1;
                dataCount += linesCount;
                int indexer = 0;
                //Console.WriteLine($"{Int32.Parse(duom[1])} indekso dydis");
                write.WriteLine($"   {"Vardas",-10} {"Kiekis",-10} {"Vidurkis",-10}");
                Student[] students = new Student[linesCount];
                for (int m = i + 1; m < count; m++)
                {
                    string[] studentData = lines[m].Split(' ');
                    //Console.WriteLine(studentData[0]);
                    students[indexer] = new Student
                    {
                        Name = studentData[0],
                        GradeCount = Int32.Parse(studentData[1]),
                        GradeAverage = Double.Parse(studentData[2]),
                        index = indexer
                    };
                    indexer++;
                    write.WriteLine(indexer + $") {studentData[0],-10} {studentData[1],-10} {studentData[2],-10}");
                }
                i = count - 1;
                Thread threadas = new Thread(delegate ()
                {
                    Program.Form(students);
                });
                threadas.Name = "gija_" + i;
                formerThreads.Add(threadas);
                faculcy++;
                if (faculcy == 5)
                {
                    breakIndex = i + 1;
                    break;
                }
            }

            for (int i = breakIndex; i < lines.Length; i++)
            {
                string[] duom = lines[i].Split(' ');
                int count = Int32.Parse(duom[1]) + i + 1;
                int indexer = 0;
                var consumers = new BData[Int32.Parse(duom[1])];
                //Console.WriteLine($"{Int32.Parse(duom[1])} indekso dydis");
                write.WriteLine(duom[0]);
                write.WriteLine($"   {"Vardas",-10} {"Kiekis",-10}");
                for (int m = i + 1; m < count; m++)
                {
                    string[] data = lines[m].Split(' ');
                    consumers[indexer] = new BData(double.Parse(data[0]), Int32.Parse(data[1]));
                    indexer++;
                    write.WriteLine(indexer + $") {data[0],-10} {data[1],-10}");
                }
                i = count - 1;
                Thread threadas = new Thread(delegate ()
                {
                    Program.Consume(consumers);
                });
                threadas.Name = "gija_" + i;
                consumerThreads.Add(threadas);
            }
            write.Flush();
            write.Close();
        }
        static void Form(Student[] students)
        {
            for (int i = 0; i < students.Length; i++)
            {
                lock (B) // įeiname į kritinę sekciją
                {
                    B.Insert(students[i].GradeAverage);
                }
            }
        }

        static void PrintToFile()
        {
            StreamWriter write = new StreamWriter("IFF5-6_RackauskasJ_L2a_rez.txt", true);
            write.WriteLine("_________ B masyvas ____________");
            write.WriteLine("{0, -5} {1, -5}", "Laukas", "Kiekis");
            BData[] like = B.Like();
            foreach (BData a in like)
            {
                write.WriteLine("{0,-5} {1, -5}", a.Field, a.Count);
            }
            write.Flush();
            write.Close();
        }
        static void Consume(BData[] duom)
        {
            int[] kiekVartoti = new int[duom.Length];
            for (int i = 0; i < kiekVartoti.Length; i++)
            {
                kiekVartoti[i] = duom[i].Count;
            }
            var checkAgain = false;

            while (!Over || checkAgain)
            {
                checkAgain = false;
                for (int i = 0; i < kiekVartoti.Length; i++)
                {
                    lock (B)
                    {
                        if (kiekVartoti[i] > 0)
                        {
                            int indeksas = B.Find(duom[i].Field, 1);
                            if (indeksas != -1)
                            {
                                B.Remove(indeksas);
                                kiekVartoti[i]--;
                                checkAgain = true;
                            }
                        }
                    }


                }
            }


        }
    }
}
