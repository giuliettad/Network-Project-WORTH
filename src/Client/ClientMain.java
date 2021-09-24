package Client;

import Server.RegistrationServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClientMain {

    private static final int TCP_PORT = 8000;
    private static final int RMI_PORT = 6500;
    public static boolean logged;
    //Variabili per la risposta del server
    private static boolean reply;
    private static String sReply;

    private static String username;
    //associazione tra ipMulticast e ThreadChat
    private static ConcurrentHashMap<String, Chat> IPChat;


    //Array di appoggio
    private static String[] aux;


    private static RegistrationServer serverSub;

    public ClientMain(){ logged = false; }



    public static void main(String[] args) throws IOException {

        String userInput = "";

        //Ottengo il riferimento del server remoto
        Remote remoteObj;
        try{
            Registry registry = LocateRegistry.getRegistry(RMI_PORT);
            remoteObj = registry.lookup("ServerRMI");
            serverSub = (RegistrationServer)  remoteObj;




        } catch (NotBoundException e) {
            System.out.println("BIND PORT EXCEPTION");
            e.printStackTrace();
        }

        Socket socket = new Socket();
        IPChat = new ConcurrentHashMap<>();
        NotifyClientImpl callbackObj = new NotifyClientImpl(IPChat);
        NotifyClient stub = (NotifyClient) UnicastRemoteObject.exportObject(callbackObj, 0);

        System.out.println("Connessione con il server...");
        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {


            // TCP Connection
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), TCP_PORT));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(),true);



            System.out.printf("WELCOME in WORTH platform\n Per procedere eseguire resistrazione o login\n");

            while (!userInput.equals("exit")){
                try {
                    userInput = input.readLine();
                    //Nella 1° posizione di arguments avrò il comando
                    String[] arguments = userInput.split(" ", 2);

                    //Se l'utente preme invio senza digitare comandi esco
                    if(userInput == null) return;

                    switch (arguments[0]){
                        case "exit":
                            System.out.println("Uscita in corso...");
                            writer.println("exit");
                            sReply = reader.readLine();
                            clientServerProtocol( "exit", username, "");
                            System.exit(0);
                        case "register":
                            if(!logged){
                                aux = arguments[1].split(" ", 2);
                                if(aux.length == 2){
                                    if(aux[0] == null || aux[1] == null) throw new NullPointerException();
                                    if(aux[0].equals("") || aux[1].equals("")) throw new IllegalArgumentException();

                                    //Registrazione effettuata con RMI
                                    reply = serverSub.register(aux[0], aux[1]);

                                    aux = arguments[1].split(" ", 2);
                                    //Comunico al server che deve notificare ai client che un nuovo utente si è registrato
                                    writer.println("reg "+ aux[0]);

                                    if (reply == true)
                                        System.out.println("Registration successful");
                                    else {
                                        System.out.println("Registration failed. User " + aux[0] + " alredy registered.");
                                        System.out.println("Please try with another username.");
                                    }
                                }

                            }
                            break;
                        case "login":
                            if(arguments.length == 2){
                                //Ho "user password" ottengo "user", "password"
                                aux = arguments[1].split(" ", 2);
                                if(aux.length == 2){
                                    //invio al server i dati di login
                                    writer.println("login "+ aux[0] +" "+ aux[1]);
                                    //leggo messaggio del server
                                    sReply = reader.readLine();
                                    //Interpreto il messaggio del server
                                    clientServerProtocol( "login", aux[0], " ");
                                    //Mi registro al servizio di callback
                                    callbackObj.setUsers(serverSub.registerForCallback(callbackObj, aux[0]));

                                }else {
                                    System.out.println("SYNTAX ERROR. Please try with login [username] [password].");
                                }
                            }else {
                                System.out.println("SYNTAX ERROR. Please try with login [username] [password].");
                            }
                            break;
                        case "logout":
                            if(arguments.length == 2){
                                username = arguments[1];
                                if(username == " ") throw new NullPointerException();
                                writer.println("logout " + username);
                                sReply = reader.readLine();
                                clientServerProtocol( "logout", username, "");
                                if(sReply.contains("200")) {
                                    serverSub.unregisterForCallback(username);
                                    logged = false;
                                }
                            }else{
                                System.out.println("SYNTAX ERROR. Please try with logout [username]");
                            }
                            break;
                        case "createProject":
                            if(arguments.length == 2){
                                String prjName = arguments[1];
                                //Controllo che il nome del progetto non sia vuoto e non contenga spazi
                                if(!prjName.equals("") && !prjName.contains(" ")){
                                    writer.println("createProject " + prjName);
                                    sReply = reader.readLine();
                                    //System.out.println(sReply);
                                    String[] aux = sReply.split("#");
                                    sReply = aux[0];
                                    if(aux[0].contains("200")){
                                        callbackObj.notifyaddToProject(aux[1]);
                                        //IPChat = callbackObj.getIPChat();
                                    }
                                    clientServerProtocol( "createProject", prjName, "");
                                }else {
                                    System.out.println("Attention project name cannot have spaces.");
                                }
                            }else {
                                System.out.println("SYNTAX ERROR. Please try with createProject [projectName]");

                            }
                            break;
                        case "addMember":
                            if (arguments.length == 2){
                                String[] aux = arguments[1].split(" ", 2);
                                if(aux.length == 2){
                                    String prjName = aux[0];
                                    String usrName = aux[1];
                                    writer.println("addMember " + prjName + " " + usrName );
                                    sReply = reader.readLine();
                                    String[] au = sReply.split("@");
                                    sReply = au[0];
                                    clientServerProtocol("addMember", prjName, usrName);
                                    if(sReply.contains("200")){
                                        //System.out.println(au[1]);
                                        callbackObj.notifyaddToProject(au[1]);
                                    }
                                }else{
                                    System.out.println("SYNTAX ERROR. Please try with createProject addMember [projectName] [username]");
                                }
                            }else{
                                System.out.println("SYNTAX ERROR. Please try with createProject addMember [projectName] [username]");
                            }
                            break;
                        case "showMembers":
                            if(arguments.length == 2){
                                writer.println("showMembers " + arguments[1]);
                                sReply = reader.readLine();
                                System.out.println(sReply);
                            } else {
                                System.out.println("SYNTAX ERROR. Type showMembers [projectName]" );
                            }
                            break;
                        case "addCard":
                            if(arguments.length == 2){
                                String[] arg = arguments[1].split(" ", 3);
                                if(arg.length == 3){
                                    writer.println("addCard " + arg[0]+ " " + arg[1] + " " + arg[2]);
                                    sReply = reader.readLine();
                                    clientServerProtocol("addCard", arg[1], arg[0]);
                                }else {
                                    System.out.println("SYNTAX ERROR. Type addMember [projectName] [cardName] [description]" );
                                }
                            }else {
                                System.out.println("SYNTAX ERROR. Type addMember [projectName] [cardName] [description]" );
                            }
                            break;
                        case "listUsers":
                            if(arguments.length == 1){
                                System.out.println(callbackObj.listUsers());
                            }else {
                                System.out.println("SYNTAX ERROR. Type listUsers" );
                            }
                            break;
                        case "listOnlineUsers":
                            if(arguments.length == 1){
                                CopyOnWriteArrayList<String> aux;
                                aux = callbackObj.listOnlineUsrs();
                                if(aux.isEmpty()){
                                    System.out.println("Error you must do login before use listOnlineUsers");
                                }else{
                                    System.out.println(aux);
                                }

                            }else {
                                System.out.println("SYNTAX ERROR. Type listOnlineUsers" );
                            }
                            break;
                        case "moveCard":
                            if(arguments.length == 2){
                                String[] arg = arguments[1].split(" ", 4);
                                if(arg.length == 4){
                                    writer.println("moveCard " + arg[0]+ " " + arg[1] + " " + arg[2]+ " " + arg[3]);
                                    sReply = reader.readLine();
                                    clientServerProtocol( "moveCard",arg[1],arg[0]);
                                }else {
                                    System.out.println("SYNTAX ERROR. Type moveCard [projectName] [cardName] [sourceList] [destList]" );
                                }
                            }else {
                                System.out.println("SYNTAX ERROR. Type moveCard [projectName] [cardName] [sourceList] [destList]" );
                            }
                            break;
                        case "listProjects":
                            if (arguments.length == 1){
                                writer.println("listProjects");
                                sReply = reader.readLine();
                                System.out.println(sReply);
                            }else {
                                System.out.println("SYNTAX ERROR. [listProjects]" );
                            }
                            break;
                        case "showCard":
                            if(arguments.length == 2){
                                String[] arg = arguments[1].split(" ", 2);
                                if(arg.length == 2){
                                    writer.println("showCard " + arg[0] +" "+ arg[1]);
                                    sReply = reader.readLine();
                                    System.out.println(sReply);
                                }else{
                                    System.out.println("SYNTAX ERROR. showCard [projectName] [cardName]" );
                                }
                            }else{
                                System.out.println("SYNTAX ERROR. showCard [projectName] [cardName]" );
                            }
                            break;
                        case "showCards":
                            if(arguments.length == 2){
                                    writer.println("showCards " + arguments[1]);
                                    sReply = reader.readLine();
                                    System.out.println(sReply);
                            }else{
                                System.out.println("SYNTAX ERROR. showCards [projectName]" );
                            }
                            break;
                        case "getCardHistory":
                            if(arguments.length == 2){
                                String[] arg = arguments[1].split(" ", 2);
                                if(arg.length == 2){
                                    writer.println("getCardHistory " + arg[0] +" "+ arg[1]);
                                    sReply = reader.readLine();
                                    System.out.println(sReply);
                                }else{
                                    System.out.println("SYNTAX ERROR. getCardHistory [projectName] [cardName]" );
                                }
                            }else{
                                System.out.println("SYNTAX ERROR. getCardHistory [projectName] [cardName]" );
                            }
                            break;
                        case "cancelProject":
                            if(arguments.length == 2){
                                writer.println("cancelProject " + arguments[1]);
                                sReply = reader.readLine();
                                String[] aux = sReply.split("@");
                                String ip = aux[1];
                                if(sReply.contains("200")){
                                    callbackObj.notifyCancelProject(ip);
                                }
                                sReply=aux[0];
                                clientServerProtocol( "cancelProject",arguments[1], "");
                            }else{
                                System.out.println("SYNTAX ERROR. cancelProject [projectName]" );
                            }
                            break;

                        case "readChat":
                            if(arguments.length == 2){
                                writer.println("readChat "+ arguments[1]);
                                sReply = reader.readLine();
                                //System.out.println(sReply);
                                IPChat = callbackObj.getIPChat();
                                readMSG(sReply);
                            }else{
                                System.out.println("SYNTAX ERROR. readChat [projectName]" );
                            }
                            break;

                        case "sendChatMsg":
                            if(arguments.length == 2){
                                String[] arg = arguments[1].split(" ", 2);
                                writer.println("sendChatMsg "+ arg[0]);
                                sReply = reader.readLine();
                                //System.out.println(sReply);
                                if(!sReply.equals("")) {
                                    //Associo gli ip creati da altri utenti o in sessioni precedenti
                                    callbackObj.notifyaddToProject(sReply);
                                    IPChat = callbackObj.getIPChat();

                                    //System.out.println(IPChat.get(sReply));
                                    IPChat.get(sReply).sendMsg(arg[1]);
                                }
                                else{
                                    System.out.println("ERROR! project not existing" );
                                }

                            }else{
                                System.out.println("SYNTAX ERROR. sendChatMsg [projectName] [message]" );
                            }
                            break;
                        case "help":
                            System.out.println("I comandi disponibili sono:");
                            System.out.println("-->'register nomeUtente Password'                                  :    Registra un utente");
                            System.out.println("-->'login nomeUtente Password'                                     :    Logga utente");
                            System.out.println("-->'logout'                                                        :    Deautentica utente loggato.");
                            System.out.println("-->'listUsers'                                                     :    Restiruisce la lista degli utenti registrati a WORTH.");
                            System.out.println("-->'listOnlineUsers'                                               :    Resttiruisce la lista degli utenti di WORTH online in questo momento.");
                            System.out.println("-->'listProjects'                                                  :    Restituisce la lista dei progetti di cui l’utente è membro.");
                            System.out.println("-->'createProject nomeProgetto'                                    :    Crea un nuovo progetto.");
                            System.out.println("-->'addMember nomeProgetto nomeUtente'                             :    Aggiunge un utente ad un progetto.");
                            System.out.println("-->'showMembers nomeProgetto'                                      :    Restituisce la lista dei membri di un progetto.");
                            System.out.println("-->'showCards nomeProgetto'                                        :    Restituisce la lista delle card di un progetto.");
                            System.out.println("-->'showCard nomeProgetto nomeCard'                                :    Recupera informazioni della una card di un progetto.");
                            System.out.println("-->'addCard nomeProgetto nomeCard descrizioneCard'                 :    Aggiunge una card ad un progetto.");
                            System.out.println("-->'moveCard nomeProgetto nomeCard listaPartenza listaDestinazione':    Sposta una card di un progetto da una lista ad un'altra. Liste: toDo, inProgress, toBeRevised, done");
                            System.out.println("-->'getCardHistory nomeProgetto nomeCard'                          :    Restituisce la sequenza di eventi di spostamento di una card.");
                            System.out.println("-->'readChat nomeProgetto'                                         :    Legge i messaggi(non ancora letti) della chat di un progetto.");
                            System.out.println("-->'sendChatMsg nomeProgetto messaggio'                            :    Invia un messaggio passato come parametro a tutti i membri di un progetto.");
                            System.out.println("-->'cancelProject nomeProgetto'                                    :    Cancella un progetto da WORTH solo se tutte le card sono nella lista DONE.");
                            System.out.println("-->'exit'                                                          :    Termina esecuzione");
                            System.out.println();
                            break;
                        default:
                            System.out.println("Attention! wrong command. Write 'help' if you need.");

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }


        }

    }



    //Metodo per l'interpretazione dei messaggi del server
    private static void clientServerProtocol(String op, String var, String var2) throws IOException {
        switch (sReply){
            case "200": System.out.println(op + " operation successfully for " + var);
                break;
            case "400": System.out.println("ATTENTION! " + var + " already exists!");
                break;
            case "401": System.out.println("ATTENTION! Wrong password.");
                break;
            case "402": System.out.println("Attention! You don't have access to this project.");
                break;
            case "404": System.out.println("ATTENTION! "+ var + " not found.");
                break;
            case "405": System.out.println("ERROR! You must login before " + op);
                break;
            case "406":System.out.println("Attention! " + op + " not reached! Checking carefully lists");
                break;
            case "408": System.out.println("Attention! " + op + " not reached! Checking carefully source list!");
            break;
            case "410": System.out.println("SYNTAX ERROR. Please try with "+ op +" " +"["+ var+"]");
                break;
            case "411": System.out.println("SYNTAX ERROR. Please try with "+ op +" " +"["+ var+"] "+"["+var2+"]");
                break;
            case "412":System.out.println("SYNTAX ERROR. Type "+ op +" [projectName] [cardName] [description]");
                break;
            case "413":System.out.println("The user is already in" + var + " project");
                break;
            case "414":System.out.println("You can't cancel this project until all its cards are not in doneList!");
                break;
            case "505": System.out.println("Username "+ var + " doesn't exists! Please registered.");
                break;
            case "506": System.out.println("An error occurred updating users file.");
                break;
            case "507": System.out.println("An error occurred writing user file.");
                break;

        }
    }

    // Metodo statico per leggere i messaggi da una chat
    public static void readMSG(String ip) {
        ArrayList<String> str = IPChat.get(ip).readMsg();
        System.out.println("---------------");
        for (String string : str) {
            System.out.println(string);
        }
        System.out.println("---------------");
    }



}
