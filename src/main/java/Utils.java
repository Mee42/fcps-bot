
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.RequestBuffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();






    public static IDiscordClient buildClient(File f){
        return new ClientBuilder()
                .withToken(readToken(f))
                .build();
    }
    public static IDiscordClient buildClient(String fileName){
        return buildClient(new File(fileName));
    }
    public static IDiscordClient buildClient(){
        return buildClient("key.txt");
    }
    public static String readToken(File file){
        String token = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while(line != null) {
                token+=line;
                line = br.readLine();
            }
        }catch(Exception e) {
            System.err.println("threw a " + e.getClass().getName() + " when trying to read from key");
            System.err.println("remember - the key needs to be in a key.txt file right next to the jar");
            e.printStackTrace();
            System.exit(-1);
        }
        token = token.replace("\n","");//tokens don't have newlines, but some text editors leave on at the end
        return token;
    }


}
