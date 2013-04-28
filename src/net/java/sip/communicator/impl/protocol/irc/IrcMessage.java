package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.util.*;

public class IrcMessage
{
    public class Ping extends IrcMessage
    {
        public Ping(IrcMessage message)
        {
            super(message);
        }
    }
    
    public class Channel extends IrcMessage
    {
        public String channel;
        public String message;
        public Channel(IrcMessage irc)
        {
            super(irc);
            channel = parameters.get(0);
            message = parameters.get(1);
        }
    }
    
    public class Private extends IrcMessage
    {
        public String to;
        public String message;
        public Private(IrcMessage irc)
        {
            super(irc);
            to = parameters.get(0);
            message = parameters.get(1);
        }
    }

    public class Part extends IrcMessage
    {
        public List<String> channels;
        public String message;
        
        public Part(IrcMessage irc)
        {
            super(irc);
            String temp = parameters.get(0);
            int i = 0;
            int j;
            while((j = temp.indexOf(',',i)) != -1)
            {
                channels.add(temp.substring(i, j));
                i = j + 1;
            }
            channels.add(temp.substring(i));
            if(parameters.size() > 1)
            {
                message = parameters.get(1);
            }
        }
    }
    
    public class Join extends IrcMessage
    {
        public Map<String,String> channels;
        public boolean leave;
        public Join(IrcMessage irc)
        {
            super(irc);
            List<String> channels = new ArrayList<String>();
            String temp = parameters.get(0);
            if (temp.equals("0"))
            {
                leave = true;
            }
            else
            {
                leave = false;
                int i = 0;
                int j;
                while((j = temp.indexOf(',',i)) != -1)
                {
                    channels.add(temp.substring(i, j));
                    i = j + 1;
                }
                channels.add(temp.substring(i));
                temp = parameters.get(0);
                i = 0;
                for(String channel: channels)
                {
                    j = temp.indexOf(',',i);
                    if(j == -1 && i != -1)
                    {
                        this.channels.put(channel, temp.substring(i));
                        i = -1;
                    }
                    else
                    {
                        this.channels.put(channel, temp.substring(i, j));
                        i = j + 1;
                    }
                }
            }
        }
    }
    
    protected int code;
    protected String nickname; // Or server name.
    protected String user;
    protected String host;
    protected String command;
    protected List<String> parameters;
    
    private static final Logger logger = Logger.getLogger(IrcOutputThread.class);
    
    private IrcMessage(IrcMessage irc)
    {
        nickname = irc.nickname;
        user = irc.host;
        command = irc.command;
        code = irc.code;
        parameters = irc.parameters;
    }
    
    private IrcMessage()
    {
        nickname = "";
        user = "";
        host = "";
        code = 0;
        command = "";
        parameters = new ArrayList<String>();
    }

    public static IrcMessage fromLine(String line)
    {
        IrcMessage message = new IrcMessage();

        if (line.length() > 0)
        {
            int i = 0;
            if(line.charAt(i) == ':')
            {
                i += 1;
                int j = line.indexOf(' ', i);
                int k = line.indexOf('!', i);
                if(k != -1 && k < j)
                {
                    message.nickname = line.substring(i, k);
                    i = k + 1;
                    k = line.indexOf('@', i);
                    if (k != -1 && k < j)
                    {
                        message.user = line.substring(i, k);
                        message.host = line.substring(k + 1, j);
                    }
                    else
                    {
                        logger.error("Invalid prefix:" + line);
                        message.host = line.substring(k + 1, j);
                    }
                }
                else
                {
                    k = line.indexOf('@', i);
                    if (k != -1 && k < j)
                    {
                        message.nickname = line.substring(i, k);
                        message.host = line.substring(k + 1, j);
                    }
                    else
                    {
                        message.nickname = line.substring(i, j);
                    }
                }
                i = j + 1;
            }
            
            int j = line.indexOf(' ', i);
            message.command = line.substring(i, j);
            i = j + 1;
            
            try
            {
                message.code = Integer.parseInt(message.command);
            }
            catch(NumberFormatException e)
            {
                message.code = 0;
            }
            
            int n = 0;
            
            while(n < 15)
            {
                if(line.charAt(i) == ':')
                {
                    message.addParameter(line.substring(i + 1));
                    n = 15;
                }
                else
                {
                    j = line.indexOf(' ', i);
                    if (j == -1)
                    {
                        message.addParameter(line.substring(i));
                        n = 15;
                    }
                    else
                    {
                        message.addParameter(line.substring(i, j));
                        i = j + 1;
                        n += 1;
                    }
                }
            }
            
        }
        return message;
    }
    
    public String toLine()
    {
        StringBuilder build = new StringBuilder();
        if(nickname != "")
        {
            build.append(":");
            build.append(nickname);
            if(host != "")
            {
                if(user != "")
                {
                    build.append("!");
                    build.append(user);
                }
                build.append("@");
                build.append(host);
            }
            build.append(" ");
        }
        build.append(command);
        int trailing = 0;
        for(String param: parameters)
        {
            build.append(" ");
            if (param.indexOf(' ') != -1)
            {
                build.append(":");
                trailing += 1;
            }
            build.append(param);
        }
        
        if (trailing > 1)
        {
            logger.fatal("Invalid message: " + this);
        }
        
        return build.toString();
    }
    
    public String toString(){
        StringBuilder build = new StringBuilder("'");
        build.append(nickname);
        build.append("' '");
        build.append(user);
        build.append("' '");
        build.append(host);
        build.append("' '");
        build.append(command);
        for(String param: parameters)
        {
            build.append("', '");
            build.append(param);
        }
        build.append("'");
        
        return build.toString();
    }
    
    public void addParameter(String parameter)
    {
        this.parameters.add(parameter);
    }

    public static IrcMessage Password(String serverPassword)
    {
        IrcMessage message = new IrcMessage();
        message.command = "PASS";
        message.addParameter(serverPassword);
        return message;
    }
    
    public static IrcMessage Nickname(String nickname)
    {
        IrcMessage message = new IrcMessage();
        message.command = "NICK";
        message.addParameter(nickname);
        return message;
    }

    public static IrcMessage User(String user, int mode, String realname)
    {
        IrcMessage message = new IrcMessage();
        message.command = "USER";
        message.addParameter(user);
        message.addParameter(Integer.toString(mode));
        message.addParameter("*");
        message.addParameter(realname);
        return message;
    }
    
    public static IrcMessage Pong(IrcMessage ping)
    {
        IrcMessage message = new IrcMessage();
        message.command = "PONG";
        message.parameters = ping.parameters;
        return message;
    }

    public static IrcMessage Private(String nickname, String body)
    {
        IrcMessage message = new IrcMessage();
        message.command = "PRIVMSG";
        message.addParameter(nickname);
        message.addParameter(body);
        return message;
    }
    
    public IrcMessage parse()
    {
        if(command.equals("PING"))
        {
            return new IrcMessage.Ping(this);
        }
        else if(command.equals("JOIN"))
        {
            return new IrcMessage.Join(this);
        }
        else if(command.equals("PART"))
        {
            return new IrcMessage.Part(this);
        }
        else if(code == 1 || code == 2 || code == 3 ||     // New user
                code == 375 || code == 372 || code == 376  // TODO: MOTD
               )
        {
            return new IrcMessage.Private(this);
        }
        else if(command.equals("NOTICE") || command.equals("002"))
        {
            String message = parameters.get(1);
            char c = message.charAt(0);
            if(c == '#' || c == '&' || c == '!' || c == '+')
            {
                return new IrcMessage.Channel(this);
            }
            else
            {
                return new IrcMessage.Private(this);
            }
        }
        else if(command.equals("PRIVMSG") && parameters.size() == 2)
        {
            String message = parameters.get(1);
            char c = message.charAt(0);
            if(command.equals("PRIVMSG") && c == '\u0001' && message.charAt(message.length() -1) == '\u0001')
            {
                // CTCP Request
            }
            else if(c == '#' || c == '&' || c == '!' || c == '+')
            {
                return new IrcMessage.Channel(this);
            }
            else
            {
                return new IrcMessage.Private(this);
            }
        }
        return this;
    }

}
