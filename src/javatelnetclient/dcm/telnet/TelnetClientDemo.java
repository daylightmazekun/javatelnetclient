/**
 *
 */
package javatelnetclient.dcm.telnet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.telnet.TelnetClient;

/**
 * TODO This is default class comment.<br>
 *
 *
 * @version $Revision: 227 $, $Date: 2015-01-15 16:51:17 +0900 (木, 15 1 2015) $
 * @author  G44944
 * @author  Last commit: $Author: okamura $
 */

public class TelnetClientDemo
{
    private TelnetClient telnet;

    private Socket socket;

    private final InputStream in;

    private final PrintStream out;

    private final String login;

    private final String password;

    private final String prompt;

    private long timeout = 1000l;

    public static final String READ_FILE = "C:\\daifuku\\readfile\\respi.config";

    public static final String WRITE_FILE = "C:\\daifuku\\readfile\\respi_run.log";

    public static final String CHART_SET = "UTF-8";

    public static final String PROMPT = ">";

    public TelnetClientDemo(String server, String user, String password, String prompt, boolean mode) throws IOException
    {
        telnet = new TelnetClient();
        telnet.connect(server, 23);
        in = telnet.getInputStream();
        out = new PrintStream(telnet.getOutputStream());
        this.login = user;
        this.password = password;
        this.prompt = prompt;
        try
        {
            Thread.sleep(200L);
        }
        catch (Exception e)
        {
        }
        write(user);
        try
        {
            Thread.sleep(200L);
        }
        catch (Exception e)
        {
        }
        write(password);
        readUntil(prompt);
    }


    public String readUntil(String pattern)
            throws IOException
    {
        long lastTime = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        while (true)
        {
            int c = -1;
            byte[] text;
            if (in.available() > 0)
            {
                c = in.read(text = new byte[in.available()]);
                sb.append(new String(text));
            }
            long now = System.currentTimeMillis();
            if (c != -1)
            {
                lastTime = now;
            }
            if (now - lastTime > timeout)
            {
                break;
            }
            if (sb.toString().contains(pattern))
            {
                return sb.toString();
            }
            try
            {
                Thread.sleep(50);
            }
            catch (Exception e)
            {
            }
        }
        return sb.toString();
    }

    public void print(String value)
    {
        try
        {
            System.out.println(value + ";");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void write(String value)
    {
        try
        {
            out.println(value);
            out.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String sendCommand(String command)
    {
        BufferedWriter outlog = null;
       
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        try
        {
            write(command);
            String until = readUntil(prompt);
            FileOutputStream fileOutputStream;
            fileOutputStream = new FileOutputStream(WRITE_FILE, true);
            outlog = new BufferedWriter(new OutputStreamWriter(fileOutputStream, CHART_SET));
            Date date = new Date();
            outlog.write("start time:" + dateFormat.format(date) + "\n");
            outlog.write("command:" + command + "\n");
            outlog.write("ouptut start \n======================================= \n" + until + "\n");
//            System.out.println("\n command ->" + command + " \n ouptut->" + until);
            outlog.write("ouptut end \n======================================= " + "\n");
            Date dateEnd = new Date();
            outlog.write("end time:" + dateFormat.format(dateEnd) + "\n");
            outlog.close();
            return until;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void disconnect()
    {
        try
        {
            if (socket != null) socket.close();
            if (telnet != null) telnet.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Map<String, String> file2Map(String path, String encoder)
    {
        File file = new File(READ_FILE);
        if (!file.exists())
        {
            return null;
        }
        Map<String, String> alline = new HashMap<String, String>();
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), encoder));
            String str = new String();
            while ((str = in.readLine()) != null)
            {
                String[] strs = str.split(":");
                try
                {
                    alline.put(strs[0].trim(), strs[1].trim());
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    alline.put(strs[0].trim(), "");
                    continue;
                }
            }
            in.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return alline;
    }

    public static void main(String args[])
            throws IOException,
                InterruptedException
    {
        Map<String, String> allLine = file2Map(READ_FILE, CHART_SET);
        String server = allLine.get("HOST");
        String user = allLine.get("USER");
        String password = "";
        if (allLine.get("PASSWORD") == null || allLine.get("PASSWORD").isEmpty())
        {
            password = "";
        }
        else
        {
            password = allLine.get("PASSWORD");
        }

        String prompt = PROMPT;
        String[] commands = allLine.get("COMMANDS").split(",");
        TelnetClientDemo telnetClient = new TelnetClientDemo(server, user, password, prompt, true);
        for (String command : commands)
        {
            telnetClient.sendCommand(command);
        }
        telnetClient.disconnect();
    }
}
