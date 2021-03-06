package edu.cmu.smartcommunities.jade.core;

import jade.util.ExtendedProperties;
// import java.lang.management.ManagementFactory;
// import java.lang.management.ThreadMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Agent
   extends jade.core.Agent
   {
   protected        final ExtendedProperties extendedProperties = new ExtendedProperties();
   protected        final Logger             logger             = LoggerFactory.getLogger(getClass());
   protected              long               messagesReceived   = 0;
   protected              long               messagesSent       = 0;
   private   static final long               serialVersionUID   = -1075797089153518750L;

   protected Agent()
      {
      logger.trace("Begin <init>");
/*
      final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
      
      if (threadMXBean.isThreadCpuTimeSupported())
         {
         if (!threadMXBean.isThreadCpuTimeEnabled())
            {
            logger.warn("Thread CPU time is not enabled.");
            }
         }
      else
         {
         logger.warn("Thread CPU time is not supported.");
         }
*/
      logger.trace("End   <init>");
      }
/*
   private void logCpuTime(final boolean startOfTransaction)
      {
      logger.debug((startOfTransaction ? "Start:  " : "End:  ") + ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime());
      }
*/
   public void setExtendedProperties(final String[] arguments)
      {
      logger.trace("Begin setExtendedProperties");
      extendedProperties.addProperties(arguments);
      for (Object object:  extendedProperties.keySet())
         {
         logger.debug("Property name:  " + object.toString());
         logger.debug("Property value: " + extendedProperties.get(object).toString());
         }
      logger.trace("End   setExtendedProperties");
      }

   @Override
   protected void setup()
      {
      logger.trace("Begin setup");
      super.setup();

      final Object[] arguments = getArguments();

      if (arguments != null)
         {
         final int    extendedPropertyCount = arguments.length;
         final String extendedProperty[]    = new String[extendedPropertyCount];

         for (int i = extendedPropertyCount - 1; i >= 0; i--)
            {
            extendedProperty[i] = arguments[i].toString();
            }
         setExtendedProperties(extendedProperty);
         }
      logger.trace("End   setup");
      }

   @Override
   protected void takeDown()
      {
      logger.info("Messages received:  " + messagesReceived + ", messages sent:  " + messagesSent);
      }
   }
