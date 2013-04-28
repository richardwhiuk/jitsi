package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

public class OperationSetPresenceIrcImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceIrcImpl>
{   
    private static final Logger logger =
        Logger.getLogger(OperationSetPresenceIrcImpl.class);
    private PresenceStatus status;
    private ContactGroupIrcImpl contactListRoot = null;
    private ProtocolProviderServiceIrcImpl parentProvider;
    
    public OperationSetPresenceIrcImpl(
        ProtocolProviderServiceIrcImpl provider)
    {
        super(provider);
        contactListRoot = new ContactGroupIrcImpl("RootGroup", provider);
        this.parentProvider = provider;
        this.status = IrcStatusEnum.OFFLINE;
    }
    
    private ContactGroupIrcImpl getNonPersistentGroup()
    {
        for (int i = 0
             ; i < getServerStoredContactListRoot().countSubgroups()
             ; i++)
        {
            ContactGroupIrcImpl gr =
                (ContactGroupIrcImpl)getServerStoredContactListRoot()
                    .getGroup(i);

            if(!gr.isPersistent())
                return gr;
        }

        return null;
    }
    
    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     * @param server 
     *
     * @param contactAddress the address of the volatile contact we'd like to
     * create.
     * @return the newly created volatile contact.
     */
    public ContactIrcImpl createVolatileContact(String address)
    {
        logger.info("Create volatile contact: " + address);
        
        //First create the new volatile contact;
        ContactIrcImpl newVolatileContact = 
            new ContactIrcImpl(address, this.parentProvider);

        newVolatileContact.setResolved(false);
        newVolatileContact.setPersistent(false);

        //Check whether a volatile group already exists and if not create
        //one
        ContactGroupIrcImpl theVolatileGroup = getNonPersistentGroup();

        //if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            theVolatileGroup = new ContactGroupIrcImpl(
                IrcActivator.getResources().getI18NString(
                    "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME")
                , parentProvider);
            theVolatileGroup.setResolved(false);
            theVolatileGroup.setPersistent(false);

            this.contactListRoot.addSubgroup(theVolatileGroup);

            fireServerStoredGroupEvent(theVolatileGroup
                           , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }

        //now add the volatile contact inside it
        theVolatileGroup.addContact(newVolatileContact);
        fireSubscriptionEvent(newVolatileContact
                         , theVolatileGroup
                         , SubscriptionEvent.SUBSCRIPTION_CREATED);

        return newVolatileContact;
    }

    public PresenceStatus getPresenceStatus()
    {
        return status;
    }
    
    public void setPresenceStatus(PresenceStatus status)
    {
        logger.info("setPresenceStatus to: " + status.getStatusName());
        this.status = status;
    }
    
    public void publishPresenceStatus(PresenceStatus status,
        String statusMessage)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        logger.info("publishPresenceStatus: " + status.getStatusName() + " " + statusMessage);
        logger.fatal("publishPresenceStatus");
    }


    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        return IrcStatusEnum.supportedStatusSet();
    }


    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        logger.fatal("queryContactStatus for: " + contactIdentifier);
        return null;
    }

    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        ContactGroupIrcImpl parentGroup
            = (ContactGroupIrcImpl)((ContactIrcImpl)contact)
            .getParentContactGroup();

        parentGroup.removeContact((ContactIrcImpl)contact);
        
        fireSubscriptionEvent(
            contact,
            contact.getParentContactGroup(),
            SubscriptionEvent.SUBSCRIPTION_REMOVED);
    }


    public Contact findContactByID(String contactID)
    {
        return contactListRoot.findContactByID(contactID);
    }


    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        //FIXME
    }

    public String getCurrentStatusMessage()
    {
        return status.getStatusName();
    }

    public Contact createUnresolvedContact(String address,
        String persistentData, ContactGroup parent)
    {
        logger.info("createUnresolvedContact: " + address + " " + persistentData + " " + parent.getGroupName());
        
        ContactIrcImpl contact = new ContactIrcImpl(
            address
            , parentProvider);

        ( (ContactGroupIrcImpl) parent).addContact(contact);

        fireSubscriptionEvent(contact,
            parent,
            SubscriptionEvent.SUBSCRIPTION_CREATED);

        // Since contacts aren't stored on the server in IRC, we'll simply
        // resolve the contact ourselves as if we've just received an event
        // from the server telling us that it has been resolved.
        fireSubscriptionEvent(
            contact, contactListRoot, SubscriptionEvent.SUBSCRIPTION_RESOLVED);

        return contact;
    }

    public Contact createUnresolvedContact(String address, String persistentData)
    {
        logger.info("createUnresolvedContact: " + address + " " + persistentData);
        
        return createUnresolvedContact(address
            , persistentData
            , getServerStoredContactListRoot());
    }


    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        subscribe(this.contactListRoot, contactIdentifier);
    }


    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        logger.info("subscribe: " + parent.getGroupName() + " " + contactIdentifier);
        ContactIrcImpl contact = new ContactIrcImpl(
            contactIdentifier
            , parentProvider);

        ((ContactGroupIrcImpl)parent).addContact(contact);

        fireSubscriptionEvent(contact,
                              parent,
                              SubscriptionEvent.SUBSCRIPTION_CREATED);
        
        //FIXME: status

        //notify presence listeners for the status change.
        fireContactPresenceStatusChangeEvent(contact
                                             , parent
                                             , IrcStatusEnum.OFFLINE);
    }


    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
    {
        logger.info("createServerStoredContactGroup: " + parent.getGroupName() + " " + groupName);
        
        ContactGroupIrcImpl newGroup
            = new ContactGroupIrcImpl(groupName, parentProvider);

        ((ContactGroupIrcImpl)parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    }


    public void removeServerStoredContactGroup(ContactGroup group)
        throws IllegalArgumentException
    {
        logger.info("removeServerStoredContactGroup: " + group.getGroupName());
        
        ContactGroupIrcImpl ircGroup = (ContactGroupIrcImpl)group;

        ContactGroupIrcImpl parent = findGroupParent(ircGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(ircGroup);

        this.fireServerStoredGroupEvent(
            ircGroup, ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
    }

    public ContactGroupIrcImpl findGroupParent(
        ContactGroupIrcImpl ircGroup)
    {
        return contactListRoot.findGroupParent(ircGroup);
    }


    public void renameServerStoredContactGroup(ContactGroup group,
        String newName)
    {
        logger.info("renameServerStoredContactGroup: " + group.getGroupName() + " " + newName);
        
        ((ContactGroupIrcImpl)group).setGroupName(newName);
        
        this.fireServerStoredGroupEvent(
            group,
            ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
    }

    public ContactGroupIrcImpl findContactParent(ContactIrcImpl ircContact)
    {
        return (ContactGroupIrcImpl)ircContact
                    .getParentContactGroup();
    }

    public void moveContactToGroup(Contact contactToMove, ContactGroup newParent)
    {
        logger.info("moveContactToGroup: " + contactToMove.getAddress() + " " + newParent.getGroupName());
        
        ContactIrcImpl ircContact
            = (ContactIrcImpl)contactToMove;

        ContactGroupIrcImpl parentIrcGroup
            = findContactParent(ircContact);

        parentIrcGroup.removeContact(ircContact);

        //if this is a volatile contact then we haven't really subscribed to
        //them so we'd need to do so here
        if(!ircContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(ircContact
                                  , parentIrcGroup
                                  , SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                ((ContactGroupIrcImpl) newParent).addContact(ircContact);

                fireSubscriptionEvent(ircContact,
                                      newParent,
                                      SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                             + ircContact.getAddress()
                             , ex);
            }
        }
        else
        {
            ( (ContactGroupIrcImpl) newParent)
                    .addContact(ircContact);

            fireSubscriptionMovedEvent(contactToMove
                                      , parentIrcGroup
                                       , newParent);
        }
    }


    public ContactGroup getServerStoredContactListRoot()
    {
        return contactListRoot;
    }


    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        // If the parent is null then we're adding under root.

        if(parentGroup == null)
            parentGroup = getServerStoredContactListRoot();
        
        logger.info("createUnresolvedContactGroup: " + groupUID + " " + persistentData + " " + parentGroup.getGroupName());
        
        ContactGroupIrcImpl newGroup
            = new ContactGroupIrcImpl(
                ContactGroupIrcImpl.createNameFromUID(groupUID)
                , parentProvider);
    
        ((ContactGroupIrcImpl)parentGroup).addSubgroup(newGroup);
    
        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);
    
        return newGroup;
    }


}
