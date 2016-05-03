package org.aktin.report.manager;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.MimeConstants;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.aktin.report.Report;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.utils.URI;

/**
 * Generates a Report from the Rscript output files
 * at the desired location.
 * <p>
 * Implementations will generate a PDF document or
 * HTML with image files in specified directory.
 * <p>
 * See also  https://xmlgraphics.apache.org/fop/1.1/embedding.html
 * 
 * @author R.W.Majeed
 *
 */
class PDFGenerator {

	
	/**
	 * Generate a PDF report. After return of the function, no additional files
	 * should remain in {@code workingPath}.
	 * 
	 * @param workingPath path containing the data files generated from R
	 * @param report report to use
	 * @param pdf PDF output location
	 * @throws IOException IO error. {@code workingPath} looks as if 
	 * this function was never called.
	 */
	//ToDo: SVG/Image-Problems when built via maven?
	//http://stackoverflow.com/questions/31386864/apache-fop-2-0-why-is-no-imagepreloader-found-for-svg
	public void generatePDF(Path workingPath, Report report, Path pdf) throws IOException{
		String[] files = report.copyResourcesForFOP(workingPath);
		// invoke Apache FOP
		// clean up afterwards
		System.out.println(new File("File: "+files[0]) +"; readable: "+ new File(files[0]).canRead()+"; Length: "+new File(files[0]).length());
		System.out.println(new File("File: "+files[1]) +"; readable: "+ new File(files[1]).canRead()+"; Length: "+new File(files[1]).length());
		

		// Step 1: Construct a FopFactory
		// (reuse if you plan to render multiple documents!)
		//Config File: https://xmlgraphics.apache.org/fop/2.0/configuration.html
		FopFactory fopFactory;
		try {
			//svgs tend to cause path problems if the baseDir is not set correctly
			fopFactory = FopFactory.newInstance(workingPath.resolve("dummy.xconf").toFile()); //ToDo this should maybe be included in the Report Interface to allow for configuration
			
			// Step 2: Set up output stream.
			// Note: Using BufferedOutputStream for performance reasons (helpful with FileOutputStreams).
			//OutputStream out = new BufferedOutputStream(new FileOutputStream(new File("C:\\temp\\myfile.pdf")));
			//OutputStream out = new BufferedOutputStream(new FileOutputStream(workingPath.resolve("myfile.pdf").toFile()));
			OutputStream out = new BufferedOutputStream(new FileOutputStream(pdf.toFile()));

			try {
			    // Step 3: Construct fop with desired output format
			    Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);			   

			    // Step 4: Setup JAXP using identity transformer
			    TransformerFactory factory = TransformerFactory.newInstance();
			    //Transformer transformer = factory.newTransformer(); // identity transformer
			    Transformer transformer = factory.newTransformer(new StreamSource(new File(files[1]))); //Second file from Report interface is the XSL file

			    // Step 5: Setup input and output for XSLT transformation
			    // Setup input stream
			    Source src = new StreamSource(new File(files[0])); //First file from Report interface is the XML input (Source)

			    // Resulting SAX events (the generated FO) must be piped through to FOP
			    Result res = new SAXResult(fop.getDefaultHandler());

			    // Step 6: Start XSLT transformation and FOP processing
			    transformer.transform(src, res);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
			    //Clean-up
			    out.close();
			}
		} catch (SAXException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}  

		
		
			
		//throw new UnsupportedOperationException("TODO implement");
	}
	
	 private static class LocalResolver implements URIResolver {
         private String BaseFolder; 
            @Override
            public Source resolve(String href, String base) throws TransformerException {
             File f = new File(BaseFolder + "\\" + href);
             System.out.println(f);
             if (f.exists())
             return new StreamSource(f);
                     else
                      throw new TransformerException("File " + f.getAbsolutePath() +" not found!");         
            }

         public LocalResolver(String BaseFolder) {
           this.BaseFolder = BaseFolder;   
         }

     }
}
