package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class Chat extends Thread{
    final static int BUFFER_SIZE = 4096;

    private Queue queue;
    private int PORT;
    private MulticastSocket multicast;
    private InetAddress group;


    public Chat(MulticastSocket socket, InetAddress group, int port) throws IOException {
        this.queue = new Queue();
        this.PORT = port;
        this.multicast = socket;
        this.group = group;
    }

    //invia un datagramPacket al gruppo multicast
    public void sendMsg(String msg) throws NullPointerException, IOException {
        if (msg == null) throw new NullPointerException();
        byte[] buffer = msg.getBytes();
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length, this.group,this.PORT);
        multicast.send(datagram);
    }

    //legge un messaggio dalla coda
    public ArrayList<String> readMsg() {
        return queue.readClear();
    }

    //Attende per un secondo di riceve un DatagramPacket se non lo riceve restituisce controllo al chimante
    // se lo riceve estrae il messaggio dal paccheto e lo aggiunge in coda
    public void recive() throws IOException {
        byte[] msg_recived = new byte[BUFFER_SIZE];
        DatagramPacket PACK = new DatagramPacket(msg_recived, msg_recived.length, this.group, this.PORT);
        multicast.setSoTimeout(1000);
        try {
            multicast.receive(PACK);
        } catch (SocketTimeoutException e) {
            return;
        }
        String msg = new String(PACK.getData(), 0, PACK.getLength(), "UTF-8");
        addMessage(msg);
    }

    //aggiunge un messaggio in coda messaggi
    public void addMessage(String msg) {
        queue.put(msg);
    }

    //un thread viene generato per stare in ascolto su un diverso gruppo multicast
    //che rappresenta la chat di un progetto di cui l'utente fa parte
    @Override
    public void run() {
        try {

            //mi unisco al gruppo multicast
            multicast.joinGroup(group);
            //fin tanto che non vengo interroto
            while (!Thread.interrupted()) {
                //Tenta di leggere i messaggi sul gruppo per 1 secondo
                //se presenti li aggiunge alla coda dei messaggi
                recive();
            }
            multicast.leaveGroup(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("INTERROTTO");
        multicast.close();
    }

}
