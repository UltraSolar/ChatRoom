package Chatbotproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable
{
    private ArrayList<Connectionhandler> connection;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server()
    {
        connection = new ArrayList<>();
        done = false;
    }

    @Override
    public void run()
    {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while(!done) {
                Socket client = server.accept();
                Connectionhandler handler = new Connectionhandler(client);
                connection.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }

    }

    public void broadcastMessage(String message)
    {
        for(Connectionhandler ch : connection)
        {
            if(ch != null){
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown()
    {
        done= true;
        if(!server.isClosed()){
            try {
                server.close();
            } catch (IOException e) {
                // ignore
            }
            for (Connectionhandler ch : connection)
            {
                ch.shutdown();
            }
        }
    }

    class Connectionhandler implements Runnable
    {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public Connectionhandler(Socket client)
        {
            this.client=client;
        }

        @Override
        public void run()
        {
            try
            {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please Enter Nick name : ");
                nickname = in.readLine();
                System.out.println(nickname + " Connected!");
                broadcastMessage(nickname + " joined the Chat!");
                String message;
                while((message = in.readLine()) != null)
                {
                    if(message.startsWith("/nickname "))
                    {
                        String[] messageSplit = message.split(" ", 2);
                        if(messageSplit.length == 2)
                        {
                            broadcastMessage(nickname + " has renamed to "+ messageSplit[1]);
                            System.out.println(nickname + " has renamed to "+ messageSplit[1]);
                            nickname=messageSplit[1];
                            out.println("Successfully changed the Nickname to "+nickname);

                        }else
                        {
                            out.println("No Nickname provided!");
                        }
                    }
                    else if(message.startsWith("/quit"))
                    {
                        broadcastMessage(nickname + " has left the chat!");
                        shutdown();

                    }
                    else
                        {
                        broadcastMessage( nickname + ": "+message);
                        }
                }
            }
            catch(IOException e)
            {
                shutdown();
            }
        }

        public void sendMessage(String message)
        {
            out.println(message);
        }

        public void shutdown()
        {
            try{
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            }catch (IOException e)
            {
                // ignore
            }
        }
    }

    public static void main(String[] args)
    {
        Server server = new Server();
        server.run();
    }

}
