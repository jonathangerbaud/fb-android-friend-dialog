fb-android-friend-dialog
========================

I made this dialog since the "normal" way of invoking the friend dialog end with an "Sorry, something went wrong" error. Recently, Facebook answered in a bug report that they were not going to fix this in a short or medium term (see this bug report https://developers.facebook.com/x/bugs/506955376009451/)

More info here : http://www.abewy.com/site/blog/2013/12/18/facebook-friend-dialog/

Usage
=====

Assuming that you are logged in Facebook using the Facebook Android SDK

	private void handleAddFriend()
	{
	  new FbFriendDialog(this, user.getUsername(), new FbFriendDialog.DialogListener() {

		  @Override
		  public void onFacebookError(FacebookError e)
		  {
			  Log.d("UserTimeline", "FbFriendDialog onFacebookError: " + e);
		  }

		  @Override
		  public void onError(DialogError e)
		  {
			  Log.d("UserTimeline", "FbFriendDialog onError: " + e);
		  }

		  @Override
		  public void onComplete(Bundle values)
		  {
			  if (values != null)
			  {
				  String result = values.getString("action");

				  if (result != null && result.equals("1"))
				  {
					  // friend request sent or confirmed
				  }
				  else if (result != null && result.equals("0"))
				  {
					  // friend request declined or canceled
				  }
			  }
		  }

		  @Override
		  public void onCancel()
		  {
			  Log.d("UserTimeline", "FbFriendDialog onCancel: ");
		  }
	  }).show();
	}


Required dependencies
=====================

Facebook Android SDK : https://github.com/facebook/facebook-android-sdk


License
-------
`fb-android-friend-dialog` is available under the [Beerware](http://en.wikipedia.org/wiki/Beerware) license.

If we meet some day, and you think this stuff is worth it, you can buy me a beer in return.
