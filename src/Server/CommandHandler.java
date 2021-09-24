package Server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandHandler implements Runnable {
    private Socket clientSocket;
    private boolean logged;
    private CopyOnWriteArrayList<User> userList;
    private User user = new User();
    UserDB userDB = new UserDB();
    RegistrationImplementation regImpl;

    private CopyOnWriteArrayList<String> projects;
    //riferimento alla lista progetti

    int indexFound;


    public CommandHandler(Socket s, CopyOnWriteArrayList<User> users, CopyOnWriteArrayList<String> projectsList, RegistrationImplementation registration) {
   // public CommandHandler(Socket s, CopyOnWriteArrayList<User> users, HashMap<String,Project> projectsList, RegistrationImplementation registration) {
        this.logged = false;
        this.clientSocket = s;
        this.userList = users;
        this.projects = projectsList;
        this.regImpl = registration;
    }


    @Override
    public void run() {

        while(true) {
            try{

                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                String request = inFromClient.readLine();
                if(request == null){
                    return;
                }

                if(request.equals(" ")) throw new NullPointerException();

                String[] arguments = request.split(" ", 2);
                try{
                    switch (arguments[0]){
                        //Mi serve per aggiornare la struttura dagli utenti quando avviene una nuova registrazione
                        case "reg":
                            String[] a = arguments[1].split(" ", 2);
                            regImpl.notifyRegistratio(a[0]);
                            break;
                        case "login":
                            if(arguments.length == 2){
                                if(!logged){
                                    String[] aux = arguments[1].split(" ", 2);
                                    String pssw = aux[1];
                                    String usrn = aux[0];
                                    //Creazione utente
                                    user.setNickName(usrn);
                                    user.setPassword(pssw);
                                    //Controllo che sia presente nel file tramite la struttura di appogggio
                                    if(userList.contains(user)){
                                        int indexFound = userList.indexOf(user);
                                        User foundUser =(User) userList.get(indexFound);
                                        if(foundUser.checkPassword(user.getPassword())){
                                            //Controllo che non sia già online
                                            if(foundUser.getIsOnline().equals("online")){
                                                outToClient.println("400");
                                            }else{
                                                //Login avvenuto correttamente, aggiorno il file
                                                logged = true;
                                                //aggiorno lo stato dell'utente nella lista e poi aggiorno il file
                                                indexFound = userList.indexOf(user);
                                                foundUser = (User) userList.get(indexFound);
                                                foundUser.setIsOnline("online");

                                                regImpl.logIn(usrn);

                                                userDB.writeFile(userList);
                                                //Successful login
                                                outToClient.println("200");
                                            }
                                        }else{
                                            //Wrong password
                                            outToClient.println("401");
                                        }
                                    }else{
                                        //Username doesn't exists
                                        outToClient.println("505");
                                    }
                                }else {
                                    //User already logged in
                                    outToClient.println("400");
                                }
                            }else{
                                //SYNTAX ERROR
                                outToClient.println("410");
                            }
                            break;

                        case "exit":
                            //Faccio prima il logout dell'utente e poi esco
                                if(logged){
                                    if(userList.contains(user)){
                                        indexFound = userList.indexOf(user);
                                        User foundUser =(User) userList.get(indexFound);
                                        if(user.getNickName().equals(foundUser.getNickName())){
                                            foundUser.setIsOnline("Offline");
                                            regImpl.logOut(user.getNickName());
                                            logged = false;
                                            userDB.writeFile(userList);
                                            //Successful
                                            outToClient.println("200");
                                        }else{
                                            //Successful
                                            outToClient.println("200");
                                        }
                                    }else{
                                        //Username not found
                                        outToClient.println("404");
                                    }
                                }else{
                                    //Not logged
                                    outToClient.println("405");
                                }
                            return;

                        case "logout":
                            if(arguments.length == 2){
                                if(logged){
                                    String username = arguments[1];
                                    User user1 = new User();
                                    user1.setNickName(username);

                                    if(userList.contains(user1)){
                                        indexFound = userList.indexOf(user);
                                        User foundUser =(User) userList.get(indexFound);
                                        if(user1.getNickName().equals(foundUser.getNickName())){
                                            foundUser.setIsOnline("Offline");
                                            regImpl.logOut(username);
                                            logged = false;
                                            userDB.writeFile(userList);
                                            //Successful
                                            outToClient.println("200");
                                        }else{
                                            outToClient.println("405");
                                        }
                                    }else{
                                        //Username not found
                                        outToClient.println("404");
                                    }
                                }else{
                                    //Not logged
                                    outToClient.println("405");
                                }
                            }else{
                                //SYNTAX ERROR
                                outToClient.println("410");
                            }
                            break;

                        case "createProject":
                            if(arguments.length == 2){
                                if(logged){
                                        String prjName = arguments[1];
                                        Project p = new Project(user.getNickName(), prjName);
                                        if (!(projects.contains(p))) {
                                            //Creo la cartella del nuovo progetto e al suo interno il file dei membri
                                            String directory = p.createProjectDir(prjName);
                                            //Aggiungo il progetto all'array dei progetti
                                            projects.addIfAbsent(p.getProjectName()+"@"+p.getIp());
                                            //Aggiungo il creatore del progetto come membro progetto
                                            p.AddMember(user.getNickName());
                                            //Aggiungo nel file dei progetti il cretore progetto come membro
                                            boolean check = p.writeProjectMembers(directory);
                                            if (check) {
                                                user.setIsOnline("online");
                                                userDB.readFile();
                                                //Aggiungo all'untente il progetto nella lista progetti
                                                if (user.getProjectList().add(prjName)) {
                                                    userDB.readFromFile();
                                                    userDB.addProject(user, prjName+"@"+p.getIp());
                                                    //Successfully created
                                                    outToClient.println("200#"+ p.getIp());
                                                } else {//An error occurred updating users file
                                                    outToClient.println("506#");
                                                }

                                            } else {
                                                //An error occurred writing user file
                                                outToClient.println("507#");
                                            }

                                        } else { //Progetto già esistente con quel nome
                                            //Project already exists
                                            outToClient.println("400#");
                                        }

                                }else{
                                    //Non sono ancora loogato
                                    outToClient.println("405#");
                                }

                            }else{
                                //SYNTAX ERROR
                                outToClient.println("410#");
                            }
                            break;
                        case "addMember":
                            if(arguments.length == 2){
                                if(logged){
                                    String[] aux = arguments[1].split(" ", 2);
                                    if(aux.length == 2){
                                        userDB.readFile();
                                        if(userDB.isMember(aux[0],userDB.getUser(user))){
                                            User usr = new User();
                                            usr.setNickName(aux[1]);
                                            //Controllo che l'utente da aggiungere sia registrato
                                            if(userDB.foundUser(usr)){
                                                if(userDB.addMemberL(aux[0], usr, user)){
                                                    outToClient.println("200@"+userDB.getUser(user).getIpFromPrj(aux[0]));
                                                }else{
                                                    //already in project
                                                    outToClient.println("413");
                                                }

                                            }else{
                                                //user not exists
                                                outToClient.println("505");
                                            }
                                        }else{
                                            //L'utente non ha accesso al progetto al quale vuole accedere
                                            outToClient.println("402");
                                        }
                                    }else{
                                        //SYNTAX ERROR
                                        outToClient.println("411");
                                    }
                                }else{
                                    //Non sono ancora loogato
                                    outToClient.println("405");
                                }
                            }else{
                                //SYNTAX ERROR
                                outToClient.println("410");
                            }
                            break;

                        case "showMembers":
                            if(arguments.length == 2){
                                if(logged) {
                                    userDB.readFile();
                                    //Controllo che il progetto sia memorizzato nella lista dei progetti dell'user che vuole leggere i membri
                                    if(userDB.isMember(arguments[1],userDB.getUser(user))){
                                        String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);
                                        String str = userDB.getPrjMembers(arguments[1]+"@"+ip);
                                        outToClient.println("Members of " + arguments[1] +" project are: " + str);
                                    }else {
                                        outToClient.println("ERROR! You don't have access of " + arguments[1] + " project.");
                                    }
                                }else{
                                        outToClient.println("ERROR! You must login before showMembers.");
                                    }
                            }else{
                                outToClient.println("SYNTAX ERROR. Please try with showMembers [projectName].");
                            }
                            break;
                        case "addCard":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    //Imposto il limit a 3 perchè in questo caso ho prjName, cardName, description
                                    String[] args = arguments[1].split(" ", 3);
                                    if(args.length == 3){
                                        //Controllo che il progetto sia memorizzato nella lista dei progetti dell'user che vuole aggiungere la card
                                        if(userDB.isMember(args[0],userDB.getUser(user))){
                                            //Controllo che la card con quel nome non esista già
                                            Project pr = new Project(args[0]);
                                            pr.readToDoL();
                                            if(!pr.existenceCard(args[1])){
                                                Card c = new Card(args[1]);
                                                c.setcDescription(args[2]);
                                                boolean check = pr.addCard(c);
                                                if(check) {
                                                    pr.writeToDoL();
                                                    //Successfully
                                                    pr.writeCardFile(args[0], args[1], c);
                                                    outToClient.println("200");
                                                    //outToClient.println("Card: " + args[1] + " successfully added in " + args[0] + " project.");
                                                }else{
                                                    //Card already exists
                                                    outToClient.println("400");
                                                }

                                            }else{
                                                //Card already exists.")
                                                outToClient.println("400");
                                            }
                                        }else{
                                            //no access to this project
                                            outToClient.println("402");
                                        }
                                    }else {
                                        //SYNTAX ERROR
                                        outToClient.println("412");
                                    }
                                }else{
                                    //not logged
                                    outToClient.println("405");
                                }
                            }else {
                                //SYNTAX ERROR
                                outToClient.println("412");
                            }
                            break;

                        case "moveCard":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    //Imposto il limit a 4 perchè in questo caso ho prjName, cardName, listaPartenza, listaDestinazione
                                    String[] args = arguments[1].split(" ", 4);
                                    if(args.length == 4){
                                        //Controllo che il progetto sia memorizzato nella lista dei progetti dell'user che vuole spostare la card
                                        if(userDB.isMember(args[0],userDB.getUser(user))){
                                            //Controllo che la card con quel nome esista
                                            Project pr = new Project(args[0]);
                                            pr.readAllLists();
                                            int res;
                                            if(pr.existenceCard(args[1])){
                                                if((res = pr.moveCard(args[1], args[2], args[3]))==7){
                                                    pr.writeAllLists("src/StoredData/ProjectDir");
                                                 outToClient.println("200");

                                            }else {
                                                if(res <0) outToClient.println("406");
                                                else if(res == 0) outToClient.println("408");
                                                }
                                            }else outToClient.println("404");
                                        }

                                    }else outToClient.println("410");

                                }else outToClient.println("405");
                            }
                            break;
                        case "listProjects":
                            if(arguments.length == 1){
                                if(logged){
                                    userDB.readFile();
                                    outToClient.println(userDB.getUserProjectList(user));
                                } else {
                                    //Non sono loggato
                                    outToClient.println("no logged in");
                                }
                            }
                            break;
                        case "showCard":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    String[] aux = arguments[1].split(" ", 2);
                                    if(userDB.isMember(aux[0], userDB.getUser(user))){
                                        //Controllo che la card esista
                                        Project p = new Project(aux[0]);
                                        p.readAllLists();
                                        if(p.existenceCard(aux[1])){
                                            //La card esiste quindi visualizzo le card info
                                            String toClient = p.getCardInfo(aux[1]);
                                            outToClient.println(toClient);

                                        }else{
                                            //card non esistente
                                            outToClient.println("Card "+ aux[1] + " not found");
                                        }
                                    }else{
                                        //Non ha l'accesso
                                        outToClient.println("Attention! You don't have access to this project.");
                                    }
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. showCard [projectName] [cardName]" );
                            }
                            break;
                        case "showCards":
                            if(arguments.length == 2) {
                                if(logged){
                                    userDB.readFile();
                                    if(userDB.isMember(arguments[1],userDB.getUser(user))){
                                        //leggo le cards
                                        Project pr = new Project(arguments[1]);
                                        //pr.readAllLists();
                                        File file = new File("src/StoredData/ProjectDir/"+arguments[1]);
                                        File[] f = file.listFiles();
                                        String app = "";
                                        String nomeFile = "";
                                        if(f !=null && f.length >0){
                                            for (File c : f){
                                                //una card non si può chiamare Members
                                                if(!c.getName().contains("Members")) {
                                                    nomeFile = c.getName();
                                                    //Concateno le stringhe da inviare al client
                                                    app += nomeFile.replace(".json", " ");
                                                }
                                            }
                                            outToClient.println(app);
                                        }else{
                                            System.out.println("No cards stored in " + file.getName());
                                        }
                                    }else{
                                        //Non ha l'accesso
                                        outToClient.println("Attention! You don't have access to this project.");
                                    }
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. showCards [projectName]" );
                            }
                            break;
                        case "getCardHistory":
                            if(arguments.length == 2) {
                                if(logged){
                                    userDB.readFile();
                                    String[] aux = arguments[1].split(" ", 2);
                                    if(aux.length == 2){
                                        if(userDB.isMember(aux[0],userDB.getUser(user))){
                                            Project pr = new Project(aux[0]);
                                            pr.readAllLists();
                                            if(pr.existenceCard(aux[1])){
                                                //ottengo la lista che la contiene
                                                Card c = pr.getCard(aux[1]);
                                                if(c!= null)
                                                    outToClient.println(c.getHistory().toString());
                                            }else{
                                                //Non esiste la card
                                                outToClient.println("Attention! Card " + aux[1]+ " not exists!");
                                            }
                                        }else{
                                            //Non ha l'accesso
                                            outToClient.println("Attention! You don't have access to this project.");
                                        }
                                    }
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. getCardHistory [projectName] [cardName]" );
                            }
                            break;
                        case "cancelProject":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    if(userDB.isMember(arguments[1], userDB.getUser(user) )){
                                        Project p = new Project(arguments[1]);
                                        p.readAllLists();
                                        CopyOnWriteArrayList<Card> c = p.foundProjCards(p.getProjectName());
                                        String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);

                                        if(c.isEmpty()){
                                            //String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);
                                            userDB.deleteProject(user, arguments[1]+"@"+ip);
                                            projects.remove(p);
                                            p.deletePrjDir(new File("src/StoredData/ProjectDir/"+ arguments[1]));
                                            //Aggiorno il file degli ip
                                            p.compareIP();
                                            p.deleteIP(ip);
                                            outToClient.println("200@"+ip);
                                        }else{
                                            //if(p.getDone() == null && (p.getToDo().containsAll(c) || p.getInProgress().containsAll(c) || p.getToBeRevised().containsAll(c) )) {
                                            if(p.getDone() == null && !p.getAllCards().isEmpty()) {
                                                outToClient.println("414");
                                            }else {
                                                //if((p.getAllCards() == null && p.getDone() == null) || (p.getDone() == null && p.getAllCards().isEmpty()) || p.getDone()!= null && (p.getAllCards().size() == p.getDone().size() && p.getAllCards().equals(p.getDone()))){
                                                boolean flag = true;
                                                for(Card card :c){
                                                    if(!p.getDone().contains(card)){
                                                        flag = false;
                                                    }
                                                }
                                                if(flag){
                                                    for(Card card :c){
                                                        if(p.getDone().contains(card)){
                                                            p.getDone().remove(card);
                                                            p.writeDoneL();
                                                        }
                                                    }
                                                    //String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);
                                                    userDB.deleteProject(user, arguments[1]+"@"+ip);
                                                    projects.remove(p);
                                                    p.deletePrjDir(new File("src/StoredData/ProjectDir/"+ arguments[1]));
                                                    //Aggiorno il file degli ip
                                                    p.compareIP();
                                                    p.deleteIP(ip);
                                                    outToClient.println("200");
                                                }else outToClient.println("414");
                                            }
                                        }
                                    }
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. cancelProject [projectName]" );
                            }
                            break;

                        case "readChat":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    if(userDB.isMember(arguments[1], userDB.getUser(user) )){
                                       String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);
                                       outToClient.println(ip);
                                    }
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. cancelProject [projectName]" );
                            }
                            break;

                        case "sendChatMsg":
                            if(arguments.length == 2){
                                if(logged){
                                    userDB.readFile();
                                    if(userDB.isMember(arguments[1], userDB.getUser(user) )){
                                        String ip = userDB.getUser(user).getIpFromPrj(arguments[1]);
                                        outToClient.println(ip);
                                    }else outToClient.println("");
                                }else{
                                    //Non loggato
                                    outToClient.println("Attention! You're not logged in.");
                                }
                            }else{
                                outToClient.println("SYNTAX ERROR. command sendChatMsg [projectName]" );
                            }
                            break;

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }


}