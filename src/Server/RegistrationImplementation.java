package Server;

import Client.NotifyClient;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegistrationImplementation extends RemoteServer implements RegistrationServer {
    //Lista degli utenti registrati
    CopyOnWriteArrayList<User> userList;
    UserDB userDB = new UserDB();
    //Struttura contenente gli utenti con il loro stato
    ConcurrentHashMap<String,String> usersStates;
    //Client da avvisare con la callback
    ConcurrentHashMap<String, NotifyClient> clientsToNotify;


    public RegistrationImplementation(CopyOnWriteArrayList<User> u) {
        super();
        this.userList = u;
        this.usersStates = new ConcurrentHashMap<>();
        if(!userList.isEmpty()) {
        /*Prendo i nickname degli utenti che ho recuperato dal file all'avvio del server con la restore
        e i relativi stati*/
            for (User usr : userList) {
                this.usersStates.put(usr.getNickName(), usr.getIsOnline());
            }
        }else{
            this.usersStates = new ConcurrentHashMap<>();
        }
        this.clientsToNotify = new ConcurrentHashMap<>();
    }

    @Override
    public boolean register(String username, String password) throws RemoteException {
        boolean registrationState = false;
        User user = new User();
        user.setNickName(username);
        user.setPassword(password);
         //Inserisco l'utente nella struttura e nel file se non presente
        if(userList == null)
            userList = new CopyOnWriteArrayList<>();

        registrationState = userList.addIfAbsent(user);

        userDB.writeFile(userList);

        return registrationState;
    }

    public synchronized ConcurrentHashMap<String, String> registerForCallback(NotifyClient client, String user) throws RemoteException{
        if(!this.clientsToNotify.containsKey(user)){
            //registro un nuovo client da avviasare con la callback
            this.clientsToNotify.put(user,client);
        }
        return this.usersStates;
    }


    public synchronized void unregisterForCallback(String user) throws RemoteException{
        //rimuovo un client da avvisare con la callback
        this.clientsToNotify.remove(user);
    }

    public synchronized void notifyUserStateUpdate(String username, String state, ConcurrentHashMap usersStates){
        Collection<NotifyClient> values = this.clientsToNotify.values();
        Iterator<NotifyClient> iterator = values.iterator();
        NotifyClient client;
        while (iterator.hasNext()){
            client = iterator.next();
            try{
              client.notifyUserStateUpdate(username, state, usersStates);
            //Se entro nel catch vuol dire che ho perso la connessione con il client da notificare
            }catch (RemoteException r){
                //quindi lo rimuovo dalla lista di client da avvisare
                iterator.remove();
            }
        }
    }

    //Metodo che mi permette di segnalare ai client di aggiornare la lista degli utenti registrati
    //a seguito di una nuova registrazione
    public synchronized void notifyRegistratio(String username){
        this.usersStates.put(username, "offline");
        Collection<NotifyClient> values = this.clientsToNotify.values();
        Iterator<NotifyClient> iterator = values.iterator();
        NotifyClient client;
        while(iterator.hasNext()){
            client = iterator.next();
            try{
                client.notifyRegistration(username, "offline", usersStates);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void logIn(String username) throws RemoteException{
        this.usersStates.replace(username, "online");
        this.notifyUserStateUpdate(username, "online", usersStates);
    }

    public synchronized void logOut(String username) throws RemoteException{
        this.usersStates.replace(username, "offline");
        this.notifyUserStateUpdate(username, "offline", usersStates);
    }

}
