package edu.tamu.tcat.dex.psql.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.tamu.tcat.dex.trc.entry.DramaticExtractException;
import edu.tamu.tcat.dex.trc.entry.EditExtractCommand;
import edu.tamu.tcat.dex.trc.entry.ExtractRepository;
import edu.tamu.tcat.dex.trc.entry.Pair;

public class TestExtractRepository
{
   @Test
   public void testSaveEntry() throws InterruptedException, ExecutionException, DramaticExtractException, SAXException, IOException, ParserConfigurationException
   {
      ExtractRepository repo = ServiceHelper.getExtractRepository();
      // ensure search service is started and listening to repository
      ServiceHelper.getSearchService();

      EditExtractCommand editCommand = repo.create(UUID.randomUUID().toString());

      editCommand.setAuthor("Matthew J. Barry");
      editCommand.setManuscriptId("MJB_1234");
      editCommand.setSourceId("Shakespeare_Hamlet");
      editCommand.setSourceRef("3.1.64");

      Set<Pair<String, String>> speakers = new HashSet<>(Arrays.asList(Pair.of("Hamlet_Hamlet", "Hamlet")));
      editCommand.setSpeakers(speakers);

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder = dbf.newDocumentBuilder();

      String tei = "<div type=\"extract\" n=\"3.1.64\" corresp=\"#Shakespeare_Hamlet\"><sp who=\"Hamlet\"><l>To be or not to be—<choice><orig>that is the question:</orig><seg type=\"smartalec\">that's not a question</seg></choice></l></sp></div>";
      InputSource is = new InputSource(new StringReader(tei));
      Document document = documentBuilder.parse(is);

      editCommand.setTEIContent(document);

      Future<String> idFuture = editCommand.execute();
      String id = idFuture.get();

      assertNotNull("No ID returned", id);
   }

}
