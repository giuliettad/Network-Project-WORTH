package Server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

//Classe contentente i metodi per la gestione della persistenza dei dati utente
public class UserDB {
    CopyOnWriteArrayList<User> userData; //struttura contenente gli utenti
    public UserDB(){
        userData = new CopyOnWriteArrayList<>();
    }

    //Metodo per la lettura dal file degli utenti
    public synchronized void readFile() {
        Gson gson = new Gson();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader("src/StoredData/dataUsers.json"));
            Type type = new TypeToken<CopyOnWriteArrayList<User>>() {
            }.getType();
            userData = gson.fromJson(reader, type);
        }catch(FileNotFoundException e){
                e.printStackTrace();
        }
    }

    //Metodo per la scrittura nel file dalla struttura
    public synchronized void writeFile(CopyOnWriteArrayList<User> userList){
        Writer wr;
        try {
            wr = new FileWriter("src/StoredData/dataUsers.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            //gson.toJson(userList, wr);
            wr.write(gson.toJson(userList));
            wr.flush();
            wr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Metodo che restituisce un utente
    //null altrimenti
    public User getUser(User u){
        if(this.userData!=null && userData.size()>0)
            return userData.get(userData.indexOf(u));
        return null;
    }

    //metodo che restituisce una stringa con la lista dei progetti dell'utente
    public String getUserProjectList(User usr){
        CopyOnWriteArrayList<String> lista = getUser(usr).getProjectList();
        String res = "";
        for(String s : lista){
            res += s + " ";
        }
        return res;
    }

    //Metodo per la lettura dal file
    //restituisce la struttura contenente gli utenti
    public synchronized CopyOnWriteArrayList readFromFile() {
        Gson gson = new Gson();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(("src/StoredData/dataUsers.json")));
            Type type = new TypeToken<CopyOnWriteArrayList<User>>() {
            }.getType();
            userData = gson.fromJson(reader, type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return userData;
    }

    public void setUserisOnline(User user, String isOnline){
        userData.get(userData.indexOf(user)).setIsOnline(isOnline);
    }

    //Metodo per aggiungere un progetto a un utente
    public boolean addProject(User u, String projectName){
        if(!userData.contains(u)) return false;
        User usr = userData.get(userData.indexOf(u));
        if(usr.getProjectList() == null){
            usr.setProjectList(new CopyOnWriteArrayList<>());
        }
        boolean result = usr.getProjectList().addIfAbsent(projectName);
        userData.remove(usr);
        userData.addIfAbsent(usr);
        writeFile(userData);
        return  result;
    }

    //Metodo per prendere i membri di un progetto
    public String getPrjMembers(String prjName) throws IOException {
        //tolgo l'ip per avere solo il nome del progetto
        String[] tmp = prjName.split("@");
        CopyOnWriteArrayList<String> prjMembers;
        String path = "src/StoredData/ProjectDir/" + tmp[0];
        Project p = new Project(prjName);
        p.readProjectMembers(path);
        prjMembers = p.getProjectMembers();
        return prjMembers.toString();

    }

    //metodo che controlla la presenza di un progetto nella lista progetti dell'utente
    public boolean isMember(String projName, User usr){
        CopyOnWriteArrayList<String> lista = usr.getProjectList();
        ArrayList<String> lstAux = new ArrayList<>();
        //cosrtuisco una lista di appoggio contenente solo i nomi progetto
        for(String pStr : lista){
            String str = pStr.split("@")[0];
            lstAux.add(str);
        }
        return lstAux.contains(projName);
    }

    //metodo che cerca un utente nella struttura dati
    public boolean foundUser(User usr){
        if(this.userData != null)
            return this.userData.contains(usr);
        else return false;
    }


    //Metodo per la rimozione di un progetto da un utente
    public void deleteProject(User u, String prjName){
        User tmp = userData.get(userData.indexOf(u));
        if(tmp.getProjectList() == null){
            tmp.setProjectList(new CopyOnWriteArrayList<>());
        }
        tmp.getProjectList().remove(prjName);
        //Cancello il progetto anche dagli altri membri
        for (User us : userData){
            us.getProjectList().remove(prjName);
        }
        //Aggiorno il file
        userData.remove(tmp);
        userData.addIfAbsent(tmp);
        writeFile(userData);
    }

    //metodo che aggiunge un utente alla lista utenti di un progetto
    public boolean addMemberL(String projName, User usrDaAggiungere, User usrCheAggiunge) throws IOException {
        CopyOnWriteArrayList<String> list;
        String fullPath ="src/StoredData/ProjectDir/" + projName;
        Project p = new Project(projName);
        p.readProjectMembers(fullPath);
        list = p.getProjectMembers();
        boolean res;
        res = list.addIfAbsent(usrDaAggiungere.getNickName());
        p.writeProjectMembers(fullPath);
        // Devo ora scriverlo anche nel file classico, nella lista progetti globale
        readFile();
        String ip = "NotExists";
        //scorro la lista dei progetti dell'utente che vuole aggiungere un membro
        //System.out.println("stampa array "+userData.get(userData.indexOf(usrCheAggiunge)).getProjectList().size());
        for(String temp : userData.get(userData.indexOf(usrCheAggiunge)).getProjectList()){
            //prendo l'ip del progetto da aggiungere
            if(temp.split("@")[0].equals(projName)){
                ip = temp.split("@")[1];
                break;
            }
        }
        //aggiugo il progetto
        addProject(usrDaAggiungere, projName+"@"+ip);
        return res;
    }
}
