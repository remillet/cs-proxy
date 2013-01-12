package org.collectionspace.services.csproxy;

import org.collectionspace.services.client.CollectionObjectClient;
import org.collectionspace.services.client.CollectionObjectFactory;
import org.collectionspace.services.client.PoxPayloadOut;
import org.collectionspace.services.collectionobject.CollectionobjectsCommon;
import org.collectionspace.services.csproxy.domain.Customer;
import org.jboss.resteasy.client.ClientResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/customers")
public class Resource
{
   private static final Logger logger = LoggerFactory.getLogger(Resource.class);
   private Map<Integer, Customer> customerDB = new ConcurrentHashMap<Integer, Customer>();
   private AtomicInteger idCounter = new AtomicInteger();

   public Resource()
   {
   }

   @POST
   @Consumes("application/xml")
   public Response createCustomer(InputStream is)
   {
      Customer customer = readCustomer(is);
      customer.setId(idCounter.incrementAndGet());
      customerDB.put(customer.getId(), customer);
      System.out.println("Created customer " + customer.getId());
      return Response.created(URI.create("/customers/" + customer.getId())).build();

   }
   
	@GET
	@Path("ping")
	@Produces("text/plain")
	public String ping() {
		String result = null;
		
		CollectionObjectClient client = new CollectionObjectClient();
		CollectionobjectsCommon collectionObjectCommon = new CollectionobjectsCommon();
		collectionObjectCommon.setObjectNumber("Testing 1-2-3");
		PoxPayloadOut payload = CollectionObjectFactory.createCollectionObjectInstance(client.getCommonPartName(),
						collectionObjectCommon);
		ClientResponse<Response> res = client.create(payload);

		try {
			int statusCode = res.getStatus();
			if (logger.isDebugEnabled()) {
				logger.debug("ping" + ": HTTP status = " + statusCode);
			}
			if (statusCode == Response.Status.CREATED.getStatusCode()) {
				result = extractId(res);
			}
		} finally {
			res.releaseConnection();
		}

		return result;
	}
   
   @GET
   @Path("{id : \\d+}")
   @Produces("application/xml")
   public StreamingOutput getCustomer(@PathParam("id") int id)
   {
      final Customer customer = customerDB.get(id);
      if (customer == null)
      {
         throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
      return new StreamingOutput()
      {
         public void write(OutputStream outputStream) throws IOException, WebApplicationException
         {
            outputCustomer(outputStream, customer);
         }
      };
   }

   @GET
   @Path("{first : [a-zA-Z]+}-{last:[a-zA-Z]+}")
   @Produces("application/xml")
   public StreamingOutput getCustomerFirstLast(@PathParam("first") String first,
                                               @PathParam("last") String last)
   {
      Customer found = null;
      for (Customer cust : customerDB.values())
      {
         if (cust.getFirstName().equals(first) && cust.getLastName().equals(last))
         {
            found = cust;
            break;
         }
      }
      if (found == null)
      {
         throw new WebApplicationException(Response.Status.NOT_FOUND);
      }
      final Customer customer = found;
      return new StreamingOutput()
      {
         public void write(OutputStream outputStream) throws IOException, WebApplicationException
         {
            outputCustomer(outputStream, customer);
         }
      };
   }

   @PUT
   @Path("{id : \\d+}")
   @Consumes("application/xml")
   public void updateCustomer(@PathParam("id") int id, InputStream is)
   {
      Customer update = readCustomer(is);
      Customer current = customerDB.get(id);
      if (current == null) throw new WebApplicationException(Response.Status.NOT_FOUND);

      current.setFirstName(update.getFirstName());
      current.setLastName(update.getLastName());
      current.setStreet(update.getStreet());
      current.setState(update.getState());
      current.setZip(update.getZip());
      current.setCountry(update.getCountry());
   }


   protected void outputCustomer(OutputStream os, Customer cust) throws IOException
   {
      PrintStream writer = new PrintStream(os);
      writer.println("<customer id=\"" + cust.getId() + "\">");
      writer.println("   <first-name>" + cust.getFirstName() + "</first-name>");
      writer.println("   <last-name>" + cust.getLastName() + "</last-name>");
      writer.println("   <street>" + cust.getStreet() + "</street>");
      writer.println("   <city>" + cust.getCity() + "</city>");
      writer.println("   <state>" + cust.getState() + "</state>");
      writer.println("   <zip>" + cust.getZip() + "</zip>");
      writer.println("   <country>" + cust.getCountry() + "</country>");
      writer.println("</customer>");
   }

   protected Customer readCustomer(InputStream is)
   {
      try
      {
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(is);
         Element root = doc.getDocumentElement();
         Customer cust = new Customer();
         if (root.getAttribute("id") != null && !root.getAttribute("id").trim().equals(""))
            cust.setId(Integer.valueOf(root.getAttribute("id")));
         NodeList nodes = root.getChildNodes();
         for (int i = 0; i < nodes.getLength(); i++)
         {
            Element element = (Element) nodes.item(i);
            if (element.getTagName().equals("first-name"))
            {
               cust.setFirstName(element.getTextContent());
            }
            else if (element.getTagName().equals("last-name"))
            {
               cust.setLastName(element.getTextContent());
            }
            else if (element.getTagName().equals("street"))
            {
               cust.setStreet(element.getTextContent());
            }
            else if (element.getTagName().equals("city"))
            {
               cust.setCity(element.getTextContent());
            }
            else if (element.getTagName().equals("state"))
            {
               cust.setState(element.getTextContent());
            }
            else if (element.getTagName().equals("zip"))
            {
               cust.setZip(element.getTextContent());
            }
            else if (element.getTagName().equals("country"))
            {
               cust.setCountry(element.getTextContent());
            }
         }
         return cust;
      }
      catch (Exception e)
      {
         throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
      }
   }

   protected String extractId(ClientResponse<Response> res) {
       MultivaluedMap<String, Object> mvm = res.getMetadata();
       String uri = (String) ((List<Object>) mvm.get("Location")).get(0);
       if (logger.isDebugEnabled()) {
           logger.debug("extractId:uri=" + uri);
       }
       String[] segments = uri.split("/");
       String id = segments[segments.length - 1];
       if (logger.isDebugEnabled()) {
           logger.debug("id=" + id);
       }
       return id;
   }   
}
