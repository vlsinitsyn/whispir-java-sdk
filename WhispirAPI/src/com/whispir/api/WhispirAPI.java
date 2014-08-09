package com.whispir.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.OptionsMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.whispir.api.exceptions.WhispirAPIException;

/**
 * WhispirAPI 
 * 
 * Wrapper class to simplify the usage of the Whispir API.  
 * 
 * Utilises Apache HTTPClient to post simple messages via JSON.
 * 
 * @author Jordan Walsh
 * @version 1.0
 * 
 */

public class WhispirAPI {

	private static final String WHISPIR_MESSAGE_HEADER_V1 = "application/vnd.whispir.message-v1+json";
	private static final String WHISPIR_MESSAGE_HEADER_V2 = "application/vnd.whispir.message-v2+json";
	private static final String API_HOST = "api.whispir.com";
	private static final String API_URL = "https://api.whispir.com/";
	private static final String API_EXT = "?apikey=";
	private static final String NO_AUTH_ERROR = "Whispir API Authentication failed. API Key, Username or Password was not provided.";
	private static final String AUTH_FAILED_ERROR = "Whispir API Authentication failed. API Key, Username or Password were provided but were not correct.";
	
	private String apikey;
	private String username;
	private String password;
	private String version;

	@SuppressWarnings("unused")
	private WhispirAPI() {}
	
	/**
	 * Instantiates the WhispirAPI object.
	 * 
	 * Requires the three parameters to be provided.
	 *  
	 * @param apikey
	 * @param username
	 * @param password
	 */
	
	public WhispirAPI(String apikey, String username, String password) throws WhispirAPIException{	
		this(apikey, username, password, "v1");
	}
	
	public WhispirAPI(String apikey, String username, String password, String version) throws WhispirAPIException{
		
		if(apikey == null || username == null || password == null || version == null ) {
			throw new WhispirAPIException(NO_AUTH_ERROR);
		}
		
		if("".equals(apikey) || "".equals(username) || "".equals(password) || "".equals(version)) {
			throw new WhispirAPIException(NO_AUTH_ERROR);
		}
		
		try {
			this.apikey = apikey;
			this.username = username;
			this.password = password;
			
			//If the GET request fails, then throw an error as the API won't work.
			int response = this.testHttpCall();
			
			if (response != 200) {
				throw new WhispirAPIException(AUTH_FAILED_ERROR);
			} 
			
			if("v2".equals(version)) {
				this.version = WHISPIR_MESSAGE_HEADER_V2;
			} else {
				this.version = WHISPIR_MESSAGE_HEADER_V1;
			}
			
		} catch (WhispirAPIException e) {
			throw e;
		}
	}
	
	public void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	/**
	 * Allows a user to send a message in the default My Company workspace
	 * @param recipient - the mobile number or email address of the recipient of the message
	 * @param subject - the textual subject of the message
	 * @param content - the textual content of the Push/SMS message.
	 * 
	 * For more complex content, the user should use the Map content overloaded function
	 * 
	 * @return response - the HTTP response code of the performed action.
	 */
	public int sendMessage(String recipient, String subject, String content) throws WhispirAPIException{		
		return sendMessage("", recipient, subject, content);
	}
	
	/**
	 * Allows a user to send a message in the specified Workspace ID
	 * @param recipient - the mobile number or email address of the recipient of the message
	 * @param subject - the textual subject of the message
	 * @param content - the textual content of the Push/SMS message.
	 * 
	 * For more complex content, the user should use the Map content overloaded function
	 * 
	 * @return response - the HTTP response code of the performed action.
	 */
	public int sendMessage(String workspaceId, String recipient, String subject, String content) throws WhispirAPIException{
		Map<String,String> smsContent = new HashMap<String,String>();
		smsContent.put("body", content);
		return sendMessage(workspaceId, recipient, subject, smsContent);
	}
	
	/**
	 * Allows a user to send a message in any workspace, with any combination of content within the content map
	 * @param recipient - the mobile number or email address of the recipient of the message
	 * @param subject - the textual subject of the message
	 * @param content - the Map of content for the Whispir Message
	 * 
	 * The content Map is expected to provide the following information
	 * 
	 * For SMS/Push
	 * body - The content for the Push/SMS message
	 * 
	 * For Email
	 * emailType - The required mime type for the email (text/plain, text/html)
	 * emailBody - The content for the Email
	 * 
	 * 
	 * @return response - the HTTP response code of the performed action.
	 */
	public int sendMessage(String workspaceId, String recipient, String subject, Map<String,String> content) throws WhispirAPIException{
		int response = 0;
		
		if(recipient == null || recipient.length() < 8) {
			//error with the recipient information, returning HTTP 422.
			return 422;
		}
		
		try {
			//Check for SMS/Push Content
			String sms = content.get("body");
			
			String myString = new JSONObject().put("to", recipient)
					.put("subject", subject).put("body", sms)
					.toString();

			response = httpPost(workspaceId, myString);

		} catch (JSONException e) {
			throw new WhispirAPIException("Error occurred parsing the object with the content provided." + e.getMessage());
		} catch (Exception e) {
			throw new WhispirAPIException("Error occurred." + e.getMessage());
		}
		
		return response;
	}
	
	private int testHttpCall() throws WhispirAPIException {	
		OptionsMethod method = (OptionsMethod)createOptionsMethod();
		
		return executeHttpMethod(method);
	}
	
	private int httpPost(String workspace, String jsonContent) throws WhispirAPIException {	
		PostMethod method = (PostMethod)createPostMethod(workspace, jsonContent);
		
		return executeHttpMethod(method);
	}

	private int executeHttpMethod(HttpMethod method) throws WhispirAPIException {
		int statusCode = 0;

		// Create an instance of HttpClient.
		HttpClient client = new HttpClient();

		client.getState().setCredentials(
				new AuthScope(API_HOST, 443, null),
				new UsernamePasswordCredentials(this.username, this.password));

		try {
			statusCode = client.executeMethod(method);
		} catch (HttpException e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Release the connection.
			method.releaseConnection();
		}

		return statusCode;
	}
	
	private HttpMethod createPostMethod(String workspaceId, String content) throws WhispirAPIException {
		// Create a method instance.
		String url = "";
		RequestEntity request;
		
		if(workspaceId != null && !"".equals(workspaceId)) {
			url = API_URL + "workspaces/" + workspaceId + "/messages" + API_EXT + this.apikey;
		} else {
			url = API_URL + "messages" + API_EXT + this.apikey;
		}
		PostMethod method = new PostMethod(url);

		method.setDoAuthentication(true);
		
		method.setRequestHeader("Content-Type", this.version);
		method.setRequestHeader("Accept", this.version);
		
		try {
			request = new StringRequestEntity(content, WHISPIR_MESSAGE_HEADER_V1, null);
			method.setRequestEntity(request);
		} catch (UnsupportedEncodingException e) {
			throw new WhispirAPIException(e.getMessage());
		}
		
		return method;
	}
	
	
	/**
	 * Constructs an OPTIONS call to execute with the supplied credentials.
	 * This is used as a quick test call to determine whether the credentials are correct.
	 * 
	 * @return OptionsMethod to be executed by HTTPClient
	 */
	private HttpMethod createOptionsMethod() {
		// Create a method instance.
		final String url = API_URL + "messages" + API_EXT + this.apikey;
		
		OptionsMethod method = new OptionsMethod(url);

		method.setDoAuthentication(true);
		
		method.setRequestHeader("Content-Type", WHISPIR_MESSAGE_HEADER_V1);
		method.setRequestHeader("Accept", WHISPIR_MESSAGE_HEADER_V1);
		
		return method;
	}
}
