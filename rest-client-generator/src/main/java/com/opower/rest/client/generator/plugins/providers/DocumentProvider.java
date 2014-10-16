package com.opower.rest.client.generator.plugins.providers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Provider that reads and writes org.w3c.dom.Document
 *
 * @author <a href="sduskis@gmail.com>Solomon Duskis</a>
 * @version $Revision: $
 */
@Provider
@Produces({"text/*+xml", "application/*+xml"})
@Consumes({"text/*+xml", "application/*+xml"})
public class DocumentProvider extends AbstractEntityProvider<Document>
{
   private static final Logger logger = LoggerFactory.getLogger(DocumentProvider.class);
   
   private final TransformerFactory transformerFactory;
   private final DocumentBuilderFactory documentBuilder;
   private final boolean expandEntityReferences;

   public DocumentProvider() {
       this(true);
   }

   public DocumentProvider(boolean expandEntityReferences)
   {
      this.documentBuilder = DocumentBuilderFactory.newInstance();
      this.transformerFactory = TransformerFactory.newInstance();
      this.expandEntityReferences = expandEntityReferences;
   }

   public boolean isReadable(Class<?> clazz, Type type,
                             Annotation[] annotation, MediaType mediaType)
   {
      return Document.class.isAssignableFrom(clazz);
   }

   public Document readFrom(Class<Document> clazz, Type type,
                            Annotation[] annotations, MediaType mediaType,
                            MultivaluedMap<String, String> headers, InputStream input)
           throws IOException, WebApplicationException
   {
      try
      {
         documentBuilder.setExpandEntityReferences(expandEntityReferences);
         return documentBuilder.newDocumentBuilder().parse(input);
      }
      catch (Exception e)
      {
         throw new RuntimeException("ReaderException: ", e);
      }
   }

   public boolean isWriteable(Class<?> clazz, Type type,
                              Annotation[] annotation, MediaType mediaType)
   {
      return Document.class.isAssignableFrom(clazz);
   }

   public void writeTo(Document document, Class<?> clazz, Type type,
                       Annotation[] annotation, MediaType mediaType,
                       MultivaluedMap<String, Object> headers, OutputStream output)
           throws IOException, WebApplicationException
   {
      try
      {
         DOMSource source = new DOMSource(document);
         StreamResult result = new StreamResult(output);
         transformerFactory.newTransformer().transform(source, result);
      }
      catch (TransformerException te)
      {
         throw new RuntimeException("WriterException: ", te);
      }
   }
}
