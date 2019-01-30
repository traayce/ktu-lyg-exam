using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace L1a
{

    class Student
    {
        public string Name { get; set; }
        public int GradeCount { get; set; }
        public double GradeAverage { get; set; }
        public int index { get; set; }

        public override string ToString()
        {
            return string.Format("{0,-2}{1,-15} {2, -10}  {3, -10}",index + ")", Name, GradeCount, GradeAverage);
        }
    }

    class Faculcy
    {
        public string FaculcyName { get; set; }
        public List<Student> Students { get; set; }

        public Faculcy()
        {
            Students = new List<Student>();
        }
    }

    class Test
    {
        /// <summary>
        /// Skaitymas iš failo
        /// </summary>
        /// <param name="filename"></param>
        /// <returns></returns>
        public List<Faculcy> ReadFromFile(string filename)
        {
            string[] lines = System.IO.File.ReadAllLines(filename);
            List<Faculcy> faculcies = new List<Faculcy>();
            for (int i = 0; i < lines.Length; i++)
            {
                string[] duom = lines[i].Split(' ');
                Faculcy faculcy = new Faculcy {FaculcyName = duom[0]};
                int count = Int32.Parse(duom[1]) + i + 1;
                int indexer = 0;
                for (int m = i + 1; m < count; m++)
                {
                    indexer++;
                    string[] studentData = lines[m].Split(' ');
                    faculcy.Students.Add(
                        new Student
                        {
                            Name = studentData[0],
                            GradeCount = Int32.Parse(studentData[1]),
                            GradeAverage = Double.Parse(studentData[2]),
                            index = indexer
                        });
                }
                i = count - 1;
                faculcies.Add(faculcy);
            }
            return faculcies;
        }

        public List<string> testList = new List<string>();

        /// <summary>
        /// Metodas kviečiamas gijos vykdymo metu
        /// </summary>
        /// <param name="faculcy"></param>
        public void test(Faculcy faculcy)
        {
            foreach (Student student in faculcy.Students)
            {
                testList.Add(String.Format("{0,10}{1}", Thread.CurrentThread.Name, student));
            }
        }
        /// <summary>
        /// Gijų sąrašo išspausdinimas
        /// </summary>
        public void Print()
        {
            foreach (string thread in testList)
            {
                Console.WriteLine(thread);
            }
        }
        /// <summary>
        /// Saugojimas į failą
        /// </summary>
        /// <param name="filename"></param>
        /// <param name="faculcies"></param>
        public void SaveToFile(string filename, List<Faculcy> faculcies)
        {
            using (System.IO.StreamWriter file =
                new System.IO.StreamWriter(filename))
            {
                foreach (Faculcy faculcy in faculcies)
                {
                    file.WriteLine("{0,15}", faculcy.FaculcyName);
                    file.WriteLine("{0, -2}{1,-15} {2, -10}  {3, -10}"," ", "Vardas", "Kursas", "Vidurkis");
                    foreach (Student student in faculcy.Students)
                    {
                        file.WriteLine(student.ToString());
                    }
                }
                foreach (string thread in testList)
                {
                    file.WriteLine(thread);
                }
            }
        }

        class Program
        {
            /// <summary>
            /// Pagrindinis programos metodas
            /// </summary>
            /// <param name="args"></param>
            static void Main(string[] args)
            {
                Test test = new Test();
                List<Faculcy> faculcies =
                    test.ReadFromFile("iff5-6_RačkauskasJ_L1a_dat.txt");
                int i = 0;

                foreach (Faculcy faculcy in faculcies)
                {
                    i++;
                    Thread thredas = new Thread(() => { test.test(faculcy); });
                    thredas.Name = i.ToString() + "_gija ";
                    thredas.Start();
                }
                new Thread(() => { test.Print(); }).Start();
                test.SaveToFile("iff5-6_RačkauskasJ_L1a_rez.txt", faculcies);
            }
        }
    }
}
