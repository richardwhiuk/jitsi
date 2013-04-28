/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ircaccregwizz;

import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Registers the <tt>IrcAccountRegistrationWizard</tt> in the UI Service.
 *
 * @author Lionel Ferreira & Michael Tarantino
 */
public class IrcAccRegWizzActivator
    extends AbstractServiceDependentActivator
{
    private static Logger logger = Logger.getLogger(
        IrcAccRegWizzActivator.class.getName());

    /**
     * A currently valid bundle context.
     */
    public static BundleContext bundleContext;

    /**
     * The <tt>UIService</tt>.
     */
    public static UIService uiService;

    private static WizardContainer wizardContainer;

    private static IrcAccountRegistrationWizard ircWizard;

    /**
     * Starts this bundle.
     * @param o a reference to the dependant service - the <tt>UIService</tt>
     */
    public void start(Object dependentService)
    {
	uiService = (UIService)dependentService;

        if (logger.isInfoEnabled())
            logger.info("Loading irc account wizard.");

	wizardContainer = uiService.getAccountRegWizardContainer();

	ircWizard = new IrcAccountRegistrationWizard(wizardContainer);

        Hashtable<String, String> containerFilter
            = new Hashtable<String, String>();

        containerFilter.put(
                ProtocolProviderFactory.PROTOCOL,
                ProtocolNames.IRC);

        bundleContext.registerService(
            AccountRegistrationWizard.class.getName(),
            ircWizard,
            containerFilter);

        if (logger.isInfoEnabled())
            logger.info("IRC account registration wizard [STARTED].");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context)
       throws Exception
    {
    }

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    /**
     * Returns the <tt>ProtocolProviderFactory</tt> for the IRC protocol.
     * @return the <tt>ProtocolProviderFactory</tt> for the IRC protocol
     */
    public static ProtocolProviderFactory getIrcProtocolProviderFactory()
    {
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + ProtocolProviderFactory.PROTOCOL
            + "=" + ProtocolNames.IRC + ")";

        try
        {
            serRefs = bundleContext.getServiceReferences(
                ProtocolProviderFactory.class.getName(), osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            logger.error("IrcAccRegWizzActivator : " + ex);
        }

        return (ProtocolProviderFactory) bundleContext.getService(serRefs[0]);
    }

    /**
     * Sets the bundleContext
     *
     * @param bc the BundleContext
     */
    public void setBundleContext(BundleContext bc)
    {
        bundleContext = bc;
    }

    /**
     * Returns the bundleContext that we received when we were started.
     *
     * @return a currently valid instance of a bundleContext.
     */
    public BundleContext getBundleContext()
    {
        return bundleContext;
    }

    /**
     * Returns the <tt>UIService</tt>.
     *
     * @return the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        return uiService;
    }

}
