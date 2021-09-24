package Server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Project {
    private String pName;
    private CopyOnWriteArrayList<String> projectMembers;
    private CopyOnWriteArrayList<Card> toDo;
    private CopyOnWriteArrayList<Card> inProgress;
    private CopyOnWriteArrayList<Card> toBeRevised;
    private CopyOnWriteArrayList<Card> done;

    private String ip;
    CopyOnWriteArrayList<String> ipAddresses;

    public Project(String projectName) {
        this.pName = projectName;
        this.projectMembers = new CopyOnWriteArrayList<>();
        this.toDo = new CopyOnWriteArrayList<>();
        this.inProgress = new CopyOnWriteArrayList<>();
        this.toBeRevised = new CopyOnWriteArrayList<>();
        this.done = new CopyOnWriteArrayList<>();
    }


    public String getIp(){return  this.ip;}
   // public void setIp(String iP) { this.ip = iP; }

    public Project(String user, String prjName){
        this(prjName);
        boolean flag=false;
        String iP="";
        //creo ipMulticast e verifico se già utilizzato
        //se non è utilizzato esco dal while e salvo ip nel file degli ipMulticast
        //altrimenti ne genero un altro
        while(!flag) {
            //crea ip Multicast per chat
            int oct1=239;
            int oct2=255;

            // int oct2 = (int) ((Math.random() * (255 + 1 - 0)) + 0);
            int oct3 = (int) ((Math.random() * (255 + 1 - 0)) + 0);
            int oct4 = (int) ((Math.random() * (255 + 1 - 0)) + 0);

            iP = oct1 + "." + oct2 + "." + oct3 + "." + oct4;

            //se ip valido
            if (compareIP(ip)) {
                flag=true;
            }
        }
        this.ip = iP;
        //System.out.println(ip);
        ipAddresses.add(this.ip);
        storeIP();
        this.projectMembers.addIfAbsent(user);

    }


    //confronta l'ip appena generato con quelli già utilizzati
    public boolean compareIP(String ip){
        compareIP();
        if(ipAddresses == null) {
            ipAddresses = new CopyOnWriteArrayList<>();
        }else{
            for (String i : ipAddresses) {
                if (i.equals(ip))
                    return false;
            }
        }
        return true;
    }

    //Metodo per la lettura dal file degli ip
    public synchronized void compareIP() {
        BufferedReader reader;
        try {
            Gson gson = new Gson();
            reader = new BufferedReader(new FileReader(("src/StoredData/ProjectDir/ip.json")));
            Type type = new TypeToken<CopyOnWriteArrayList<String>>() {
            }.getType();
            ipAddresses = gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
    }
    }

    //metodo per il salvataggio degli ip su file
    public synchronized boolean storeIP() {
        Writer writer;
        try {
            writer = new FileWriter("src/StoredData/ProjectDir/ip.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(ipAddresses, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //Metodo per eliminare un ip dal file ip.json usato nel metodo cancelProject
    public void deleteIP(String ip) throws IOException {
        for(String ipAdd : ipAddresses){
            if(ipAdd.equals(ip))
                ipAddresses.remove(ip);
        }
        storeIP();
    }
    //Metodo che restituisce il nome di un progetto
    public String getProjectName() {
        return this.pName;
    }

    //Metodi get delle liste
    public CopyOnWriteArrayList<Card> getToDo(){return toDo;}
    public CopyOnWriteArrayList<Card> getInProgress(){return inProgress;}
    public CopyOnWriteArrayList<Card> getToBeRevised(){return toBeRevised;}
    public CopyOnWriteArrayList<Card> getDone(){return done;}

    //Metodo che ritorna la lista di tutte le card
    public List<Card> getAllCards(){
        List<Card> result = new ArrayList<>();
        if(toDo!= null)
            result.addAll(toDo);
        if(inProgress != null)
            result.addAll(inProgress);
        if(toBeRevised != null)
            result.addAll(toBeRevised);
        if(done != null)
            result.addAll(done);
        return result;
    }
    //Metodo che restituisce la lista dei membri di un progetto
    public CopyOnWriteArrayList<String> getProjectMembers() {
        return this.projectMembers;
    }


    //Aggiungo un membro al progetto
    public void AddMember(String user) {
        this.projectMembers.addIfAbsent(user);
    }

    //Creazione della cartella relativa al progetto
    public synchronized String createProjectDir(String projectName) throws IOException {
        String path = "src/StoredData/ProjectDir/" + projectName + "/";
        File prj = new File(path);
        File members = new File(path + "projectMembers.json");
        if (prj.mkdir() && members.createNewFile()) {
            return path;
        } else return null;
    }

    //funzione che legge tutte le liste di cards
    public synchronized void readAllLists(){
        Gson gson = new Gson();
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader("src/StoredData/ProjectDir/todoList.json"));
            Type type = new TypeToken<CopyOnWriteArrayList<Card>>() {
            }.getType();
            toDo = gson.fromJson(br, type);
            br = new BufferedReader(new FileReader("src/StoredData/ProjectDir/inProgressList.json"));
            inProgress = gson.fromJson(br, type);
            br = new BufferedReader(new FileReader("src/StoredData/ProjectDir/toBeRevisedList.json"));
            toBeRevised = gson.fromJson(br, type);
            br = new BufferedReader(new FileReader("src/StoredData/ProjectDir/doneList.json"));
            done = gson.fromJson(br, type);
        } catch (FileNotFoundException e) {
        }
    }

    //funzione che scrive tutte le liste di cards
    public synchronized void writeAllLists(String path){
        Writer writer;
        try {
            writer = new FileWriter(path+"/toDoList.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if(toDo != null) {
                gson.toJson(toDo, writer);
            }
            writer.flush();
            writer.close();
            writer = new FileWriter(path+"/inProgressList.json");
            gson = new GsonBuilder().setPrettyPrinting().create();
            if(inProgress != null) {
                gson.toJson(inProgress, writer);
            }
            writer.flush();
            writer.close();
            writer = new FileWriter(path+"/toBeRevisedList.json");
            gson = new GsonBuilder().setPrettyPrinting().create();
            if(toBeRevised != null) {
                gson.toJson(toBeRevised, writer);
            }
            writer.flush();
            writer.close();
            writer = new FileWriter(path+"/doneList.json");
            gson = new GsonBuilder().setPrettyPrinting().create();
            if(done != null) {
                gson.toJson(done, writer);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //metodo per la lettura da file dei membri di progetto
    public synchronized void readProjectMembers(String path) throws IOException {
        BufferedReader reader;
        Gson gson = new Gson();
        try {
            reader = new BufferedReader(new FileReader(path + "/projectMembers.json"));
            Type type = new TypeToken<CopyOnWriteArrayList<String>>() {
            }.getType();
            projectMembers = gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Metodo per leggere la lista toDo
    public synchronized void readToDoL() throws IOException {
        Gson gson = new Gson();
        BufferedReader reader;
        try {
          reader  = new BufferedReader(new FileReader("src/StoredData/ProjectDir/toDoList.json"));
            Type type = new TypeToken<CopyOnWriteArrayList<Card>>() {
            }.getType();
            toDo = gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //metodo per la scrittura su file dei membri di progetto
    public synchronized boolean writeProjectMembers(String path) {
        Writer wr;
        try {
          wr = new FileWriter(path + "/projectMembers.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(projectMembers, wr);
            wr.flush();
            wr.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public synchronized CopyOnWriteArrayList<Card> foundProjCards(String prjName) {
        //Metodo per il controllo dell'esistenza di una card in un progetto
        CopyOnWriteArrayList<Card> cards = new CopyOnWriteArrayList<>();
        File pDir = new File("src/StoredData/ProjectDir/" + prjName);
        File[] files = pDir.listFiles();
        boolean res = false;

        if (files.length > 0) {
            //Scorro i file e memorizzo le card
            for (File file : files) {
                if (file.isFile()) {
                    if(!(file.getName().equals("projectMembers.json"))) {
                        Card c = new Card(file.getName().replace(".json",""));
                        cards.add(c);
                    }

                }
            }
        }
        return cards;
    }

    //Metodo per la scrittura nel file della card
    public synchronized void writeCardFile(String prjName, String cardName, Card card) {
        Writer wr;
        try {
            wr = new FileWriter("src/StoredData/ProjectDir/" + prjName + "/" + cardName + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(card, wr);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo per la scrittura nel file della lista toDo
    public synchronized void writeToDoL() {
        Writer wr;
        try {
            wr = new FileWriter("src/StoredData/ProjectDir/toDoList.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(toDo, wr);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Metodo per la scrittura nel file della lista done
    public synchronized void writeDoneL() {
        Writer wr;
        try {
            wr = new FileWriter("src/StoredData/ProjectDir/doneList.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(done, wr);
            wr.flush();
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //metodo per aggiungere una card
    public boolean addCard(Card c) {
        if(toDo == null)
            this.toDo = new CopyOnWriteArrayList<>();
        return this.toDo.addIfAbsent(c);
    }


    //Metodo per lo spostamento di una card
    public int moveCard(String cardName, String srcList, String destList) {
        CopyOnWriteArrayList<Card> sList;
        CopyOnWriteArrayList<Card> dList;
        //Trovo la lista sorgente della card
        if (srcList.equals("toDo") || srcList.equals("todo") || srcList.equals("TODO")){
            if (this.toDo == null)
                this.toDo = new CopyOnWriteArrayList<>();
            sList = this.toDo;
            srcList = "toDo";
        } else if (srcList.equals("inProgress") || srcList.equals("inprogress") || srcList.equals("INPROGRESS")) {
            if (this.inProgress == null)
                this.inProgress = new CopyOnWriteArrayList<>();
            sList = this.inProgress;
            srcList = "inProgress";
        } else if (srcList.equals("toBeRevised") || srcList.equals("toberevised") || srcList.equals("TOBEREVISED") ) {
            if (this.toBeRevised == null)
                this.toBeRevised = new CopyOnWriteArrayList<>();
            sList = this.toBeRevised;
            srcList = "toBeRevised";
        } else if (srcList.equals("done") || srcList.equals("DONE")) {
            if (this.done == null)
                this.done = new CopyOnWriteArrayList<>();
            sList = this.done;
            srcList = "done";
        } else {
            sList = null;
        }

        //Trovo la lista destinazione della card
        if (destList.equals("toDo") || destList.equals("todo") || destList.equals("TODO")) {
            if (this.toDo == null)
                this.toDo = new CopyOnWriteArrayList<>();
            dList = this.toDo;
            destList = "toDo";
        } else if (destList.equals("inProgress") || destList.equals("inprogress") || destList.equals("INPROGRESS")) {
            if (this.inProgress == null)
                this.inProgress = new CopyOnWriteArrayList<>();
            dList = this.inProgress;
            destList = "inProgress";
        } else if (destList.equals("toBeRevised") || destList.equals("toberevised") || destList.equals("TOBEREVISED")) {
            if (this.toBeRevised == null)
                this.toBeRevised = new CopyOnWriteArrayList<>();
            dList = this.toBeRevised;
            destList = "toBeRevised";
        } else if (destList.equals("done") || destList.equals("DONE") || destList.equals("Done")) {
            if (this.done == null)
                this.done = new CopyOnWriteArrayList<>();
            dList = this.done;
            destList = "done";
        } else {
            dList = null;
        }

        if (sList != null && dList != null) {
            //Controllo che le liste siano diverse se non lo sono non sposto nulla
            if (sList != dList) {
                //Controllo i vincoli di spostamento: todolist->progress, progress->done || revised, revised->progress || done
                boolean constraints = ((srcList.equals("toDo") && destList.equals("inProgress")) || (srcList.equals("inProgress") && (destList.equals("done") || destList.equals("toBeRevised"))) || (srcList.equals("toBeRevised") && (destList.equals("inProgress") || destList.equals("done"))));
                if (!constraints) return -6; //Se la condizione è falsa esco senza spostare, altrimenti continua dopo
                //controllo che la lista src contenga la card da spostare
                Card c = new Card(cardName);
                if (sList.contains(c)) {
                    c = sList.get(sList.indexOf(c));
                    //aggiorno la history della card
                    c.set_History(destList);
                    //sposto la card
                    dList.addIfAbsent(c);
                    //la elimino dalla lista sorgente
                    sList.remove(c);
                    //aggiorno il file delle liste
                    writeAllLists("src/StoredData/ProjectDir");
                    //aggiorno il file della card
                    writeCardFile(this.pName, cardName, c);
                } else {
                    return 0;
                }
            }
            return 7;
        } else {
            return -1;
        }
    }

    //Metodo che controlla l'esistenza di una card
    public boolean existenceCard(String cardName){
        Card c = new Card(cardName);
        List<Card> app = new ArrayList<>();
        if(toDo != null) app.addAll(toDo);
        if(inProgress != null) app.addAll(inProgress);
        if(toBeRevised != null) app.addAll(toBeRevised);
        if(done != null) app.addAll(done);
        if(app != null){
            return app.contains(c);
        }
        return false;
    }

    //Metodo che restituisce tutte le informazioni di una specifica card.
    public String getCardInfo(String cardName){
        Card c = new Card(cardName);
        boolean found = false;
        if(toDo != null && toDo.contains(c)){
            c = toDo.get(toDo.indexOf(c));
            found = true;
        }else if(inProgress != null && inProgress.contains(c)){
            c = inProgress.get(inProgress.indexOf(c));
            found = true;
        }else if(toBeRevised != null && toBeRevised.contains(c)){
            c = toBeRevised.get(toBeRevised.indexOf(c));
            found = true;
        }else if(done != null && done.contains(c)){
            c = done.get(done.indexOf(c));
            found = true;
        }
        if(found){
            return "card name: "+c.getCardName()+", card description: "+c.getCardDescription()+", current list: "+c.getLastElement(c.getCardName());
        }else{
            return null;
        }
    }



    //ottiene il riferimento alla cardname passata se esiste
    //null altrimenti
    public Card getCard(String cardName){
        Card card=new Card(cardName);

        for (Card tmp:toDo){
            if(tmp.equals(card))
                return tmp;
        }
        for (Card tmp:inProgress){
            if(tmp.equals(card))
                return tmp;
        }
        for (Card tmp:toBeRevised){
            if(tmp.equals(card))
                return tmp;
        }
        for (Card tmp:done){
            if(tmp.equals(card))
                return tmp;
        }

        return null;
    }

    //Metodo per la cancellazione della directory del prog e il suo contenuto
    public synchronized boolean deletePrjDir(File dir){
        File[] file = dir.listFiles();
        if(file != null){
            for (File f : file){
                deletePrjDir(f);
            }
        }
        return dir.delete();
    }


}
