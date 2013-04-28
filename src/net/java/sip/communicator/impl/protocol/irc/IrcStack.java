package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

public class IrcStack
{
    private IrcAccountID account;
    private String server;
    
    private final OperationSetPresenceIrcImpl presence;
    private final OperationSetMultiUserChatIrcImpl multiChat;
    private final OperationSetBasicInstantMessagingIrcImpl basicChat;
    
    private enum ConnectState
    {
        NotConnected,
        Connecting,
        Connected
    }
    
    private ConnectState state;
    private final ProtocolProviderServiceIrcImpl parentProvider;
    private IrcInputThread input;
    private IrcOutputThread output;
    
    private BlockingQueue<IrcMessage.Private> privateMessages;
    private Timer privateMessageTimer;
    
    private static final Logger logger
       = Logger.getLogger(IrcStack.class);

    public IrcStack(ProtocolProviderServiceIrcImpl parentProvider, 
        IrcAccountID account,
        String server)
    {
        this.privateMessageTimer = new Timer();
        this.privateMessages = new LinkedBlockingQueue<IrcMessage.Private>();
        this.parentProvider = parentProvider;
        this.multiChat = (OperationSetMultiUserChatIrcImpl) parentProvider
            .getOperationSet(OperationSetMultiUserChat.class);
        this.basicChat = (OperationSetBasicInstantMessagingIrcImpl) parentProvider
            .getOperationSet(OperationSetBasicInstantMessaging.class);
        this.presence = (OperationSetPresenceIrcImpl) parentProvider
            .getOperationSet(OperationSetPresence.class);
        this.account = account;
        this.server = server;
        
        logger.info("Creating IRC stack with for: " + this.account.getUserID() + "@" + server);
    }

    public void connect(String serverAddress, int serverPort,
        String serverPassword, boolean autoNickChange)
    {
        synchronized (this)
        {
            if(state == ConnectState.Connected || state == ConnectState.Connecting)
            {
                logger.info("Already in state:" + state);
                return;
            }
            
            state = ConnectState.Connecting;

            int mode = 0;
            
            logger.info("Connecting to " + serverAddress + ":" + serverPort);
            
            try
            {
                Socket socket = new Socket(serverAddress, serverPort);
                
                logger.info("Connected to server: " + serverAddress + ":" + serverPort);
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                
                BufferedReader breader = new BufferedReader(inputStreamReader);
                BufferedWriter bwriter = new BufferedWriter(outputStreamWriter);
                
                input = new IrcInputThread(this, breader);
                output = new IrcOutputThread(this, bwriter);
                
                logger.info("Starting threads");
                
                if (serverPassword != null)
                {
                    output.add(IrcMessage.Password(serverPassword));
                }
                
                output.add(IrcMessage.Nickname(this.account.getUserID()));
                output.add(IrcMessage.User(this.account.getUserID(), mode, this.account.getUserID()));
                
                input.start();
            }
            catch(IOException e)
            {
                logger.fatal("Connecting failed with: ", e);
            }
            
        }
            
    }

    public void setPassword()
    {
        
    }
    
    public synchronized void onConnect()
    {
        if (state == ConnectState.Connecting)
        {
            state = ConnectState.Connected;
            presence.setPresenceStatus(IrcStatusEnum.ONLINE);
            parentProvider.setCurrentRegistrationState(RegistrationState.REGISTERED);
            output.start();
        }
        else if(state == ConnectState.NotConnected)
        {
            logger.warn("onConnect while not connected");
        }
    }
    
    public void handle(IrcMessage message)
    {
       onConnect();
       
       message = message.parse();
       
       if(message.getClass() == IrcMessage.Ping.class)
       {
           logger.info("Ping request, send pong");
           output.add(IrcMessage.Pong(message));
       }
       else if(message.getClass() == IrcMessage.Channel.class)
       {
           IrcMessage.Channel channel = (IrcMessage.Channel) message;
           logger.info("Channel message from: " + channel.channel + " saying: " + channel.message);
       }
       else if(message.getClass() == IrcMessage.Private.class)
       {
           IrcMessage.Private privmsg = (IrcMessage.Private) message;
           receiveMessage(privmsg);
       }
       else if(message.getClass() == IrcMessage.Join.class)
       {
           IrcMessage.Join join = (IrcMessage.Join) message;
           logger.info("Join message from: " + join.nickname);
       }
    }

    public void sendMessage(String nickname, String message)
    {
        output.add(IrcMessage.Private(nickname, message));
    }
    
    protected void receiveMessage(IrcMessage.Private message)
    {
        boolean found = false;
        
        synchronized(privateMessages)
        {
            for(IrcMessage.Private privmsg: privateMessages)
            {
                if (privmsg.nickname.equals(message.nickname))
                {
                    found = true;
                    privmsg.message += '\n' + message.message;
                }
            }
            
            if (!found)
            {
                privateMessages.add(message);
            }
        }
        
        if (!found)
        {
            privateMessageTimer.schedule(new TimerTask(){
                @Override
                public void run()
                {
                    try
                    {
                        IrcMessage.Private message = privateMessages.take();
                        logger.info("Private message from: " + message.nickname + " saying: " + message.message);
                        privateMessage(message.nickname, message.message);
                    }
                    catch (InterruptedException e)
                    {
                        logger.error("Interrupted sending private message");
                    }
                }
            }, 200);
        }
    }
    
    protected void privateMessage(String nickname, String messageContent)
    {
        MessageIrcImpl message =
            new MessageIrcImpl(messageContent,
                MessageIrcImpl.DEFAULT_MIME_TYPE,
                MessageIrcImpl.DEFAULT_MIME_ENCODING, null);
        
        String address = nickname + "@" + server;
        
        ContactIrcImpl from = 
            (ContactIrcImpl) presence.findContactByID(address);
        
        if(from == null)
        {
            from = presence.createVolatileContact(address);
        }
        
        from.setPresenceStatus(IrcStatusEnum.ONLINE);
        basicChat.receiveInstantMessage(message, from);

    }
    
    public void dispose()
    {
        
    }

    public boolean isConnected()
    {
        return (state == ConnectState.Connected);
    }

    public void disconnect()
    {
        logger.fatal("disconnect");
        // FIXME
    }

    public List<String> getServerChatRoomList()
    {
        logger.fatal("getServerChatRoomList");
        // FIXME
        return null;
    }

    public boolean isJoined(ChatRoomIrcImpl chatRoomIrcImpl)
    {
        logger.fatal("isJoined");
        // FIXME
        return false;
    }

    public void join(ChatRoomIrcImpl chatRoomIrcImpl)
    {
        logger.fatal("join");
        // FIXME
    }

    public void join(ChatRoomIrcImpl chatRoomIrcImpl, byte[] password)
    {
        logger.fatal("join");
        // FIXME
    }

    public void leave(ChatRoomIrcImpl chatRoomIrcImpl)
    {
        logger.fatal("leave");
        // FIXME
    }

    public void banParticipant(String name, String contactAddress, String reason)
    {
        logger.fatal("ban");
        // FIXME
    }

    public void kickParticipant(String name, String contactAddress,
        String reason)
    {
        logger.fatal("kick");
        // FIXME
    }

    public void setSubject(String name, String subject)
    {
        logger.fatal("subject");
        // FIXME
    }

    public String getNick()
    {
        logger.fatal("getNick");
        // FIXME
        return null;
    }

    public void setUserNickname(String nickName)
    {
        logger.fatal("setUserNickname");
        // FIXME
    }

    public void sendInvite(String userAddress, String chatRoomName)
    {
        logger.fatal("sendInvite");
        // FIXME
    }

    public void sendCommand(ChatRoomIrcImpl chatRoomIrcImpl,
        String messagePortion)
    {
        logger.fatal("sendCommand");
        // FIXME
    }
}
