package edu.cmu.smartcommunities.database.controller;

import edu.cmu.smartcommunities.database.model.Locality;
import edu.cmu.smartcommunities.database.model.Measurement;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class LocalityManager
   {
   private static final Set<Locality> localitySet = new HashSet<>();

   // Begin workaround data
   private static final String        fileName        = "/home/mcsmith/Locality.data";
   private static       Locality      root            = null;
   private static final boolean       usingWorkaround = true;
   // End   workaround data

   public Locality getLocality(final String fullyQualifiedName)
      {
      if (localitySet.size() == 0)
         {
         loadLocalities();
         }
      return getLocality(new StringTokenizer(fullyQualifiedName, "."),
                         localitySet);
      }

   private Locality getLocality(final StringTokenizer stringTokenizer,
                                final Set<Locality>   localitySet)
      {
      if (stringTokenizer.hasMoreTokens())
         {
         final String name = stringTokenizer.nextToken();

         for (Locality locality:  localitySet)
            {
            if (name.equals(locality.getName()))
               {
               if (stringTokenizer.hasMoreTokens())
                  {
                  return getLocality(stringTokenizer,
                                     locality.getChildLocalitySet());
                  }
               else
                  {
                  return locality;
                  }
               }
            }
         }
      return null;
      }

   public Measurement getMeasurement(final Locality locality,
                                     final Date     dateTime)
      {
      final GetMeasurementBusinessTransaction businessTransaction = new GetMeasurementBusinessTransaction(locality,
                                                                                                          dateTime);

      if (usingWorkaround)
         {
         synchronized (root)
            {
            businessTransaction.execute();
            writeToDisc();
            }
         }
      else
         {
      // HibernateUtil.executeBusinessTransaction(businessTransaction);
         }
      return businessTransaction.measurement;
      }

   public Locality loadLocalities()
      {
      if (usingWorkaround)
         {
         readFromDisc();
         return root;
         }
      else
         {
         final LoadLocalitiesBusinessTransaction businessTransaction = new LoadLocalitiesBusinessTransaction();

      // HibernateUtil.executeBusinessTransaction(businessTransaction);
         return businessTransaction.locality;
         }
      }

   private void readFromDisc()
      {
      try
         {
         File        file                = new File(fileName);
         InputStream fileInputStream     = new FileInputStream(file);
         InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
         ObjectInput objectInputStream   = new ObjectInputStream(bufferedInputStream);

         try
            {
            System.out.println("Reading from " + file.getCanonicalPath());
            root = (Locality) objectInputStream.readObject();
            localitySet.add(root);
            }
         catch (ClassNotFoundException e)
            {
            e.printStackTrace();
            }
         finally
            {
            objectInputStream.close();
            bufferedInputStream.close();
            fileInputStream.close();
            }
         }
      catch (FileNotFoundException e)
         {
         e.printStackTrace();
         }
      catch (IOException e)
         {
         e.printStackTrace();
         }
      }

   public void setLocalOccupancy(final Locality locality,
                                 final Date     dateTime,
                                 final int      localOccupancy)
      {
      final Measurement                          measurement         = getMeasurement(locality,
                                                                                      dateTime);
      final SetLocalOccupancyBusinessTransaction businessTransaction = new SetLocalOccupancyBusinessTransaction(measurement,
                                                                                                                localOccupancy);

      if (usingWorkaround)
         {
         synchronized (root)
            {
            businessTransaction.execute();
            writeToDisc();
            }
         }
      else
         {
      // HibernateUtil.executeBusinessTransaction(businessTransaction);
         }
      }

   private void writeToDisc()
      {
      try
         {
         File         file                 = new File(fileName);
         OutputStream fileOutputStream     = new FileOutputStream(file);
         OutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
         ObjectOutput objectOutputStream   = new ObjectOutputStream(bufferedOutputStream);

         try
            {
            objectOutputStream.writeObject(root);
            }
         finally
            {
            objectOutputStream.close();
            bufferedOutputStream.close();
            fileOutputStream.close();
            }
         }
      catch (final IOException ioException)
         {
         ioException.printStackTrace();
         }
      }

   private class GetMeasurementBusinessTransaction
      implements BusinessTransactionInterface
      {
      final Date        dateTime;
      final Locality    locality;
            Measurement measurement = null;

      public GetMeasurementBusinessTransaction(final Locality locality,
                                               final Date     dateTime)
         {
         this.dateTime = dateTime;
         this.locality = locality;
         }

      @Override
      public void execute()
         {
         final Set<Measurement> measurementSet = locality.getMeasurementSet();

         for (Measurement measurement:  measurementSet)
            {
            if (measurement.getMeasurementDateTime().equals(dateTime))
               {
               this.measurement = measurement;
               }
            }
         if (measurement == null)
            {
            measurement = new Measurement();
            measurement.setLocality(locality);
            measurement.setMeasurementDateTime(dateTime);
            measurementSet.add(measurement);
            }
         }
      }

   private class LoadLocalitiesBusinessTransaction
      implements BusinessTransactionInterface
      {
      private Locality locality = null;

      @Override
      public void execute()
         {
      // locality = findById(1L, false);
         loadChildLocalities(locality);
         localitySet.add(locality);
         }

      private void loadChildLocalities(final Locality locality)
         {
         for (Locality childLocality:  locality.getChildLocalitySet())
            {
            loadChildLocalities(childLocality);
            }
         }
      }

   private class SetLocalOccupancyBusinessTransaction
      implements BusinessTransactionInterface
      {
      private final int         localOccupancy;
      private final Measurement measurement;

      public SetLocalOccupancyBusinessTransaction(final Measurement measurement,
                                                  final int         localOccupancy)
         {
         this.localOccupancy = localOccupancy;
         this.measurement = measurement;
         }

      @Override
      public void execute()
         {
         measurement.setOccupancy(localOccupancy);
         measurement.setWatts(localOccupancy == 0 ? 0 : measurement.getLocality().getWatts());
         }
      }
   }
