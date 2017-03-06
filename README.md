# Introduction

This application should give you a ready-made starting point for generating token that can be used by clients to make use of Twilio video calls. Please visit your twilio account and gather the following information:

Credential | Description
---------- | -----------
Twilio Account SID | Your main Twilio account identifier - [find it on your dashboard](https://www.twilio.com/user/account/video).
Twilio Video Configuration SID | Adds video capability to the access token - [generate one here](https://www.twilio.com/user/account/video/profiles)
API Key | Used to authenticate - [generate one here](https://www.twilio.com/user/account/messaging/dev-tools/api-keys).
API Secret | Used to authenticate - [just like the above, you'll get one here](https://www.twilio.com/user/account/messaging/dev-tools/api-keys).

## A Note on API Keys

When you generate an API key pair at the URLs above, your API Secret will only
be shown once - make sure to save this in a secure location, 
or possibly your `~/.bash_profile`.

## Setting Up The Java Application

This application uses the lightweight [Spark Framework](www.sparkjava.com), and
requires Java 8 and [Maven](https://maven.apache.org/install.html). 

The server parses your twilio credentials at runtime from the following environmen variables:
```
ACCOUNT_SID="Your Twilio Account SID"
VIDEO_CONFIGURATION_SID="Your Twilio Video Configuration SID"
API_KEY="Your API Key"
API_SECRET="Your API Secret"
```
To facilitate the setup, we have provided the file `.env_config.example`

Please fill in the afore mentioned values (you get them from your Twilio account as outlined above) and rename the file to `.env_config` (that file has already been added to the .gitignore). 

To load the environment variables defined in that file we'll source the file:

```bash
source .env_config
```

Next, we need to install our depenedencies from Maven (you only need to do this once):

```bash
mvn install
```

And compile our application code (this needs to be done each time you change the server code):

```bash
mvn package
```

Now we should be all set! Run the application using the `java -jar` command.

```bash
java -jar target/video-quickstart-1.0-SNAPSHOT.jar
```
Or simply use the script we provided and call `./start-sparky.sh`

## Changing Twilio credentials

You should be able to change the twilio credentials while the server is running, by simply updating the `.env_config` and sourcing it again, since we're not caching the envirnment variables but instead read them again each time.

Your application should now be running at [http://localhost:4567](http://localhost:4567). 
If you open the URL in a browser you should see the video calling interface, however at the moment authenticating via the webbrowser is not supported within this implementation. You can however easily add it, if you provide the required credentials (username, account etc).

## License

MIT
