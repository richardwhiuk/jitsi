package net.java.sip.communicator.impl.protocol.irc;

import java.io.*;

import net.java.sip.communicator.util.Logger;

public class IrcInputThread
    extends Thread
{
    private IrcStack stack;
    private BufferedReader reader;

    private static final Logger logger = Logger.getLogger(IrcInputThread.class);

    public IrcInputThread(IrcStack stack, BufferedReader reader)
    {
        this.stack = stack;
        this.reader = reader;
    }
    
    public void run()
    {
        logger.info("Started Input Thread");
        String line;
        IrcMessage message;
        while (true)
        {
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    message = IrcMessage.fromLine(line);
                    logger.info("Recieved: " + message);
                    stack.handle(message);
                }
            }
            catch(Exception e)
            {
                logger.fatal("Error: ", e);
            }
        }
    }
}
