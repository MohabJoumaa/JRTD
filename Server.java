package monopoly;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class Server {

    private ServerSocket serversocket = null;
    private Socket clientsocket = null;
    private InputStream is = null;
    private ObjectInputStream ois = null;
    private List<String> ipAddresses;
    private List<Client> clientsList;

    public Server(InetAddress bindAddr, List<String> ipAddresses) {
        try {
            this.ipAddresses = ipAddresses;
            this.clientsList = new ArrayList<>(this.ipAddresses.size());
            this.serversocket = new ServerSocket(5000, 8, bindAddr);
            this.serversocket.setSoTimeout(1800);
            this.clientsocket = this.serversocket.accept();
            this.ipAddresses.stream().map((ip) -> new Client(ip, this.clientsocket)).forEachOrdered((client) -> {
                this.clientsList.add(client);
            });
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
            JOptionPane.showMessageDialog(null, "Please, Check Your Network Connection.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    public synchronized void runServerSocket(GameGraphics game) {
        new Thread(new ReceivingThread(game)).start();
    }

    protected void sendDataToClient(String player, Data data) {
        OutputStream os = null;
        ObjectOutputStream oos = null;
        try {
            Client client = null;
            for (Client client1 : this.clientsList) {
                if (client1.getIP().equals(player)) {
                    client = client1;
                    break;
                }
            }
            Socket socket = client.getSocket();
            if (this.ipAddresses.contains(client.getIP())) {
                //System.out.println("IP: " + client.getIP());
                os = socket.getOutputStream();
                oos = new ObjectOutputStream(os);
                oos.writeObject(data);
                oos.flush();
            }
        } catch (IOException ex) {
            try {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, "Please, Check Your Network Connection.", "Error", JOptionPane.ERROR_MESSAGE);
                os.close();
                oos.close();
                this.clientsocket.close();
                this.serversocket.close();
                System.exit(0);
            } catch (IOException ex1) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }

    private class ReceivingThread implements Runnable {

        private Data data = null;
        private final GameGraphics game;

        private ReceivingThread(GameGraphics game) {
            this.game = game;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    is = clientsocket.getInputStream();
                    ois = new ObjectInputStream(is);
                    this.data = (Data) ois.readObject();
                    this.game.setData(clientsocket.getInetAddress().getHostAddress(), this.data);
                }
            } catch (IOException | ClassNotFoundException ex) {
                try {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    JOptionPane.showMessageDialog(null, "Please, Check Your Network Connection.", "Error", JOptionPane.ERROR_MESSAGE);
                    is.close();
                    ois.close();
                    clientsocket.close();
                    serversocket.close();
                    System.exit(0);
                } catch (IOException ex1) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }

    }

}
