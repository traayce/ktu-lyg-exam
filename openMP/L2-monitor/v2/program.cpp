#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <iomanip>
#include <omp.h>
#define MAX_WORKER_COUNT 50

using namespace std;

static const int PROVIDER_COUNT = 5;
static const int MODIFIER_COUNT = 4;
static int insertCount = 0;
static int changesArr[MODIFIER_COUNT];
static string fileName = "SankauskasS_L2b_dat_1.txt";

struct Data {
	string name;
	int count;
	Data(int k = 0, string p = "") :
		name(p), count(k) {}
};

struct TheadWrapper {
	Data array[MAX_WORKER_COUNT];
	string name;
	int count;
	TheadWrapper(string p = "", int k = 0) :
		name(p), count(k) {}
	TheadWrapper(Data d[], string p, int c) {
		for (int i = 0; i < count; i++) {
			array[i] = d[i];
		}
		name = p;
		count = c;
	}
};

struct DataItem {
	int number;
	string text;
	double decimal;
	DataItem(int i = 0, string s = "", double d = 0.0) :
		number(i), text(s), decimal(d) {}
};

struct DataItemGroup {
	string name;
	DataItem itemArr[MAX_WORKER_COUNT];
	int count;
	DataItemGroup(string n = "", int c = 0) :
		name(n), count(c) {}
	DataItemGroup(DataItem s[], string n, int c) {
		for (int i = 0; i < count; i++) {
			itemArr[i] = s[i];
		}
		name = n;
		count = c;
	}
};

struct Monitor {
	Data data[MAX_WORKER_COUNT];
	int count;
	Monitor(int n = 0) :
		count(n) {}
	Monitor(int n, Data b[]) {
		for (int i = 0; i < n; i++) {
			data[i] = b[i];
		}
	}

	void insert(Data st) {
		if (data[0].name == "") {
			data[0] = st;
			count++;
		}
		else {
			bool newElement = true;
			for (int i = 0; i < count; i++) {
				if (data[i].name == st.name) {
					data[i].count += st.count;
					newElement = false;
				}
			}
			if (newElement) {
				int ind = placeToInsert(st);
				if (ind == count) {
					data[ind] = st;
				}
				else {
					for (int i = count - 1; i >= ind; i--) {
						data[i + 1] = data[i];
					}
					data[ind] = st;
				}
				count++;
			}
		}
	}

	int placeToInsert(Data st) {
		int ind = 0;
		for (; ind < count; ind++) {
			int a = st.name.compare(data[ind].name);
			if (a <= 0) {
				return ind;
			}
		}
		return ind;
	}

	int take(Data st) {
		int ind = found(st);
		if (ind == -1) {
			return -1;
		}
		data[ind].count = data[ind].count - st.count;

		if (data[ind].count == 0) {
			remove(ind);
		}
		return 1;
	}

	int found(Data st) {
		for (int i = 0; i < count; i++)
			if (data[i].name == st.name && data[i].count >= st.count)
				return i;
		return -1;
	}

	void remove(int ind) {
		data[ind] = Data();
		for (int i = ind; i < count; i++) {
			data[i] = data[i + 1];
		}
		count--;
	}
};

void printResult(ofstream &rez);
void readFile(DataItemGroup itemGroupArr[], TheadWrapper workerArr[], int &readerCount, int &workerCount);
void printFile(DataItemGroup itemGroupArr[], TheadWrapper workerArr[], int readerCount, int workerCount, ofstream &rez);

static Monitor dataMonitor;

int main() {
	int readerCount = 0;
	int workerCount = 0;
	DataItemGroup itemArr[PROVIDER_COUNT];
	TheadWrapper workerArr[MODIFIER_COUNT];

	for (int i = 0; i < MODIFIER_COUNT; i++) {
		changesArr[i] = 0;
	}

	ofstream rez;
	rez.open("SankauskasS_L2b_rez.txt");

	readFile(itemArr, workerArr, readerCount, workerCount);
	printFile(itemArr, workerArr, readerCount, workerCount, rez);

#pragma omp parallel num_threads(readerCount + workerCount)
	{
		if (omp_get_thread_num() < readerCount) {
#pragma omp critical
			{
				cout << itemArr[omp_get_thread_num()].name << endl;
				for (int i = 0; i < itemArr[omp_get_thread_num()].count; i++) {
					Data element = Data(itemArr[omp_get_thread_num()].itemArr[i].number,
						itemArr[omp_get_thread_num()].itemArr[i].text);
					dataMonitor.insert(element);
				}
				insertCount++;
			}
		}
		else {
			int d = omp_get_thread_num() - readerCount;
			cout << workerArr[d].name << endl;
			while (insertCount != readerCount || changesArr[d] < workerArr[d].count) {
				changesArr[d] = 0;
				while (dataMonitor.data->count == 0 && insertCount != 5) {
					cout << "wait" << endl;
				}
#pragma omp critical
				{
					for (int i = 0; i < workerArr[d].count; i++) {
						int element = dataMonitor.take(workerArr[d].array[i]);
						if (element == -1) {
							changesArr[d]++;
						}
					}
				}
			}
		}
	}

	cout << "-- Results --" << endl << endl;
	for (int i = 0; i < dataMonitor.count; i++) {
		cout << dataMonitor.data[i].name << " " << dataMonitor.data[i].count << endl;
	}
	cout << dataMonitor.count << endl;

	printResult(rez);
	rez.close();
	cout << "Done." << endl;
	return 0;
}

void readFile(DataItemGroup itemArr[], TheadWrapper workerArr[], int &readerCount, int &workerCount) {
	int count;
	string name;
	DataItem dateItemArr[MAX_WORKER_COUNT];
	Data dataArr[MAX_WORKER_COUNT];

	ifstream file;
	file.open(fileName);

	if (file.is_open()) {
		while (!file.eof()) {
			file >> name >> count;
			if (readerCount < PROVIDER_COUNT) {
				for (int i = 0; i < count; i++) {
					file >> dateItemArr[i].text;
					file >> dateItemArr[i].number;
					file >> dateItemArr[i].decimal;
				}
				itemArr[readerCount++] = DataItemGroup(dateItemArr, name, count);
			}
			else {
				for (int i = 0; i < count; i++) {
					file >> dataArr[i].name;
					file >> dataArr[i].count;
				}
				workerArr[workerCount++] = TheadWrapper(dataArr, name, count);
			}
		}
	}
	file.close();
}

void printResult(ofstream &rez) {
	rez << "Results" << endl << endl;
	for (int i = 0; i < dataMonitor.count; i++) {
		rez << i + 1;
		rez << dataMonitor.data[i].name;
		rez << dataMonitor.data[i].count;
		rez << endl;
	}
	rez << dataMonitor.count << endl;
}

void printFile(DataItemGroup itemArr[], TheadWrapper workerArr[], int readerCount, int workerCount, ofstream &rez) {
	rez << "Data" << endl;
	rez << "Items List" << endl;

	for (int i = 0; i < readerCount; i++) {
		rez << itemArr[i].name << endl;
		rez << "Item" << "Count" << "Price" << endl;
		for (int j = 0; j < itemArr[i].count; j++) {
			rez << j + 1;
			rez << itemArr[i].itemArr[j].text;
			rez << itemArr[i].itemArr[j].number;
			rez << fixed << setprecision(2) << itemArr[i].itemArr[j].decimal;
			rez << endl;
		}
		rez << endl;
	}
	rez << " Buyers" << endl;

	for (int i = 0; i < workerCount; i++) {
		rez << workerArr[i].name << endl;
		rez << "Item" << "Count" << endl;
		for (int j = 0; j < workerArr[i].count; j++) {
			rez << j + 1;
			rez << workerArr[i].array[j].name;
			rez << workerArr[i].array[j].count;
			rez << endl;
		}
		rez << endl;
	}
}