package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

public class ContactIrcImpl
    extends AbstractContact
{
    private PresenceStatus status;
    private String nickname;
    private String server;
    private ProtocolProviderServiceIrcImpl provider;
    private ContactGroupIrcImpl parentGroup;
    
    /**
     * Determines whether this contact is persistent, i.e. member of the contact
     * list or whether it is here only temporarily.
     */
    private boolean isPersistent = true;

    /**
     * Determines whether the contact has been resolved (i.e. we have a
     * confirmation that it is still on the server contact list).
     */
    private boolean isResolved = true;
    
    public ContactIrcImpl(String address,
        ProtocolProviderServiceIrcImpl provider)
    {
        int at = address.lastIndexOf("@");
        this.nickname = address.substring(0, at);
        this.server = address.substring(at + 1);
        this.provider = provider;
        this.status = IrcStatusEnum.OFFLINE;
    }
    
    public ContactIrcImpl(String nickname, String server,
        ProtocolProviderServiceIrcImpl provider)
    {
        this.nickname = nickname;
        this.server = server;
        this.provider = provider;
        this.status = IrcStatusEnum.OFFLINE;
    }

    public String getNickname()
    {
        return this.nickname;
    }

    public String getAddress()
    {
        return this.nickname + "@" + this.server;
    }

    public String getDisplayName()
    {
        return nickname;
    }

    public byte[] getImage()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setPresenceStatus(PresenceStatus status)
    {
        this.status = status;
    }
    
    public PresenceStatus getPresenceStatus()
    {
        return status;
    }

    public ContactGroup getParentContactGroup()
    {
        return parentGroup;
    }

    public ProtocolProviderService getProtocolProvider()
    {
        return provider;
    }

    public boolean isPersistent()
    {
        return isPersistent;
    }

    public void setPersistent(boolean isPersistent)
    {
        this.isPersistent = isPersistent;
    }

    public boolean isResolved()
    {
        return isResolved;
    }
    
    public void setResolved(boolean isResolved)
    {
        this.isResolved = isResolved;
    }

    public String getPersistentData()
    {
        return null;
    }

    public String getStatusMessage()
    {
        return null;
    }

    public void setParentGroup(ContactGroupIrcImpl group)
    {
        this.parentGroup = group;
    }
}
