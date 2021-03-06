package com.bridgelabz.webscraping.service;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bridgelabz.webscraping.exception.InvalidUser;
import com.bridgelabz.webscraping.message.MessageData;
import com.bridgelabz.webscraping.model.User;
import com.bridgelabz.webscraping.repository.UserRepository;
import com.bridgelabz.webscraping.response.Response;
import com.bridgelabz.webscraping.utility.JwtToken;
import com.sun.istack.logging.Logger;

/**
 * Purpose :- This class implements all methods of interface. which performs
 * CRUD of UserScrappedSiteServiceImp in database
 * 
 * @author Krunal Parate
 * @since 14-08-2020
 */

@Service
public class UserScrappedSiteServiceImp implements IUserScrappedSiteService {
	@Autowired
	UserRepository userRepository;

	@Autowired
	Response response;

	@Autowired
	private MessageData messageData;

	@Autowired
	private JwtToken jwtToken;
	
	@Autowired
	private ModelMapper mapper;

	private static final Logger LOGGER = Logger.getLogger(UserServiceImp.class);

	/**
	 * Scraping websites using Jsoup and inserting data into databases using PdfBox
	 * (e.g .pdf, .html and .csv format)
	 */
	@Override
	public Response addScrappedSite(String token, String url, String format) throws Exception {
		String email = jwtToken.getToken(token);
		User user = userRepository.findByEmail(email);
		// Check if user is present or not
		if (user == null) {
			LOGGER.warning("Invalid user");
			throw new InvalidUser(messageData.Invalid_User);
		}
		Document doc = Jsoup.connect(url).get();
		StringJoiner joiner = new StringJoiner(",");
		for (Element table : doc.getElementsByTag("table")) {
			for (Element trElement : table.getElementsByTag("tr")) {
				for (Element tdElement : trElement.getElementsByTag("th")) {
					joiner.add(tdElement.text());
				}
				for (Element element : trElement.getElementsByTag("td")) {
					joiner.add(element.text());
				}
			}
		}
		System.out.println(joiner.toString());
		// A new PDDocument is created. By default, the document has an A4 format.
		PDDocument document = null;
		try {
			document = new PDDocument();
			// A new page is created and added to the document
			PDPage page = new PDPage();
			document.addPage(page);
			// To write to a PDF page, we have to create a PDPageContentStream object
			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			PDFont pdfFont = PDType1Font.TIMES_ITALIC;
			float fontSize = 11;
			float leading = 1.5f * fontSize;

			PDRectangle mediabox = page.getMediaBox();
			float marginX = 80;
			float marginY = 60;
			float width = mediabox.getWidth() - 2 * marginX;
			float startX = mediabox.getLowerLeftX() + marginX;
			float startY = mediabox.getUpperRightY() - marginY;

			// The text is written with showText() method
//			String text = joiner.toString();
			String text = "dflgkjjfdjfgbfdjhjxdzjfhgfdhfghdfhgdfhghdfhghdsdfjhgfjldlkhgjlhk";
			List<String> lines = new ArrayList<String>();
			int lastSpace = -1;
			while (text.length() > 0) {
				int spaceIndex = text.indexOf(' ', lastSpace + 1);
				if (spaceIndex < 0)
					spaceIndex = text.length();
				String subString = text.substring(0, spaceIndex);
				float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
				if (size > width) {
					if (lastSpace < 0)
						lastSpace = spaceIndex;
					subString = text.substring(0, lastSpace);
					lines.add(subString);
					text = text.substring(lastSpace).trim();
					lastSpace = -1;
				} else if (spaceIndex == text.length()) {
					lines.add(text);
					text = "";
				} else {
					lastSpace = spaceIndex;
				}
			}
			// Text is written betweeen beginText() methods.
			contentStream.beginText();
			// We set the font and text leading.
			contentStream.setFont(pdfFont, fontSize);
			// We start a new line of text with newLineAtOffset() method. The origin of a
			// page is at the bottom-left corner
			contentStream.newLineAtOffset(startX, startY);
			for (String line : lines) {
				contentStream.showText(line);
				// With the newLine() method, we move to the start of the next line of text
				contentStream.newLineAtOffset(0, -leading);
			}
			// Text is written betweeen endText() methods.
			contentStream.endText();
			// Stream must be closed before saving document.
			contentStream.close();
			System.out.println("PDF is successfully created");

			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");
			LocalDateTime now = LocalDateTime.now();
			String pdf = null, csv = null, html = null;
			if (format.equals(pdf)) {
				document.save(
						new File("C:\\Users\\HP\\Desktop\\ScrapingData\\PdfFile" + dtf.format(now) + ".pdf"));
			} else if (format.equals(csv)) {
				document.save(
						new File("C:\\Users\\HP\\Desktop\\ScrapingData\\CsvFile" + dtf.format(now) + ".csv"));
			} else if (format.equals(html)) {
				document.save(
						new File("C:\\Users\\HP\\Desktop\\ScrapingData\\HtmlFile" + dtf.format(now) + ".html"));
			}
		} finally {
			if (doc != null) {
				document.close();
			}
		}
		return new Response(200, "PDF is successfully created", true);
	}

	/**
	 * Getting all Web Scraping data details
	 */
	@Override
	public Response getWebScrapingData(String token, String filePath) throws Exception {
		String email = jwtToken.getToken(token);
		User user = userRepository.findByEmail(email);
		// Check if user is present or not
		if (user == null) {
			LOGGER.warning("Invalid user");
			throw new InvalidUser(messageData.Invalid_User);
		}
		// We load a PDF document from the src/main/resources directory
		File myFile = new File(filePath);
		String text;
		try (PDDocument doc = PDDocument.load(myFile)) {
			// PDFTextStripper is used to extract text from the PDF file.
			PDFTextStripper stripper = new PDFTextStripper();
			text = stripper.getText(doc);
			System.out.println(text);
		}
		return new Response(200, "Successfully showing the scrapped data", text);
	}
}