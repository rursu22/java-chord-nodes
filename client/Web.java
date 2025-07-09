import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;

class HTTPRequest {
	RequestType type;
	String resource;
	HTTPHeader headers[];

	String getHeaderValue(String key) {
		for (int i = 0; i < headers.length; i++) {
			if (headers[i].key.equals(key))
				return headers[i].value;
		}

		return null;
	}
}

public class Web {

	static int RESPONSE_OK = 200;
	static int RESPONSE_NOT_FOUND = 404;
	static int RESPONSE_SERVER_ERROR = 501;

	FormMultipart formParser = new FormMultipart();
	XMLParser XMLParser = new XMLParser();

	private void sendResponse(OutputStream output, int responseCode, String contentType, byte content[], String fileName) {
		try {
			output.write(new String("HTTP/1.1 " + responseCode + "\r\n").getBytes());
			output.write("Server: Kitten Server\r\n".getBytes());
			if (content != null) {
				output.write(new String("Content-length: " + content.length + "\r\n").getBytes());
			}
			if (contentType != null) {
				output.write(new String("Content-type: " + contentType + "\r\n").getBytes());
			}
			if (fileName != null) {
				output.write(("Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n").getBytes());
			}
			output.write(new String("Connection: close\r\n").getBytes());
			output.write(new String("\r\n").getBytes());

			if (content != null)
				output.write(content);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String serveCSS(String response) {
		// Since the CSS is just a file, we can use the download function to download
		// the file and link it as a stylesheet
		response += "<head>\n" +
				"<link rel = 'stylesheet' href = '/download/style.css' />\n" +
				"</head";

		return response;
	}

	String serveNavbar(String response) {
		response += "<div id = 'navbar'>\n" +
				"<ul>\n" +
				"<a href = '/upload'><li> Upload Files </li></a>\n" +
				"<a href = '/files'><li> List Files </li></a>\n" +
				"</ul>\n" +
				"</div>\n";

		return response;

	}

	/**
	 * Lists all of the files that are on the DHT.
	 * @param output
	 */
	public void listFiles(OutputStream output) {
		String html = "";
		try {
			// RMI
			Registry registry = LocateRegistry.getRegistry("localhost", 9002);
			IDistributedHashTable DHT = (IDistributedHashTable) registry.lookup("DHT");
			HashMap<String, Integer> progress = DHT.sendTasksProgress();

			// Set up the HTML
			
			html += "<html>\n";
			html = serveCSS(html);
			html += "<body>\n";
			html = serveNavbar(html);
			html += "<div id = 'filesWrapper'>\n";
			if(progress.size() > 0) {
				for(HashMap.Entry<String, Integer> progressEntry : progress.entrySet()) {
					html += "<div id = 'fileInfo'>\n";
					html += "<h1 id = 'fileName'>" + progressEntry.getKey() + "</h1>\n";
					// This is how I do the progress, initially starts at 0, and when the XML for the file pops up, 
					// the progress becomes 1
					if(progressEntry.getValue() == 0) {
						// When the file is not ready te
						html += "<h1> Not Done Yet! </h1>\n";
					} else {
						// When the file is ready
						String XML = DHT.sendXML(progressEntry.getKey());
						HashMap<String, String> XMLAttributes = XMLParser.getAttributes(XML);
						html += "<div id = 'XMLAttributeCard'>\n";
						for(HashMap.Entry<String,String> XMLEntry : XMLAttributes.entrySet()) {
							html += "<div class = 'XMLPair'>\n";
							html += "<div class = 'XMLAttribute'>" + XMLEntry.getKey() + "</div>\n";
							html += "<div class = 'XMLValue'>" + XMLEntry.getValue() + "</div>\n";
							html += "</div>\n";
						}
						html += "</div>\n";
						html += "<a id = 'downloadButton' href = '/download/" + progressEntry.getKey() + "'>Download XML</a>\n";
					}
					html += "</div>\n";
				}
			} else {
				html += "<h1> No tasks have been added yet! </h1>\n";
			}
			html += "</div>\n";
			html += "</body>\n";
			html += "</html>";
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		sendResponse(output, RESPONSE_OK, "text/html", html.getBytes(), null);
	}

	// example of a form to fill in, which triggers a POST request when the user
	// clicks submit on the form
	void page_upload(OutputStream output) {
		String response = "";
		response += "<html>\n";
		response = serveCSS(response);
		response += "<body>\n";
		response = serveNavbar(response);
		response += "<div id = 'formWrapper'>\n";
		response += "<form action=\"/upload_do\" method=\"POST\" enctype=\"multipart/form-data\">\n";
		// response += "<input type=\"text\" name=\"name\" placeholder=\"File name\" required/>\n";
		response += "<input type=\"file\" name=\"content\" required/>\n";
		response += "<select id = 'taskType' name = 'taskDropdown'>\n";
		response += "<option value = 'text' selected>Text</option>\n";
		response += "<option value = 'csv'>CSV</option>\n";
		response += "<option value = 'image'>Image</option>\n";
		response += "</select>\n";
		response += "<input type=\"submit\" name=\"submit\"/>\n";
		response += "</form>\n";
		response += "</div>\n";
		response += "</body>\n";
		response += "</html>\n";

		sendResponse(output, RESPONSE_OK, "text/html", response.getBytes(), null);
	}

	void downloadFile(HTTPRequest request, OutputStream output) {
		String filename = request.resource.substring("/download/".length());
		// We don't want to download the css file to our disk, we just want the website to be able to retrieve it from disk,
		// so we have to implement it in a different way compared to the XML
		if (filename.equals("style.css")) {
			File downloadedFile = new File("./files/" + filename);
			try (FileInputStream fis = new FileInputStream(downloadedFile)) {
				byte[] fileContent = fis.readAllBytes();
				sendResponse(output, RESPONSE_OK, "text/css", fileContent, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				// We connect to the DHT
				Registry registry = LocateRegistry.getRegistry("localhost", 9002);
				IDistributedHashTable DHT = (IDistributedHashTable) registry.lookup("DHT");
				// We retrieve the XML for the specific filename that we want
				// i.e., if we want to download test.txt.xml, we go to /download/test.txt.xml
				String XML = DHT.sendXML(filename);
				byte[] XMLAsBytes = XML.getBytes();
				sendResponse(output, RESPONSE_OK, "application/xml", XMLAsBytes, filename + ".xml");
			} catch (Exception e) {
				e.printStackTrace();
				sendResponse(output, RESPONSE_NOT_FOUND, null, null, null);
			}
		}
	}

	// this function maps GET requests onto functions / code which return HTML pages
	void get(HTTPRequest request, OutputStream output) {
		if (request.resource.equals("/upload")) {
			page_upload(output);
		} else if (request.resource.startsWith("/download/")) {
			downloadFile(request, output);
		} else if (request.resource.equals("/files")) {
			listFiles(output);
		} else if (request.resource.equals("/")) {
			listFiles(output);
		} else {
			sendResponse(output, RESPONSE_NOT_FOUND, null, null, null);
		}
	}

	// this function maps POST requests onto functions / code which return HTML
	// pages
	void post(HTTPRequest request, byte payload[], OutputStream output) {
		if (request.resource.equals("/upload_do")) {
			// FormMultipart
			if (request.getHeaderValue("content-type") != null
					&& request.getHeaderValue("content-type").startsWith("multipart/form-data")) {
				FormData data = formParser.getFormData(request.getHeaderValue("content-type"), payload);
				try {
					// We connect to the DHT
					Registry registry = LocateRegistry.getRegistry("localhost", 9002);
					IDistributedHashTable DHT = (IDistributedHashTable) registry.lookup("DHT");
					String filename = "";
					byte[] content = null;
					String taskType = "";
					// We go through each of the fields of the form and get the specific data from them
					for (int i = 0; i < data.fields.length; i++) {
						if (data.fields[i].name.equals("content")) {
							FileFormField fileField = (FileFormField) data.fields[i];
							filename = fileField.filename;
							content = fileField.content;
						} else if (data.fields[i].name.equals("taskdropdown")) {
							taskType = new String(data.fields[i].content);
							// System.out.println(taskType);
						}
					}
					// We let the DHT distribute the task to the worker nodes
					DHT.distributeToNodes(filename, content, taskType);
					// Redirect the user to the listFiles page for instant feedback
					listFiles(output);

				} catch (Exception e) {
					e.printStackTrace();
				}

				sendResponse(output, RESPONSE_OK, "text/html", "<html>File sent, thanks!</html>".getBytes(), null);
			} else {
				sendResponse(output, RESPONSE_SERVER_ERROR, null, null, null);
			}
		}
	}

}