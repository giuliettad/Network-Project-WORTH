package Server;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class User {
    private String nickName;
    private String password;
    // Stato dell'utente, è un campo che non rendo persistente nella scrittura sul file
    transient private String isOnline;
    // lista progetti di cui l'utente è membro
    private CopyOnWriteArrayList<String> projectList;

    public User(){
        this.nickName = "";
        this.password = "";
        //Inizializzo lo stato a offline
        this.isOnline ="offline";
        this.projectList = new CopyOnWriteArrayList<>();
    }

    public String getNickName() {
        return nickName;
    }

    public String getPassword() {
        return password;
    }

    public String getIsOnline() { return isOnline; }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIsOnline(String isOnline) { this.isOnline = isOnline; }

    public boolean checkPassword(String password){return password.equals(getPassword());}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(nickName, user.nickName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nickName);
    }

    public CopyOnWriteArrayList<String> getProjectList() {
        return this.projectList;
    }

    public void setProjectList(CopyOnWriteArrayList<String> projectList) {
        this.projectList = projectList;
    }

    //metodo che ritorna l'ip di un progetto
    public String getIpFromPrj(String projectName){
        for(String tmp : projectList){
            if(tmp.split("@")[0].equals(projectName))
                return tmp.split("@")[1];
        }
        return null;
    }



}
