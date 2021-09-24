package Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

//Interfaccia per la callback
public interface NotifyClient extends Remote {
    void notifyRegistration(String username, String state, ConcurrentHashMap usersStates) throws RemoteException;
    void notifyUserStateUpdate(String user, String state, ConcurrentHashMap list) throws RemoteException;
    //void notifyUnregistration(String username) throws RemoteException;

    //Metodo per comunicare l'ip multicast del progetto a cui l'utente è stato aggiunto
    public void notifyaddToProject(String ip) throws RemoteException;

    //Metodo per notificare agli utenti che un progetto del quale fanno parte è stato cancellato
    public void notifyCancelProject(String ip) throws RemoteException;
}
