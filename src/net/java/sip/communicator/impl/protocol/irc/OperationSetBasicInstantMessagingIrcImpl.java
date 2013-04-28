package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

public class OperationSetBasicInstantMessagingIrcImpl
    extends AbstractOperationSetBasicInstantMessaging
{
    private ProtocolProviderServiceIrcImpl ircProvider = null;
    
    public OperationSetBasicInstantMessagingIrcImpl(
        ProtocolProviderServiceIrcImpl provider)
    {
        this.ircProvider = provider;
    }

    private static final Logger logger
        = Logger.getLogger(OperationSetBasicInstantMessagingIrcImpl.class);
    
    public void sendInstantMessage(Contact to, Message message)
        throws IllegalStateException, IllegalArgumentException
    {
        if( !(to instanceof ContactIrcImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not a IRC contact."
                + to);
        
        if (!(to.getProtocolProvider() == ircProvider))
        {
            throw new IllegalArgumentException(
                "The specified IRC contact is not associated to this network."
                + to);
        }
        
        ContactIrcImpl ircTo = (ContactIrcImpl) to;
        
        ircProvider.getIrcStack().sendMessage(ircTo.getNickname(), message.getContent());
        
         // FIXME: Make this fire when the message has actually been sent.
         fireMessageDelivered(message, to);
    }
    
    public void receiveInstantMessage(Message message, Contact from)
    {
        fireMessageReceived(message, from);
    }

    public boolean isOfflineMessagingSupported()
    {
        return false;
    }

    public boolean isContentTypeSupported(String contentType)
    {
        if(contentType.equals(DEFAULT_MIME_TYPE))
            return true;
        else
           return false;
    }

    @Override
    public Message createMessage(String content, String contentType,
        String encoding, String subject)
    {
        MessageIrcImpl message = new MessageIrcImpl(content, contentType, encoding, subject);
        return message;
    }

}
