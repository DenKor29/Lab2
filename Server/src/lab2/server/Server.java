package lab2.server;

import lab2.network.TCPConnection;
import lab2.network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Server implements TCPConnectionListener {

    private ServerSocket serverSocket;
    private ServerListener eventListener;

    private Thread rxThread;

    private static final String IPP_ADDR = "127.0.0.1";
    private static final int PORT = 9000;




    public Server(ServerListener eventListener)  {

        this.eventListener = eventListener;

    }

    public  void start(int port,int timeoutAcept){
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                connected(port,timeoutAcept);
                disconnectedServer();
            }
        });
        rxThread.start();
    }
    public void interrupt(){
      rxThread.interrupt();
    }
    private void connected(int port,int timeoutAccept) {

        System.out.println("Start  - listening port " + port);

        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(timeoutAccept);
            eventListener.onConnectionServer(Server.this);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        };

            while (!rxThread.isInterrupted()) {
                try {
                    Socket client_socket = serverSocket.accept();
                    if (client_socket !=null) {
                        new TCPConnection(this, client_socket);
                    }
                } catch (IOException e) {
                    if (!(e instanceof SocketTimeoutException))
                        System.out.println("TCPConnection exeption:" + e);
                }
                ;
            }


    }

    private void disconnectedServer(){
        try {
            serverSocket.close();
        }
        catch (IOException e) {
            eventListener.onException(this,e);
        };

    }

    private void disconnected(TCPConnection tcpConnection) {

            System.out.println("Client disconnecting ...");
            tcpConnection.disconnected();
            //eventListener.onDisconnectionReady(this,tcpConnection);

    }
    private void process(TCPConnection tcpConnection,String value){

        String nick = "";
        String message = "";

        String[] data = value.split(":");

        if (data.length == 1) {
            // Нет никнейма
            message = data[0];
        }
        if (data.length == 2) {
            // Нет никнейма
            nick = data[0];
            message = data[1];
        }

        if (data.length >2) {
            //Это разбивка основного текста - всотанавливаем строку
            for (int i=1;i<data.length;i++) {
                message+=data[i]+":";
            }
        }
        //Обработка команд от клиента
        if (message.equals("END")) disconnected(tcpConnection);
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        eventListener.onConnectionReady(this,tcpConnection);
    }

    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String value) {

        String message = value.trim();
        eventListener.onMessageString(Server.this,tcpConnection,value);
        tcpConnection.sendString(value);
        process(tcpConnection,value);

    }

    @Override
    public synchronized void onDisconnection(TCPConnection tcpConnection) {
        eventListener.onDisconnectionReady(this,tcpConnection);
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exeption:"  + e );
    }

    public synchronized void sendString(TCPConnection tcpConnection,String value){

        tcpConnection.sendString(value);
    }
}
