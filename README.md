## kendo-emailer

A little utility that uses the [Google Apps API][2] to pull Calendar events and notify people about them by sending an email via the Gmail API.
As the repository name implies, I use this to automate sending out an email to the instructors of the [San Francisco Kendo Dojo][3] so they get a weekly friendly reminder when their class is coming up. 
But you can adapt this to pull events and send mail for other reasons of course!

### Setting Up and Customizing

All these files should be placed in `src/main/resources`. Things will work best when all these files are present!
* **client_secret.json** - Grab this file when setting up using the [Google API setup instructions][1].
* **email_footer.txt** - For text shown before the schedule print-out.
* **email_from.txt** - For the email address that will be shown as the sender of the email.
* **email_header.txt** - For text shown after the schedule print-out.
* **instructors.csv** - Contacts that will be emailed. This file should look like:

```
# Email, Full Name, Debug (if true, email is sent for Preview mode)
mark@mailservice.com, Mark, true
john@mailservice.com, John, false
```

### Running kendo-emailer

* **Debug** mode - Sends no emails to anyone - **gradle -q run**
* **Preview** mode - Sends email to debug contacts - **gradle -q run -Dexec.args="preview"**
* **Production** mode - Sends email to all contacts - **gradle -q run -Dexec.args="production"**

### Sample Run and Output

````
~/Desktop/src/kendo-emailer [master] gradle -q run

Credentials saved to /Users/mcerqueira/.credentials/kendo-emailer-google.json

GoogleHelper/getUpcomingCalendarEvents size = 9
KendoEmailer/main - upcomingEvents.size() = 20
refreshRecipientContactList - sInstructorList.size() = 1

Subject: [SF Kendo] 03/24 - Teaching this week: Teacher, Taacher, Toocher
From : email@mailservice.com
To : allinstructors@mailservice.com

Greetings! Here's who's teaching this week and next!<br>
<b>Thursday - 03-24-2016</b><br>
&nbsp;&nbsp;&nbsp;&nbsp;A - Teacher<br>
&nbsp;&nbsp;&nbsp;&nbsp;C - Taacher<br>
<br>
<b>Monday - 03-28-2016</b><br>
&nbsp;&nbsp;&nbsp;&nbsp;A - Toocher<br>
&nbsp;&nbsp;&nbsp;&nbsp;B - Teacher<br>
<br>
<b>Thursday - 03-31-2016</b><br>
&nbsp;&nbsp;&nbsp;&nbsp;A - Toocher<br>
&nbsp;&nbsp;&nbsp;&nbsp;B - Teacher<br>
<br>
<b>Monday - 04-04-2016</b><br>
&nbsp;&nbsp;&nbsp;&nbsp;A - Taacher<br>
&nbsp;&nbsp;&nbsp;&nbsp;B - Teecher<br>
<br>
<br>
Please contact the administrator if you cannot make a class you are scheduled to teach.<br>
````

[1]: https://developers.google.com/gmail/api/quickstart/java#prerequisites
[2]: https://developers.google.com/google-apps/
[3]: http://www.sanfranciscokendo.org
