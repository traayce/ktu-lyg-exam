#include <iostream>
#include <fstream>
#include <sstream>
#include <cstring>
#include <omp.h>

#include <chrono>
#include <thread>

using namespace std;
const int records = 19;
struct student {
	string name;
	int count;
	double average;
	int threadnum;
}CommonArray[records];

string Names[records];
int Counts[records];
double Averages[records];
int Index = 0;

void Read() {
	string name;
	int gradeCount;
	double gradeAverage;
	ifstream file("iff5-6_RackauskasJ_L1b_dat.txt");
	int i = 0;
	while (file >> name >> gradeCount >> gradeAverage) {
		Names[i] = name;
		Counts[i] = gradeCount;
		Averages[i] = gradeAverage;
		i++;
	}
	file.close();
}
void Write() {
	ofstream file("iff5-6_RackauskasJ_L1b_rez.txt");
	file << "____Data___\n";
	file << "    Names      Counts Averages\n";
	for (int i = 0; i < records; i++) {
		char buffer[100];
		snprintf(buffer, sizeof(buffer), "%-2d) %-10s %-6d %-6f \n", i, Names[i].c_str(), Counts[i], Averages[i]);
		file << buffer;
	}
	file << "\n____CommonArray____\n";
	for (int i = 0; i < records; i++) {
		char buffer[100];
		snprintf(buffer, sizeof(buffer), "gija_%d %-10s %-6d %-6f \n", CommonArray[i].threadnum, CommonArray[i].name.c_str(), CommonArray[i].count, CommonArray[i].average);
		file << buffer;
	}
	file.flush();
	file.close();
}
int main()
{
	Read();
	omp_set_num_threads(records + 1);

#pragma omp parallel 
	{
		int indeksas = Index;
		Index++;
		int gijosNr = omp_get_thread_num();
		CommonArray[indeksas].threadnum = gijosNr;
		CommonArray[indeksas].name = Names[gijosNr];
		CommonArray[indeksas].count = Counts[gijosNr];
		CommonArray[indeksas].average = Averages[gijosNr];
	}
	Write();
	return 0;
}