#include <iostream>
#include <fstream>
#include <sstream>
#include <cstring>
#include <omp.h>
#include <string>
#include <thread>

using namespace std;

struct student {
	string name;
	int gradeCount;
	double Average;
};


struct b_data {
	double field;
	int count;
	bool set;
};

const int recordsCount = 28;
const int faculciesCount = 5;
int doneCounter = 0;

void ReadFromFile();
void PrintPrimaryData();
void Insert(double a);
void Remove(int index);
int Find(double a, int count);
void PrintToFile();

void Consume();
void Form();
void awaitNotFull();
void awaitNotEmpty();

student P[5][10];
b_data b_duomenis[4][10];

//OMP lock
omp_lock_t *lock;

//B masyvas ir duom
b_data B[recordsCount];
int dataAmmount = 0;

int array_lengths[5];
int array_lengthsV[4];
int main()
{
	lock = new omp_lock_t;
	omp_init_lock(lock); // sukuriame locka
	ReadFromFile();
	PrintPrimaryData();

	for (int i = 0; i < recordsCount; i++) {
		B[i].set = false; // masyve nustatome, kad visos reikšmės tuščios
	}

	int tid;
	omp_set_num_threads(faculciesCount + 4 + 1); // paleidžiame gijas, kiek fakultetu + vartotojų gijų
#pragma omp parallel private(tid)
	{
		tid = omp_get_thread_num();

		if (tid <= faculciesCount && tid > 0) { // formavimo gijos

			Form();
		}
		else if (tid > faculciesCount) // vartojimo gijos
		{
			Consume();
		}
#pragma omp barrier // susinchronizuojame gijas

	}

	PrintToFile();

	return 0;
}

void ReadFromFile() {
	ifstream failas("iff5-6_RackauskasJ_L2b_dat_1.txt");
	string vardas;
	int temp;
	int count;
	double dtemp;

	for (int i = 0; i < faculciesCount; i++) {
		failas >> vardas >> count;
		array_lengths[i] = count;

		for (int j = 0; j < count; j++) {
			failas >> P[i][j].name >> P[i][j].gradeCount >> P[i][j].Average;
		}
	}

	for (int i = 0; i < 4; i++) {
		failas >> vardas >> count;
		array_lengthsV[i] = count;

		for (int j = 0; j < count; j++) {
			failas >> b_duomenis[i][j].field >> b_duomenis[i][j].count;
		}
	}
}


void PrintPrimaryData() {
	ofstream out("iff5-6_RackauskasJ_L2b_rez.txt");
	for (int i = 0; i < faculciesCount; i++) {
		out << "   Vardas	kiekis vidurkis" << endl;
		for (int j = 0; j < array_lengths[i]; j++) {
			char buffer[100];
			snprintf(buffer, sizeof(buffer), "%-2d) %-10s %-10d %-10f \n", j, P[i][j].name.c_str(), P[i][j].gradeCount, P[i][j].Average);
			out << buffer;
		}
	}

	for (int i = 0; i < 4; i++) {
		out << "V" << i << endl;
		out << "   Vardas   Kiekis" << endl;
		for (int j = 0; j < array_lengthsV[i]; j++) {
			char buffer[100];
			snprintf(buffer, sizeof(buffer), "%-2d) %-10f %-10d \n", j, b_duomenis[i][j].field, b_duomenis[i][j].count);
			out << buffer;
		}
	}
	out.flush();
	out.close();
}

void Insert(double a) {
	awaitNotFull();
	omp_set_lock(lock);

	if (dataAmmount == 0) { // jei sąrašas tuščias
		B[0].field = a;
		B[0].count = 1;
		B[0].set = true;
		dataAmmount++;
	}
	else
	{
		bool found = false;
		for (int i = 0; i < dataAmmount && !found; i++) {
			if (B[i].set == false) {
				B[i].field = a;
				B[i].count = 1;
				B[i].set = true;
				dataAmmount++;
				found = true;
			}
			else if (a == B[i].field && !found) { // jei laukas jau yra sąraše
				B[i].count++;
				found = true;
			}
			else if (a < B[i].field) {
				for (int j = dataAmmount - 1; j != i - 1; j--) {
					B[j + 1] = B[j];
					B[j + 1].set = true;
				}
				B[i].field = a;
				B[i].count = 1;
				dataAmmount++;
				found = true;
			}
			else if ((i + 1) == dataAmmount) { // Dedame į sąrašo galą
				B[i + 1].field = a;
				B[i + 1].count = 1;
				B[i + 1].set = true;
				dataAmmount++;
				found = true;
			}
		}
	}
	omp_unset_lock(lock);

}

void Remove(int index) {
	awaitNotEmpty();
	omp_set_lock(lock); //kritine sekcija
	if (B[index].count > 1) { // jei laukas įrašytas kelis kartus
		B[index].count--;
	}
	else // jei ne ištrinam ir perstumiam masyvo elementus
	{
		for (int j = index; j < dataAmmount; j++) {
			B[j] = B[j + 1];
		}
		dataAmmount--;
		B[dataAmmount].set = false;
	}

	omp_unset_lock(lock); // atiduodam kritinę sekciją
}

int Find(double a, int count) {
	omp_set_lock(lock); // kritinė sekcija

	for (int i = 0; i < dataAmmount; i++) {
		if (B[i].field == a && B[i].count >= count) {
			omp_unset_lock(lock);
			return i;
		}
	}
	omp_unset_lock(lock);
	return -1;

}


void awaitNotFull() {
	while (dataAmmount == recordsCount) {

	}
}

void awaitNotEmpty() {
	while (dataAmmount == 0) {

	}
}

void Form() {
	for (int i = 0; i < array_lengths[omp_get_thread_num() - 1]; i++) {
		Insert(P[omp_get_thread_num() - 1][i].Average);
	}
	doneCounter++;
}

void Consume() {
	bool changes = true;
	while (doneCounter != 5 || changes) {
		changes = false;
		for (int i = 0; i < array_lengthsV[omp_get_thread_num() - 6]; i++) {
			if (b_duomenis[omp_get_thread_num() - 6][i].count > 0) {
				int index = Find(b_duomenis[omp_get_thread_num() - 6][i].field, 1);
				if (index != -1) {
					Remove(index);
					b_duomenis[omp_get_thread_num() - 6][i].count--;
					changes = true;
				}
			}
		}

	}
}

void PrintToFile() {
	ofstream out("iff5-6_RackauskasJ_L2b_rez.txt", std::ios_base::app);
	out << "**** B" << " ****" << endl;
	out << "  Laukas  Kiekis" << endl;
	for (int i = 0; i < dataAmmount; i++) {
		char buffer[100];
		snprintf(buffer, sizeof(buffer), "  %-6f %-6d \n", B[i].field, B[i].count);
		out << buffer;
	}
	out.flush();
	out.close();
}
