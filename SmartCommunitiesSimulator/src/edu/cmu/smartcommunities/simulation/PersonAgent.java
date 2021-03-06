package edu.cmu.smartcommunities.simulation;

import edu.cmu.smartcommunities.jade.core.Agent;
import jade.core.AID;
import jade.core.ServiceException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
//import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class PersonAgent
   extends Agent
   {
   private              Date                  agendaModificationDateTime = null;
   private              Date                  agendaReportDateTime       = null;
   private static final String                className                  = PersonAgent.class.getName();
   public  static final String                agendaOntology             = className + ":Agenda";
   private        final DateFormat            dateTimeFormat             = new SimpleDateFormat(TimeAgent.iso8601DateTimeFormat);
   private              String                defaultWorkSpaceAgentName  = null;
   private        final String[]              meetingRoomAgentName       = new String[24 * 60];
// private              String                movementModelFile          = null; // TODO:  get this from a property file.  don't use faculty
   public  static final String                movementOntology           = className + ":Movement";
   private              double                percentMeetingTime         = 0;
   private              Calendar              preferredWorkStartDateTime = Calendar.getInstance();
   private              Calendar              preferredWorkStopDateTime  = Calendar.getInstance();
   private static final long                  serialVersionUID           = 9023344232278656873L;
   private              double                timeAllocatedToMeetings    = 0;
   private              TopicManagementHelper topicManagementHelper      = null;
   private              Calendar              workStartDateTime          = null;
   private              Calendar              workStopDateTime           = null;

   private String getDateTimeProperty(final String propertyName)
      {
      return extendedProperties.getProperty(propertyName, "2012-01-03 00:00:00");
      }

   private String getDefaultWorkSpaceAgentName(final int hour)
      {
      return hour < preferredWorkStartDateTime.get(Calendar.HOUR_OF_DAY) || hour >= preferredWorkStopDateTime.get(Calendar.HOUR_OF_DAY) ? null : defaultWorkSpaceAgentName;
      }

   protected void setup()
      {
      String dateTimeString = null;

      super.setup();
      try
         {
         defaultWorkSpaceAgentName = extendedProperties.getProperty("WorkSpaceName");
         percentMeetingTime = Double.parseDouble(extendedProperties.getProperty("PercentMeetingTime", "0.125"));
         preferredWorkStartDateTime.setTime(dateTimeFormat.parse(getDateTimeProperty("PreferredWorkStartTime")));
         preferredWorkStopDateTime.setTime(dateTimeFormat.parse(getDateTimeProperty("PreferredWorkStopTime")));
         topicManagementHelper = (TopicManagementHelper) getHelper(TopicManagementHelper.SERVICE_NAME);
         workStartDateTime = preferredWorkStartDateTime;
         workStopDateTime = preferredWorkStopDateTime;

         final String stochasticalMovementModelFileName = getProperty("StatisticalModelFileName", null);
         
         if (stochasticalMovementModelFileName == null)
            {
            addBehaviour(new ManageMeetingsBehaviour(this));
            }
         else
            {
            final StringTokenizer stringTokenizer       = new StringTokenizer(getProperty("MeetingRoomAgentNames", ""), ",");
            final String[]        meetingRoomAgentNames = new String[stringTokenizer.countTokens()];

            for (int i = 0; stringTokenizer.hasMoreTokens(); i++)
               {
               meetingRoomAgentNames[i] = stringTokenizer.nextToken().trim();
               }
            addBehaviour(new CreateAgendaAtMidnightBehaviour(this,
                                                             stochasticalMovementModelFileName,
                                                             getProperty("BreakRoomAgentName", "UnknownBreakRoom"),
                                                             meetingRoomAgentNames));
            }
         addBehaviour(new ReportAgendaBehaviour(this, 100, 3000));
         }
      catch (final ParseException parseException)
         {
         logger.error("Unable to interpret date/time:  " + dateTimeString,
                      parseException);
         }
      catch (final ServiceException serviceException)
         {
         logger.error("Unable to obtain the topic management helper.",
                      serviceException);
         }
      }

   private class CreateAgendaAtMidnightBehaviour
      extends CyclicBehaviour
      {
   // private        final String   defaultWorkSpaceAgentName    = "desk";
      private        final String   breakRoomAgentName;
   // private        final String[] meetingRoomAgentName        = new String[24 * 60];
      private        final String[] meetingRoomAgentNameArray;

      private        final MessageTemplate         messageTemplate  = MessageTemplate.MatchOntology(TimeAgent.timeSimulationOntology);
      private        final StochasticMovementModel model;
      private static final long                    serialVersionUID = 8741300705885943099L;

      public CreateAgendaAtMidnightBehaviour(final Agent    agent,
                                             final String   fileName,
                                             final String   breakRoomAgentName,
                                             final String[] meetingRoomAgentNameArray)
         {
         super(agent);
         this.breakRoomAgentName = breakRoomAgentName;
         model = new StochasticMovementModel(fileName);
         this.meetingRoomAgentNameArray = meetingRoomAgentNameArray;
         }
      
      public void action()
         {
         final ACLMessage inboundMessage = receive(messageTemplate);

         if (inboundMessage == null)
            {
            block();
            }
         else
            {
            try
               {
               final Calendar simulatedDateTime = (Calendar) inboundMessage.getContentObject();

               if (simulatedDateTime.get(Calendar.HOUR_OF_DAY) == 0 && simulatedDateTime.get(Calendar.MINUTE) == 0) // midnight
                  {
                  // S = Staff
                  // F = Faculty (don't use this it doesn't have sufficient data)
                  // P1 = PhD Student (Created with a lot of data)
                  // P2 = PhD Student (Created with less data)
                  // M = Masters Students

                  int                     dayOfMonth = 0;
                  WorkdayActivity         activity   = null;
               // StochasticMovementModel t1         = new StochasticMovementModel( a_s__args[0] );

                  model.initDay();
                  for (int minute = meetingRoomAgentName.length - 1; minute >= 0; minute--)
                     {
                     meetingRoomAgentName[minute] = null;
                     }
                  do
                     {
                     model.nextActivity();
                     System.out.println("Activity:  " + model.getActivity() + " at " + model.getActivityStartTime().getTime() + " for " + model.getActivityDuration() + " minutes");

                     final Calendar dateTime    = model.getActivityStartTime();
                     final int      beginMinute = dateTime.get(Calendar.HOUR_OF_DAY) * 60 + dateTime.get(Calendar.MINUTE);
                     final int      duration    = model.getActivityDuration();

                     switch (activity = model.getActivity())
                        {
                        case ARRIVAL:
                           {
                           // By default, the person will be at his/her desk until further notice.
                           setLocation(dateTime,
                                       dayOfMonth = dateTime.get(Calendar.DAY_OF_MONTH),
                                       beginMinute,
                                       meetingRoomAgentName.length,
                                       defaultWorkSpaceAgentName);
                           break;
                           }
                        case BREAK:
                        case BREAK_AND_RESTROOM:
                        case RESTROOM:
                           {
                           // Move to the break room.
                           setLocation(dateTime,
                                       dayOfMonth,
                                       beginMinute,
                                       beginMinute + duration,
                                       breakRoomAgentName);
                           break;
                           }
                        case DEPARTURE:
                           {
                           // Move to an unknown location for the balance of the day.
                           setLocation(dateTime,
                                       dayOfMonth,
                                       beginMinute,
                                       meetingRoomAgentName.length,
                                       null);
                           break;
                           }
                        case LEAVE_BUILDING:
                           {
                           // Move to an unknown location for the duration.
                           setLocation(dateTime,
                                       dayOfMonth,
                                       beginMinute,
                                       beginMinute + duration,
                                       null);
                           break;
                           }
                        case MEETING:
                        case VISIT_OTHER_WORK_AREA:
                           {
                           // Move to a randomly selected meeting room.  (Maybe someone else will be there, but no guarantees.)
                           setLocation(dateTime,
                                       dayOfMonth,
                                       beginMinute,
                                       beginMinute + duration,
                                       meetingRoomAgentNameArray[(int) (meetingRoomAgentNameArray.length * Math.random())]);
                           break;
                           }
                        default:
                           {
                           System.out.println("Not yet implemented");
                           }
                        }
                     }
                  while (activity != WorkdayActivity.DEPARTURE);
                  for (int minute = 0; minute < meetingRoomAgentName.length; minute++)
                     {
                     System.out.println("location[" + minute + "]:  " + meetingRoomAgentName[minute]);
                     }
                  }
               }
            catch (final UnreadableException unreadableException)
               {
               logger.error("Unable to get simulated time content object:  " + unreadableException.getMessage(),
                            unreadableException);
               }
            }
/*
            final Calendar dateTime = (Calendar) inboundMessage.getContentObject();

            if (dateTime.get(Calendar.HOUR_OF_DAY) == 0 && dateTime.get(Calendar.MINUTE) == 0) // midnight
               {
               model.initDay();
               for (int minute = 0; minute < meetingRoomAgentName.length; minute++)
                  {
                  meetingRoomAgentName[minute] = null;
                  }
               model.nextActivity(); // burn the "UNKNOWN" activity and get to the "ARRIVAL" activity

               Calendar startTime = model.getActivityStartTime();               

               model.nextActivity();

               Calendar endTime   = model.getActivityStartTime();

               // Report to workspace room (startTime to endTime) was spent at desk
               do
                  {          
                  startTime = model.getActivityStartTime();
                  switch (model.getActivity())
                     {
                     case ARRIVAL:
                        // pick the work location
                        break;
                     case BREAK:
                     case BREAK_AND_RESTROOM:
                     case RESTROOM:
                        // pick the kitchen
                        break;
                     case MEETING:
                     case VISIT_OTHER_WORK_AREA:
                        // pick a room at random
                        break;
                     case LEAVE_BUILDING:
                     default:
                        // leave the meeting room agent name null
                        break;
                     }           
                  int t = model.getActivityDuration();
                  //Report to room_selected_from_case person was there from startTime for t mins
                  startTime = startTime + t;
                  model.nextActivity();           
                  endTime = model.getActivityStartTime();
                  //Report to workspace room (startTime to endTime) was spent at desk
                  }
               while (model.getActivity() != WorkdayActivity.DEPARTURE);
               }
            }
*/
         }

      private void setLocation(final Calendar dateTime,
                               final int      dayOfMonth,
                               final int      beginMinute,
                               final int      endMinute,
                               final String   location)
         {
         if (dateTime.get(Calendar.DAY_OF_MONTH) == dayOfMonth)
            {
            final int safeEndMinute = Math.min(endMinute, meetingRoomAgentName.length);

            for (int minute = beginMinute; minute < safeEndMinute; minute++)
               {
               meetingRoomAgentName[minute] = location;
               }
            }
         }
      }

   private class ManageMeetingsBehaviour
      extends CyclicBehaviour
      {
      private              Calendar        dateTime         = null;
      private        final MessageTemplate messageTemplate  = MessageTemplate.MatchOntology(MeetingRoomAgent.meetingOntology);
      private static final long            serialVersionUID = -6090924341976674951L;

      public ManageMeetingsBehaviour(final Agent agent)
         {
         super(agent);

         AID meetingCreationTopic = null;

         try
            {
            meetingCreationTopic = topicManagementHelper.createTopic(MeetingRoomAgent.serviceType);
            topicManagementHelper.register(meetingCreationTopic);
            }
         catch (final ServiceException serviceException)
            {
            logger.error("Unable to subscribe to the meeting creation topic.",
                         serviceException);
            }
         }

      @Override
      public void action()
         {
         logger.trace("Begin ManageMeetingsBehaviour.action");

         final ACLMessage inboundMessage = receive(messageTemplate);

         if (inboundMessage == null)
            {
            block();
            }
         else
            {
            messagesReceived++;
            try
               {
               final Calendar meetingStartDateTime = (Calendar) inboundMessage.getContentObject();
               final int      meetingStartHour     = meetingStartDateTime.get(Calendar.HOUR_OF_DAY);
               final int      meetingLength        = 1; // TODO:  Make more flexible

               if (this.dateTime == null || (this.dateTime.get(Calendar.HOUR_OF_DAY) > dateTime.get(Calendar.HOUR_OF_DAY)))
                  {
                  workStartDateTime = preferredWorkStartDateTime; // TODO:  Add some randomness
                  workStopDateTime = preferredWorkStopDateTime; // TODO:  Add some randomness
                  for (int minute = 0; minute < meetingRoomAgentName.length; minute++)
                     {
                     meetingRoomAgentName[minute] = getDefaultWorkSpaceAgentName(minute / 60);
                     }
                  timeAllocatedToMeetings = 0;
                  this.dateTime = meetingStartDateTime;
                  }
               switch (inboundMessage.getPerformative())
                  {
                  case ACLMessage.CONFIRM:
                     {
                     final Calendar meetingStopDateTime = (Calendar) meetingStartDateTime.clone();

                     meetingStopDateTime.set(Calendar.HOUR_OF_DAY, meetingStopDateTime.get(Calendar.HOUR_OF_DAY) + meetingLength);
                     if (meetingStartDateTime.before(workStartDateTime))
                        {
                        workStartDateTime = meetingStartDateTime;
                        }
                     if (meetingStopDateTime.after(workStopDateTime))
                        {
                        workStopDateTime = meetingStopDateTime;
                        }
                     break;
                     }
                  case ACLMessage.DISCONFIRM:
                     {
                     for (int minute = 0; minute < 60; minute++)
                        {
                        meetingRoomAgentName[meetingStartHour * 60 + minute] = getDefaultWorkSpaceAgentName(meetingStartHour);
                        }
                     timeAllocatedToMeetings -= meetingLength;
                     break;
                     }
                  case ACLMessage.PROPOSE:
                     {
                     final ACLMessage outboundMessage               = inboundMessage.createReply();
                           int        performative                  = ACLMessage.REJECT_PROPOSAL;
                     final String     scheduledMeetingRoomAgentName = meetingRoomAgentName[meetingStartHour * 60];

                     outboundMessage.setContentObject(meetingStartDateTime);
                     outboundMessage.setOntology(inboundMessage.getOntology());
                     if (scheduledMeetingRoomAgentName == null || scheduledMeetingRoomAgentName.equals(defaultWorkSpaceAgentName))
                        {
                        if (timeAllocatedToMeetings / 8.0 < percentMeetingTime)
                           {
                           if (Math.random() < percentMeetingTime)
                              {
                              for (int minute = 0; minute < 60; minute++)
                                 {
                                 meetingRoomAgentName[meetingStartHour * 60 + minute] = inboundMessage.getSender().getLocalName();
                                 }
                              performative = ACLMessage.ACCEPT_PROPOSAL;
                              timeAllocatedToMeetings += meetingLength;
                              }
                           }
                        }
                     outboundMessage.setPerformative(performative);
                     send(outboundMessage);
                     messagesSent++;
                     break;
                     }
                  default:
                     {
                     logger.warn("Unexpected message performative:  " + ACLMessage.getPerformative(inboundMessage.getPerformative()));
                     }
                  }
               agendaModificationDateTime = new Date();
               }
            catch (final IOException ioException)
               {
               logger.error("Unable to set simulated date time content object:  " + ioException.getMessage(),
                            ioException);
               }
            catch (final UnreadableException unreadableException)
               {
               logger.error("Unable to get simulated time content object:  " + unreadableException.getMessage(),
                            unreadableException);
               }
            }
         logger.trace("End   ManageMeetingsBehaviour.action");
         }
      }
/*
   private class MoveBehaviour
      extends CyclicBehaviour
      {
      protected              Calendar        dateTime         = null;
      private          final MessageTemplate messageTemplate  = MessageTemplate.MatchOntology(TimeAgent.timeSimulationOntology);
      private   static final long            serialVersionUID = 1833830971141389258L;

      public MoveBehaviour(final Agent agent)
         {
         super(agent);

         AID timeSimulationTopic = null;

         try
            {
            timeSimulationTopic = topicManagementHelper.createTopic(TimeAgent.serviceType);
            topicManagementHelper.register(timeSimulationTopic);
            }
         catch (final ServiceException serviceException)
            {
            logger.error("Unable to subscribe to the time simulation topic.",
                         serviceException);
            }
         }

      @Override
      public void action()
         {
         logger.trace("Begin MoveBehaviour.action");

         final ACLMessage inboundMessage = receive(messageTemplate);

         if (inboundMessage == null)
            {
            block();
            }
         else
            {
            messagesReceived++;
            try
               {
               final Calendar   dateTime        = (Calendar) inboundMessage.getContentObject();
               final ACLMessage outboundMessage = createOutboundMessage(dateTime);

               if (outboundMessage != null)
                  {
                  logger.debug("Sending movement");
                  outboundMessage.setOntology(movementOntology);
                  send(outboundMessage);
                  messagesSent++;
                  }
               this.dateTime = dateTime;
               }
            catch (final IOException ioException)
               {
               logger.error("Unable to set simulated time content:  " + ioException.getMessage(),
                            ioException);
               }
            catch (final UnreadableException unreadableException)
               {
               logger.error("Unable to get simulated time content:  " + unreadableException.getMessage(),
                            unreadableException);
               }
            }
         logger.trace("End   MoveBehaviour.action");
         }

      protected ACLMessage createOutboundMessage(final Calendar dateTime)
         throws IOException
         {
         final ACLMessage aclMessage;

         if (this.dateTime == null || (this.dateTime.get(Calendar.HOUR_OF_DAY) > dateTime.get(Calendar.HOUR_OF_DAY)))
            {
            workStartDateTime = preferredWorkStartDateTime; // TODO:  Add some randomness
            workStopDateTime = preferredWorkStopDateTime; // TODO:  Add some randomness
            for (int minute = 0; minute < meetingRoomAgentName.length; minute++)
               {
               meetingRoomAgentName[minute] = getDefaultWorkSpaceAgentName(minute / 60);
               }
            timeAllocatedToMeetings = 0;
            aclMessage = null;
            }
         else
            {
            final int hour   = dateTime.get(Calendar.HOUR_OF_DAY);
            final int minute = hour * 60 + dateTime.get(Calendar.MINUTE);

            if (this.dateTime.get(Calendar.HOUR_OF_DAY) == 0 && hour == 1) // 01:00
               {
               synchronized (PersonAgent.class)
                  {
                  for (int i = 0; i < meetingRoomAgentName.length; i++)
                     {
                     logger.debug("At minute " + i + ", scheduled for " + meetingRoomAgentName[i]);
                     }
                  }
               }
            
            final int        previousMinute             = (minute + meetingRoomAgentName.length - 1) % meetingRoomAgentName.length;
            final String     enteringWorkSpaceAgentName = meetingRoomAgentName[minute];
            final String     leavingWorkSpaceAgentName  = meetingRoomAgentName[previousMinute];
                  boolean    messageNeedsToBeSent       = false;
            final ACLMessage outboundMessage            = new ACLMessage(ACLMessage.INFORM);

            if (leavingWorkSpaceAgentName != null)
               {
               messageNeedsToBeSent = true;
               outboundMessage.addReceiver(new AID(leavingWorkSpaceAgentName, AID.ISLOCALNAME));
               }
            if (enteringWorkSpaceAgentName != null)
               {
               messageNeedsToBeSent = true;
               if (!enteringWorkSpaceAgentName.equals(leavingWorkSpaceAgentName))
                  {
                  outboundMessage.addReceiver(new AID(enteringWorkSpaceAgentName, AID.ISLOCALNAME));
                  }
               }
            if (messageNeedsToBeSent)
               {
               outboundMessage.setContentObject(new Movement(dateTime,
                                                             enteringWorkSpaceAgentName,
                                                             leavingWorkSpaceAgentName));
               aclMessage = outboundMessage;
               }
            else
               {
               aclMessage = null;
               }
            }
         // TODO:  lunch
         return aclMessage;
         }
      }

   private class StochasticMovementBehaviour
      extends MoveBehaviour
      {
      private              StochasticMovementModel movementModel      = null;
      private              Calendar                timeOfNextActStart = null;
      private              Calendar                timeOfNextActEnd   = null;
      private static final long                    serialVersionUID   = 7877528228852209982L;

      public StochasticMovementBehaviour(final Agent agent)
         {
         super(agent);
         movementModel = new StochasticMovementModel(movementModelFile);
         }

      protected ACLMessage createOutboundMessage(final Calendar dateTime)
         throws IOException
         {
         final ACLMessage aclMessage = null; // needs to be something non-null

         if (this.dateTime == null || (this.dateTime.get(Calendar.HOUR_OF_DAY) > dateTime.get(Calendar.HOUR_OF_DAY)))
            {
            movementModel.initDay(dateTime);
            workStartDateTime = movementModel.getWorkStartTime();
            workStopDateTime = movementModel.getWorkEndTime();
            }
         else
            {
            if ((timeOfNextActEnd == null) || (timeOfNextActEnd.compareTo(dateTime) >= 0))
               {
               this.movementModel.nextActivity();
               this.timeOfNextActStart = this.movementModel.getActivityStartTime();
               int duration = this.movementModel.getActivityDuration();
               this.timeOfNextActEnd = (Calendar) this.timeOfNextActStart.clone();
               this.timeOfNextActEnd.add(Calendar.MINUTE, duration);
               }
            // need to retain previous activity.
            if (timeOfNextActStart.compareTo(dateTime) < 0)
               {
               // entering workspace = home work space
               }
            else
               {
               // entering workspace is dependent on type of activity (movementModel.getActivity())
               }
            }
         // send messages
         return aclMessage;
         }
      }
*/
   private class ReportAgendaBehaviour
      extends TickerBehaviour
      {
      private        final long minimumQuiesenceTime;
      private static final char one                  = '1';
      private static final long serialVersionUID     = -3732074322999794346L;
      private static final char zero                 = '0';

      public ReportAgendaBehaviour(final Agent agent,
                                   final long  period,
                                   final long  minimumQuiesenceTime)
         {
         super(agent,
               period);
         this.minimumQuiesenceTime = minimumQuiesenceTime;
         }

      @Override
      protected void onTick()
         {
         if (agendaModificationDateTime != null)
            {
            if ((agendaReportDateTime == null) ||
                (agendaReportDateTime.before(agendaModificationDateTime)))
               {
               final Date currentDateTime = new Date();

               if (currentDateTime.getTime() - agendaModificationDateTime.getTime() >= minimumQuiesenceTime)
                  {
                  final Map<String, char[]> localityOccupancyMap = new HashMap<>();

                  synchronized (PersonAgent.class){
                  for (int minute = 0; minute < meetingRoomAgentName.length; minute++)
                     {
                     final String localityAgentName = meetingRoomAgentName[minute];

                     if (localityAgentName != null)
                        {
                        final boolean localityIsKnown = localityOccupancyMap.containsKey(localityAgentName);
                        final char[]  occupied;

                        if (localityIsKnown)
                           {
                           occupied = localityOccupancyMap.get(localityAgentName);
                           }
                        else
                           {
                           occupied = new char[24 * 60];
                           for (int i = 0; i < meetingRoomAgentName.length; i++)
                              {
                              occupied[i] = zero;
                              }
                           }
                        occupied[minute] = one;
                        localityOccupancyMap.put(localityAgentName, occupied);
                     // logger.debug("agenda Map:  " + new String(occupied));
                        }
                     }
                  for (String localityAgentLocalName:  localityOccupancyMap.keySet())
                     {
                     final ACLMessage outboundMessage = new ACLMessage(ACLMessage.INFORM);

                     outboundMessage.addReceiver(new AID(localityAgentLocalName, AID.ISLOCALNAME));
                     outboundMessage.setContent(new String(localityOccupancyMap.get(localityAgentLocalName)));
                     outboundMessage.setOntology(agendaOntology);
                     send(outboundMessage);
                     messagesSent++;
                     logger.debug("Sending agenda to " + localityAgentLocalName + ": " + outboundMessage.getContent());
                     }}
                  agendaReportDateTime = currentDateTime;
                  }
               }
            }
         }
      }
/*
   private static class Movement
      implements Serializable
      {
      public         final Calendar dateTime;
      public         final String   enteringRoomAgentName;
      public         final String   leavingRoomAgentName;
      private static final long     serialVersionUID      = -4350548970268564453L;

      public Movement(final Calendar dateTime,
                      final String   enteringRoomAgentName,
                      final String   leavingRoomAgentName)
         {
         this.dateTime = dateTime;
         this.enteringRoomAgentName = enteringRoomAgentName;
         this.leavingRoomAgentName = leavingRoomAgentName;
         }
      }
*/
   }
