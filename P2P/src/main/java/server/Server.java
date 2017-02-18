package server;

import javax.imageio.IIOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by od on 16.02.2017.
 */
public class Server extends Thread {

    HashSet<Client> peerList;
    private ServerSocket serverSocket;
    private ServerSocket heartBeatSocket;
    private int heartBeatPort;

    public Server(int port , int port2) throws IOException {
        System.out.println("Server is opening from " + port);
        serverSocket = new ServerSocket(port);
        peerList = new HashSet<Client>();
        heartBeatPort = port2;
    }

    private void sendPeerList(Socket socket) throws IOException {
        //Serialize and send the socket.
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeInt(0);
    }


    private int refreshList() {

        synchronized (this) {
            System.out.println("Refresh is called.");

            if (peerList.size() == 0)
                return -1;

            System.out.println("HB is opening from server, " + heartBeatPort + " size " + peerList.size());

            //Send heartbeats to each peer and remove the ones doesnt responding
            try {
                heartBeatSocket = new ServerSocket(heartBeatPort);
                heartBeatSocket.setReuseAddress(true);
            } catch (IOException e) {
                System.out.println("HeartBeat socket cannot be opened.");
                e.printStackTrace();
                return -1;
            }


            HashSet<String> newList = new HashSet<String>();
            CountDownLatch latch = new CountDownLatch(1 + peerList.size());
            System.out.println("Latch is " + latch.getCount());
            new Thread(() -> {
                try {
                    long t = System.currentTimeMillis();
                    long end = t + 4000;

                    while (System.currentTimeMillis() < end) {
                        System.out.println(System.currentTimeMillis() + " : " + end );
                        Socket conn = heartBeatSocket.accept();
                        System.out.println("CCConnected to " + conn.getRemoteSocketAddress());

                        DataInputStream in = new DataInputStream(conn.getInputStream());
                        int x = in.readInt();
                        if (x != 0) {
                            System.out.println("Error in heartbeat.");
                        } else {
                            String addr = in.readUTF();
                            System.out.println(addr);
                            newList.add(addr);
                        }

                        break;
                    }
                    latch.countDown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            for (Client client : peerList) {
                new Thread(() -> {
                    try {
                        Socket clientSocket = new Socket(client.getAddress(), client.getHeartBeatPort());
                        DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                        out.writeInt(0);  //0 for heartbeats
                        out.writeInt(heartBeatPort);
                        latch.countDown();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            System.out.println("Xxx");
            try {
                boolean b = latch.await(6, TimeUnit.SECONDS);
                System.out.println(b);
            } catch (InterruptedException e) {
                System.out.println("Latch is interrupted before release.");
            }

            System.out.println("Counting...");
            int count = 0;

            for (Client client : peerList) {
                if (!newList.contains(client.getAddress().toString()))
                    count++;
                peerList.remove(client);
            }

            try {
                heartBeatSocket.close();
            } catch (IOException e) {
                System.out.println("Error in closing HB socket");
            }


            System.out.println(heartBeatSocket.isClosed());

            return count;
        }
    }

    public void run() {
        while (true) {
            try {

                Timer timer = new Timer();
                timer.schedule(new runWithTime(), 0, 10 * 1000);

                Socket server = serverSocket.accept();

                DataInputStream in = new DataInputStream(server.getInputStream());
                int port = in.readInt();
                int port2 = in.readInt();
                System.out.println("Connected to " + server.getRemoteSocketAddress());
                System.out.println("Server port of the client is: "  + port);

                peerList.add( new Client(server.getInetAddress(),port,port2 ));
            }
            catch (SocketTimeoutException s) {
                System.out.println("Socket timed out!");
                break;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public class runWithTime extends TimerTask {
        public void run() {

            int x = refreshList();
            System.out.println("List refreshed, " + x + "clients disconnected.");
        }
    }
}
