/*
 * Copyright 2010 Facebook, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abewy.android.facebook.frienddialog;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.facebook.Session;

public class FbFriendDialog extends Dialog
{

	public static final String				REDIRECT_URI				= "fbconnect://success";
	public static final String				CANCEL_URI					= "fbconnect://cancel";
	public static String					DIALOG_BASE_URL				= "https://facebook.com/dialog/";
	public static final String				TOKEN						= "access_token";
	static final String						ENDPOINT					= "friends/";
	
	static final FrameLayout.LayoutParams	FILL						= new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
																				ViewGroup.LayoutParams.MATCH_PARENT);
	
	 //Hacking part: popup instead of "touch"
	static final String						DISPLAY_STRING				= "iframe";

	private String							mUrl;
	private DialogListener					mListener;
	private ProgressDialog					mSpinner;
	private ImageView						mCrossImage;
	private WebView							mWebView;
	private FrameLayout						mContent;
	
	

	/**
	 * Callback interface for dialog requests.
	 * 
	 */
	public static interface DialogListener
	{

		/**
		 * Called when a dialog completes.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 * @param values
		 *            Key-value string pairs extracted from the response.
		 */
		public void onComplete(Bundle values);

		/**
		 * Called when a Facebook responds to a dialog with an error.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onFacebookError(FacebookError e);

		/**
		 * Called when a dialog has an error.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onError(DialogError e);

		/**
		 * Called when a dialog is canceled by the user.
		 * 
		 * Executed by the thread that initiated the dialog.
		 * 
		 */
		public void onCancel();

	}

	public FbFriendDialog(Context context, String userId, DialogListener listener)
	{
		super(context, android.R.style.Theme_Translucent_NoTitleBar);
		mUrl = formatURL(userId);
		mListener = listener;
	}
	
	private String formatURL(String userId)
	{
		String url = DIALOG_BASE_URL + ENDPOINT;
		
		Bundle parameters = new Bundle();
		
		// Dialog parameters
		
		// Pass the userId
		parameters.putString("id", userId);
		
		// redirect_uri : to return the result (confirming/canceling/error)
		parameters.putString("redirect_uri", REDIRECT_URI);
		
		// Pass your app app_id (required)
		parameters.putString("app_id", Session.getActiveSession().getApplicationId());
		
		// Set the display type of the dialog : iframe (instead of "touch")
		parameters.putString("display", DISPLAY_STRING);
		
		//You can also use "popup" for the "display" parameter. In this case, you can comment 
		// this line as it is not a required parameter.
		parameters.putString("access_token", Session.getActiveSession().getAccessToken());
		
		return url + "?" + Util.encodeUrl(parameters);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mSpinner = new ProgressDialog(getContext());
		mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
		mSpinner.setMessage("Loading...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		mContent = new FrameLayout(getContext());

		/*
		 * Create the 'x' image, but don't add to the mContent layout yet
		 * at this point, we only need to know its drawable width and height
		 * to place the webview
		 */
		createCrossImage();

		/*
		 * Now we know 'x' drawable width and height,
		 * layout the webivew and add it the mContent layout
		 */
		int crossWidth = mCrossImage.getDrawable().getIntrinsicWidth();
		setUpWebView(crossWidth / 2);

		/*
		 * Finally add the 'x' image to the mContent layout and
		 * add mContent to the Dialog view
		 */
		mContent.addView(mCrossImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		addContentView(mContent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
	}

	private void createCrossImage()
	{
		mCrossImage = new ImageView(getContext());
		// Dismiss the dialog when user click on the 'x'
		mCrossImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				mListener.onCancel();
				FbFriendDialog.this.dismiss();
			}
		});
		Drawable crossDrawable = getContext().getResources().getDrawable(R.drawable.close);
		mCrossImage.setImageDrawable(crossDrawable);
		/*
		 * 'x' should not be visible while webview is loading
		 * make it visible only after webview has fully loaded
		 */
		mCrossImage.setVisibility(View.INVISIBLE);
	}

	@SuppressWarnings("deprecation")
	private void setUpWebView(int margin)
	{
		LinearLayout webViewContainer = new LinearLayout(getContext());
		mWebView = new WebView(getContext());
		mWebView.setVerticalScrollBarEnabled(false);
		mWebView.setHorizontalScrollBarEnabled(false);
		mWebView.setWebViewClient(new FbFriendDialog.FbWebViewClient());
		mWebView.getSettings().setJavaScriptEnabled(true);
		
		//Hacking part: Pretend that we are using a desktop version of the browser, not a mobile or tablet 
		mWebView.getSettings().setUserAgentString("Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_5_7; en-us) AppleWebKit/530.17 (KHTML, like Gecko) Version/4.0 Safari/530.17");
		
		mWebView.loadUrl(mUrl);
		mWebView.setLayoutParams(FILL);
		mWebView.setVisibility(View.INVISIBLE);
		mWebView.getSettings().setSavePassword(false);

		webViewContainer.setPadding(margin, margin, margin, margin);
		webViewContainer.addView(mWebView);
		mContent.addView(webViewContainer);
	}

	private class FbWebViewClient extends WebViewClient
	{

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			if (url.startsWith(REDIRECT_URI))
			{
				Bundle values = Util.parseUrl(url);

				String error = values.getString("error");
				if (error == null)
				{
					error = values.getString("error_type");
				}

				if (error == null)
				{
					mListener.onComplete(values);
				}
				else if (error.equals("access_denied") || error.equals("OAuthAccessDeniedException"))
				{
					mListener.onCancel();
				}
				else
				{
					mListener.onFacebookError(new FacebookError(error));
				}

				FbFriendDialog.this.dismiss();
				return true;
			}
			else if (url.startsWith(CANCEL_URI))
			{
				mListener.onCancel();
				FbFriendDialog.this.dismiss();
				return true;
			}
			else if (url.contains(DISPLAY_STRING))
			{
				// Hacking part: return true and the dialog disappears. So set to true
				return false;
			}
			
			// Hacking part: same as above.
			return false;
			
			// launch non-dialog URLs in a full browser
			//getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			//return true;
		}

		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			Log.d("FbDialog.FbWebViewClient", "onReceivedError: " + errorCode + failingUrl + description);
			super.onReceivedError(view, errorCode, description, failingUrl);
			mListener.onError(new DialogError(description, errorCode, failingUrl));
			FbFriendDialog.this.dismiss();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			super.onPageStarted(view, url, favicon);
			mSpinner.show();
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{
			super.onPageFinished(view, url);
			mSpinner.dismiss();
			/*
			 * Once webview is fully loaded, set the mContent background to be transparent
			 * and make visible the 'x' image.
			 */
			mContent.setBackgroundColor(Color.TRANSPARENT);
			mWebView.setVisibility(View.VISIBLE);
			mCrossImage.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Encapsulation of Dialog Error.
	 *
	 * @author ssoneff@facebook.com
	 */
	public static class DialogError extends Throwable {

	    private static final long serialVersionUID = 1L;

	    /**
	     * The ErrorCode received by the WebView: see
	     * http://developer.android.com/reference/android/webkit/WebViewClient.html
	     */
	    private int mErrorCode;

	    /** The URL that the dialog was trying to load */
	    private String mFailingUrl;

	    public DialogError(String message, int errorCode, String failingUrl) {
	        super(message);
	        mErrorCode = errorCode;
	        mFailingUrl = failingUrl;
	    }

	    int getErrorCode() {
	        return mErrorCode;
	    }

	    String getFailingUrl() {
	        return mFailingUrl;
	    }

	}
	
	/**
	 * Encapsulation of a Facebook Error: a Facebook request that could not be
	 * fulfilled.
	 *
	 * @author ssoneff@facebook.com
	 */
	public static  class FacebookError extends RuntimeException {

	    private static final long serialVersionUID = 1L;

	    private int mErrorCode = 0;
	    private String mErrorType;

	    public FacebookError(String message) {
	        super(message);
	    }

	    public FacebookError(String message, String type, int code) {
	        super(message);
	        mErrorType = type;
	        mErrorCode = code;
	    }

	    public int getErrorCode() {
	        return mErrorCode;
	    }

	    public String getErrorType() {
	        return mErrorType;
	    }

	}
	
	/**
	 * Utility class supporting the Facebook Object.
	 *
	 * @author ssoneff@facebook.com
	 *
	 */
	@SuppressWarnings("deprecation")
	private static class Util {

		public static String encodeUrl(Bundle parameters) {
	        if (parameters == null) {
	            return "";
	        }

	        StringBuilder sb = new StringBuilder();
	        boolean first = true;
	        for (String key : parameters.keySet()) {
	            Object parameter = parameters.get(key);
	            if (!(parameter instanceof String)) {
	                continue;
	            }

	            if (first) first = false; else sb.append("&");
	            sb.append(URLEncoder.encode(key) + "=" +
	                      URLEncoder.encode(parameters.getString(key)));
	        }
	        return sb.toString();
	    }

	    public static Bundle decodeUrl(String s) {
	        Bundle params = new Bundle();
	        if (s != null) {
	            String array[] = s.split("&");
	            for (String parameter : array) {
	                String v[] = parameter.split("=");
	                if (v.length == 2) {
	                    params.putString(URLDecoder.decode(v[0]),
	                                     URLDecoder.decode(v[1]));
	                }
	            }
	        }
	        return params;
	    }

	    /**
	     * Parse a URL query and fragment parameters into a key-value bundle.
	     *
	     * @param url the URL to parse
	     * @return a dictionary bundle of keys and values
	     */
	    public static Bundle parseUrl(String url) {
	        // hack to prevent MalformedURLException
	        url = url.replace("fbconnect", "http");
	        try {
	            URL u = new URL(url);
	            Bundle b = decodeUrl(u.getQuery());
	            b.putAll(decodeUrl(u.getRef()));
	            return b;
	        } catch (MalformedURLException e) {
	            return new Bundle();
	        }
	    }
	}
}