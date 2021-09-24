package Client;

import java.util.ArrayList;

//Classe per la gestione della coda dei messaggi ricevuti nelle chat di progetto
public class Queue {

    private ArrayList<String> queue;
    public Queue(){ queue = new ArrayList<>();}
    public synchronized void put(String m){queue.add(m);}

    //Restituisce l'intero contenuto della coda(tutti i messaggi di chat non ancora letti)
    //Ed elimina i messaggi dopo averli restituiti
    public synchronized ArrayList<String> readClear() {
        ArrayList<String> temp = new ArrayList<>(queue);
        queue.clear();
        return temp;
    }
}
