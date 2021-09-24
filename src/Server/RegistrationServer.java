package Server;

import Client.NotifyClient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public interface RegistrationServer extends Remote {
    boolean register(String username, String password) throws RemoteException;

    ConcurrentHashMap<String, String> registerForCallback(NotifyClient client, String user) throws RemoteException;
    void unregisterForCallback(String user) throws RemoteException;

}
