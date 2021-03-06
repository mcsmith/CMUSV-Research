package edu.cmu.smartcommunities.simulation;

import edu.cmu.smartcommunities.jade.core.Agent;
import jade.core.AID;
import jade.core.ServiceException;
import jade.core.behaviours.TickerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
// import java.util.Date;
// import java.util.GregorianCalendar;

public class TimeAgent
   extends Agent
   {
   private static final String     className                = TimeAgent.class.getName();
   private              long       clockTickInterval        = 0;
   private        final DateFormat dateFormat               = new SimpleDateFormat(iso8601DateTimeFormat);          
   public  static final String     iso8601DateTimeFormat    = "yyyy-MM-dd HH:mm:ss";
   private              int        millisecondsPerClockTick = 0;
   private static final long       serialVersionUID         = 3175025500104404418L;
   public  static final String     serviceType              = "TimeSimulation";
// private              long       simulatedTime            = 0;
   private        final Calendar   simulatedDateTime        = Calendar.getInstance();
   public  static final String     timeSimulationOntology   = className + ":" + serviceType;
   private              AID        timeSimulationTopic      = null;

   @Override
   protected void setup()
      {
      String startDateTimeString = "2012-01-03 00:00:00";

      super.setup();
      try
         {
         clockTickInterval = extendedProperties.getIntProperty("ClockTickInterval", 1000);
         millisecondsPerClockTick = extendedProperties.getIntProperty("MillisecondsPerClockTick", 1000);
         startDateTimeString = extendedProperties.getProperty("StartDateTime", startDateTimeString);
         simulatedDateTime.setTime(dateFormat.parse(startDateTimeString));
      // simulatedTime = new SimpleDateFormat(iso8601DateTimeFormat).parse(startDateTimeString).getTime();
         timeSimulationTopic = ((TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME)).createTopic(serviceType);
         addBehaviour(new SimulateTimePassageBehaviour(this, clockTickInterval));
         }
      catch (final ParseException parseException)
         {
         logger.error("Unable to parse start time of '" + startDateTimeString + "'.",
                      parseException);
         }
      catch (final ServiceException serviceException)
         {
         logger.error("Unable to create " + serviceType + " topic.",
                      serviceException);
         }
      }

   private class SimulateTimePassageBehaviour
      extends TickerBehaviour
      {
      private              int  debugMessagesSent = 0;
      private static final long serialVersionUID = 175622771312000339L;

      public SimulateTimePassageBehaviour(final Agent agent,
                                          final long  period)
         {
         super(agent,
               period);
         }

      @Override
      protected void onTick()
         {
         try
            {
            if (messagesSent < (1 * 24 * 60) - 1)
               {
            final ACLMessage outboundMessage   = new ACLMessage(ACLMessage.INFORM);
         // final Date       simulatedDateTime = new Date();

         // simulatedDateTime.setTime(simulatedTime);
            logger.debug("Sending message " + debugMessagesSent++ + " representing " + dateFormat.format(simulatedDateTime.getTime()));
         // simulatedTime += millisecondsPerClockTick;
            simulatedDateTime.add(Calendar.MILLISECOND, millisecondsPerClockTick);
            outboundMessage.addReceiver(timeSimulationTopic);
            outboundMessage.setContentObject(simulatedDateTime);
            outboundMessage.setOntology(timeSimulationOntology);
            send(outboundMessage);
            messagesSent++;
               }
            }
         catch (final IOException ioException)
            {
            logger.error("Unable to set simulated date time content object:  " + ioException.getMessage(),
                         ioException);
            }
         }
      }
   }
