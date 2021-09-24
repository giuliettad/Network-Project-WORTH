package Server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static final int TCP_PORT = 8000;
    private static final int RMI_PORT = 6500;
    public static final int MAX_THREAD = 30;

    public static UserDB userData;
    private static CopyOnWriteArrayList<User> users;
    private static CopyOnWriteArrayList<String> projectsList;


    public ServerMain(){

        users = new CopyOnWriteArrayList<>();
        projectsList = new CopyOnWriteArrayList<String>();

    }

    public static void main(String[] args) throws IOException {

        ServerMain s = new ServerMain();
        s.restoreUsers();
        s.restoreProjects();

    //IMPLEMENTAZIONE RMI
        RegistrationImplementation registration;

        try{
            //Creazione istanza dell'oggetto remoto
            registration = new RegistrationImplementation(users);
            //Esportazione dell'oggetto
            RegistrationServer stub = (RegistrationServer) UnicastRemoteObject.exportObject(registration, RMI_PORT);
            //Creazione di un registry sulla porta RMI_PORT
            LocateRegistry.createRegistry(RMI_PORT);
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            //Pubblicazione dello stub nel registry
            registry.rebind("ServerRMI", stub);

            //Pool per la gestione delle connessioni con i client
            ExecutorService exService = Executors.newFixedThreadPool(MAX_THREAD);

            //SERVER LOGIN
            try(ServerSocket serverSocket = new ServerSocket();){
                serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),TCP_PORT));
                System.out.println("Server pronto");
                while (true){
                exService.execute(new CommandHandler(serverSocket.accept(), users, projectsList, registration));}
            }
            finally {
                exService.shutdownNow();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Restore dei progetti
    public void restoreProjects() throws IOException {
        //Creo il file con le 4 liste di appoggio
        File toDoList = new File("src/StoredData/ProjectDir/todoList.json");
        File inProgressList = new File("src/StoredData/ProjectDir/inProgressList.json");
        File toBeRevisedList = new File("src/StoredData/ProjectDir/toBeRevisedList.json");
        File doneList = new File("src/StoredData/ProjectDir/doneList.json");
        File ip = new File("src/StoredData/ProjectDir/ip.json");
        toDoList.createNewFile();
        inProgressList.createNewFile();
        toBeRevisedList.createNewFile();
        doneList.createNewFile();
        ip.createNewFile();

        File prjdir = new File("src/StoredData/ProjectDir");
        File[] files = prjdir.listFiles();

        //Scorro i file della ProjectDir
        if(files != null && files.length > 0){
            for(File file : files){
                if(file.isDirectory()){
                    projectsList.add(file.getName());
                    System.out.println("Stored project: " + file.getName());
                    String[] f = file.list();
                    if(f !=null && f.length >0){
                        for (String c : f){
                            if(!c.contains("Members")) {
                                System.out.println("Stored card in " + file.getName() + " project: " + c);
                            }
                        }
                    }else{
                        System.out.println("No cards stored in " + file.getName());
                    }

                }
            }
        }else{
            System.out.println("No project stored");
        }

    }

    //Restore degli utenti
    public void restoreUsers(){
        userData = new UserDB();
        //Restore degli utenti
        File prjdir = new File("src/StoredData");
        File[] files = prjdir.listFiles();

        if(files != null && files.length > 0){
            for(File file : files) {
                //Vedo se il file degli utenti esiste
                if (!file.isDirectory()) {
                    if (file.getName().equals("dataUsers.json")) {
                        try {
                            //Leggo il file dataUsers.jason e memorizzo i dati in users
                            users = userData.readFromFile();
                            for (User user : users) {
                                System.out.println("Stored users: " + user.getNickName());
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }else {
                        System.out.println("No users stored");
                    }
                }
            }
        }
    }


}
