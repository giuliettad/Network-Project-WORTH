package Server;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

//Classe che rappresentà l'entità card

public class Card {
    private String cName;
    private  String cDescription;

    public enum CardList{
        TODO,
        INPROGRESS,
        TOBEREVISED,
        DONE;

    }
    private CopyOnWriteArrayList<CardList> history;

    public Card(String cardName){
        this.cName = cardName;
        //this.cDescription = "";
        history = new CopyOnWriteArrayList<>();
        history.add(CardList.TODO);
    }


    //Più thread possono accedere contemporaneamente quindi uso la synchronysed
    public synchronized void setcDescription(String description){ this.cDescription = description; }

    public synchronized CardList getLastElement(String cardName){
        return history.get(history.size()-1);
    }

    //aggiungiunge nuovo stato card alla history
    public void set_History(String listaDest) {

        switch (listaDest) {
            case "inProgress":
                this.history.add(CardList.INPROGRESS);
                break;
            case "toBeRevised":
                this.history.add(CardList.TOBEREVISED);
                break;
            case "done":
                this.history.add(CardList.DONE);
                break;
        }
    }


    public String getCardName(){ return cName; }

    public String getCardDescription(){ return cDescription; }

    public CopyOnWriteArrayList<CardList> getHistory(){ return history; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return Objects.equals(cName, card.cName);
    }





}
