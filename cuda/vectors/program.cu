#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <thrust/host_vector.h>
#include <thrust/device_vector.h>

#define count 11	// giju skaicius
#define masyvuCount 5 // masyvu skaicius

struct Item
{
	char name[20 * count];
	int amount; 
	int eilNr; // eiles numeris masyve
	double price;
};

struct Supplier{
	struct Item items[count];
	int itemCount;
	char supplier[20];
};

void readFile(thrust::host_vector<Supplier> &suppliers);
void printFile(thrust::host_vector<Item> supplier);
void printSuppliers(thrust::host_vector<Supplier> suppliers);
void printItemsArray(struct Item supplier[], int itemCount);
void printItemsResults(thrust::host_vector<Item>);
Item addItems(thrust::device_vector<Supplier> suppliers, int index);


int main(int argc, char *argv[])
{
	//CPU vektorius
	thrust::host_vector<Supplier> suppliers(masyvuCount);
	readFile(suppliers);
	//pradiniu duomenu spausdinimas
	printSuppliers(suppliers);
	
 	// GPU vektoriaus sukurimas ir duomenu kopija i ji
	thrust::device_vector<Supplier> s = suppliers;
	// bendro vektoriaus sukurimas GPU 
	thrust::device_vector<Item> bendrasCuda(count);
	
	for (int i = 0; i < count; i++){
		bendrasCuda[i] = addItems(s, i);
	}

	// GPU kopija i CPU vektoriu
	thrust::host_vector<Item> bendras = bendrasCuda;
	
	printItemsResults(bendras);
	printFile(bendras);
	printf("Done \n");
	return 0;
}

/** Prekes sudejimas i viena eilute
*	@param suppliers - tiekeju masyvas
*	@param index - nurodo gijos konkrecia prekes vieta is kurios imsim
*/
Item addItems(thrust::device_vector<Supplier> suppliers, int index){
	Item it;
	it.amount = 0;
	it.price = 0;
	strcpy(it.name, "");
	it.eilNr = index;
	for (int i = 0; i < masyvuCount; i++){
		Supplier supplier = suppliers[i];
		it.amount = it.amount + supplier.items[index].amount;
		it.price = it.price + supplier.items[index].price;
		strcat(it.name, supplier.items[index].name);
	}
	return it;
}

/**	Tiekejo spausdinimas
*	@param suppliers - tiekejai
*/
void printSuppliers(thrust::host_vector<Supplier> suppliers){
	for (int i = 0; i < masyvuCount; i++)
	{
		printf("*** %s *** \n", suppliers[i].supplier);
		printf("  %-10s %-10s %-4s \n", "Name", "Amount", "Price");
		printItemsArray(suppliers[i].items, suppliers[i].itemCount);
	}
}

/**
*	Pradiniu prekes duomenu spausdinimas
* 	@param supplier - tiekejas
*	@param itemCount - prekiu skaicius jame
*/
void printItemsArray(struct Item supplier[], int  itemCount){
	for (int i = 0; i < itemCount; i++)
	{
		if (supplier[i].name){
			printf("%d %-10s %-10d %-4.2f \n", supplier[i].eilNr,
				supplier[i].name, supplier[i].amount, supplier[i].price);
		}
	}
}

/**
* Rezultato spausdinimas
* @param supplier - CPU vektorius is prekes
*/
void printItemsResults(thrust::host_vector<Item> supplier){
	printf("\nREZULTATAI \n\n");
	for (int i = 0; i < count; i++)
	{
		if (supplier[i].name){
			printf("%d %-50s %10d %4.2f \n", supplier[i].eilNr,
				supplier[i].name, supplier[i].amount, supplier[i].price);
		}
	}
}

/**
*	Skaitymas is failo
* @param suppliers - CPU vektorius i kuri saugosim duomenis
*/
void readFile(thrust::host_vector<Supplier> &suppliers){
	FILE *stream;							//failas
	char file_name[21] = "SankauskasS_L4.txt";  //failo vardas
	fopen_s(&stream, file_name, "r");
	char name[20];							// tiekejo pavadinimas
	int n;									// prekiu skaicius
	int supplierCount = 0;					// tiekeju iteracijos kintamasis
	while (true) {
		int readItems = fscanf(stream, "%s %d", name, &n);
		if (readItems == 2){
			strcpy(suppliers[supplierCount].supplier, name);
			suppliers[supplierCount].itemCount = n;
			for (int i = 0; i < n; i++){
				struct Item item = suppliers[supplierCount].items[i];
				char item_name[20];		// prekes pavadinimas
				int amount;				// prekes kiekis
				double price;			// prekes kaina
				fscanf(stream, "%s %d %lf", item_name, &amount, &price);
				suppliers[supplierCount].items[i].amount = amount;
				suppliers[supplierCount].items[i].eilNr = i;
				suppliers[supplierCount].items[i].price = price;
				strcpy(suppliers[supplierCount].items[i].name, item_name);
			}
			supplierCount++;
		}
		else if (readItems == EOF){
			break;
		}
	}
	if (stream)
		fclose(stream);
}

/**
* Rasymas i faila
*/
void printFile(thrust::host_vector<Item> supplier)
{
	FILE *f = fopen("SankauskasS_L4b_rez.txt", "w");
	if (f == NULL)
	{
		printf("Error opening file!\n");
		exit(1);
	}

	fprintf(f, "\nREZULTATAI \n\n");
	fprintf(f, "%-3s %-50s %-10s %-7s \n", "Nr" ,"Name", "Amount", "Price");
	for (int i = 0; i < count; i++)
	{
		if (supplier[i].name) {
			fprintf(f, "%-3d %-50s %-10d %-7.2f \n", supplier[i].eilNr, 
				supplier[i].name, supplier[i].amount, supplier[i].price);
		}
	}

	fclose(f);
}