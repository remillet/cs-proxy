package org.collectionspace.services.csproxy;

import javax.ws.rs.core.Application;

import org.collectionspace.services.csproxy.Resource;

import java.util.HashSet;
import java.util.Set;

public class CSProxyApplication extends Application
{
   private Set<Object> singletons = new HashSet<Object>();
   private Set<Class<?>> empty = new HashSet<Class<?>>();

   public CSProxyApplication()
   {
      singletons.add(new Resource());
   }

   @Override
   public Set<Class<?>> getClasses()
   {
      return empty;
   }

   @Override
   public Set<Object> getSingletons()
   {
      return singletons;
   }
}
