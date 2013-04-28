package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import net.java.sip.communicator.util.*;

public class IrcOutputThread
    extends Thread
{
    private IrcStack stack;
    private BufferedWriter writer;
    private BlockingQueue<IrcMessage> queue;
    
    private static final Logger logger = Logger.getLogger(IrcOutputThread.class);

    public IrcOutputThread(IrcStack stack, BufferedWriter writer)
    {
        this.stack = stack;
        this.writer = writer;
        this.queue = new LinkedBlockingQueue<IrcMessage>();
    }
    
    public void add(IrcMessage message)
    {
        boolean sent = false;
        while (!sent)
        {
            try
            {
                queue.put(message);
                sent = true;
            }
            catch (InterruptedException e)
            {
                
            }
        }
    }
    
    public synchronized void send(IrcMessage message)
    {
        try
        {
            writer.write(message.toLine() + "\r\n");
            writer.flush();
            logger.info("Sent: " + message);
        }
        catch (IOException e)
        {
            logger.fatal("Failed to send message: " + message, e);
        }
    }
    
    public void run()
    {
        logger.info("Started Output Thread");
        IrcMessage message = null;
        while(true)
        {
            try
            {
                message = queue.take();
                send(message);
            }
            catch (InterruptedException e)
            {
                
            }

        }
    }
}
