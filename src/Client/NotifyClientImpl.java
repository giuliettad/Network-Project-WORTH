package Client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.RemoteException;
import java.rmi.server.RemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotifyClientImpl extends RemoteObject implements NotifyClient {
    //private CopyOnWriteArrayList<User> uList;
    private ConcurrentHashMap<String,String> users;
    //Associazione ipMulticast e chat (Thread che gestisce la chat del progetto con ipmulticast)
    ConcurrentHashMap<String, Chat> IPChat;

    public NotifyClientImpl(ConcurrentHashMap<String,Chat> IPChat){
        super();
        //Struttura locale del client per memorizzare gli utenti del servizio
        //this.uList = new CopyOnWriteArrayList<>();
        this.users = new ConcurrentHashMap<>();
        this.IPChat = IPChat;

    }

    //a seguito di una nuova registrazione, aggiorno la lista degli utenti registrati
    public void notifyRegistration(String username, String state, ConcurrentHashMap l) throws RemoteException{
        this.users.clear();
        this.users.putAll(l);
        users.putIfAbsent(username, state);
        //notifica avvenuta nuova registrazione
        /* DEBUG PRINT
        Iterator it = users.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry entry = (Map.Entry) it.next();
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("NOTIFICA UTENTE REGISTRATO " + username + ": " + state);*/
    }

    //notifico il cambio di stato(online->offline/offline->online) di un utente
    public void notifyUserStateUpdate(String username, String state, ConcurrentHashMap list) throws RemoteException{
        this.users.clear();
        this.users.putAll(list);
        //System.out.println(this.users.toString());
        if(users.containsKey(username)){
            users.replace(username, state);
        }
        else{
            users.putIfAbsent(username, state);
        }
    }


    public void setUsers(ConcurrentHashMap<String,String> ServerUsers){
        this.users.clear();
        this.users.putAll(ServerUsers);
    }

    public String listUsers(){
        Set<Map.Entry<String,String>> set = this.users.entrySet();
        ArrayList<Map.Entry<String,String>> array = new ArrayList<>(set);
        //Se la lista è vuota significa che l'utente si è registrato ma
        // non si è loggato, quindi non può usare listUsers
        if(array.isEmpty()) return "Error you must do login before use listUsers";
        return array.toString();
    }

    public synchronized CopyOnWriteArrayList<String> listOnlineUsrs(){
        Set<Map.Entry<String,String>> set = this.users.entrySet();
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        Iterator<Map.Entry<String,String>> it = set.iterator();
        Map.Entry<String,String> entry;
        while(it.hasNext()){
            entry = it.next();
            if(entry.getValue().equals("online")){
                list.addIfAbsent(entry.getKey());
            }
        }
        return list;
    }

    //crea il thread per gestire la chat del progetto identificato dal gruppo muticast passato come parametro
    public void notifyaddToProject(String ip) throws RemoteException{
        try {
            MulticastSocket socket = new MulticastSocket(5555);
            Chat ct = new Chat(socket, InetAddress.getByName(ip),5555);
            this.IPChat.putIfAbsent(ip, ct);

            ct.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, Chat> getIPChat(){return  this.IPChat;}

    //notifica la cancellazione di un progetto
    public void notifyCancelProject(String ip) throws RemoteException{
        //interrompi thread che gestisce la chat
        IPChat.get(ip).interrupt();
        //rimuovi ip multicast chat
        IPChat.remove(ip);
    }

}
