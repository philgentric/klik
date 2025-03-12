package klik.audio;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import klik.actor.Aborter;
import klik.look.Look_and_feel_manager;
import klik.util.log.Logger;
import klik.util.log.Stack_trace_getter;
import klik.util.log.System_out_logger;
import klik.util.tcp.Session;
import klik.util.tcp.Session_factory;
import klik.util.tcp.TCP_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

//**********************************************************
public class Audio_player_application extends Application
//**********************************************************
{
    private final static boolean dbg = true;
    //**********************************************************
    public static void main(String[] args) {launch(args);}
    //**********************************************************

    //**********************************************************
    @Override
    public void start(Stage stage) throws Exception
    //**********************************************************
    {
        Logger logger =  new System_out_logger();

        logger.log("Audio_player_application start");

        Parameters params = getParameters();
        List<String> list = params.getRaw();

        if ( dbg)
        {
            logger.log("parameters:"+list.size());
            for(String each : list){
                logger.log(each);
            }

        }

        Look_and_feel_manager.init_Look_and_feel(logger);
        File f = null;
        if ( list.size() == 0)
        {
            //FileChooser fileChooser = new FileChooser();
            //fileChooser.setTitle("Open Audio File");
            //f = fileChooser.showOpenDialog(stage);
        }
        else
        {
            f = new File (list.get(0));
            logger.log("Audio_player_application, opening audio file: "+f.getAbsolutePath());
        }

        Audio_player.play_song(f, logger);

        Session_factory session_factory = new Session_factory() {
            @Override
            public Session make_session() {
                return new Session() {
                    @Override
                    public void on_client_connection(DataInputStream dis, DataOutputStream dos)
                    {
                        try {
                            int size = dis.readInt();
                            byte buffer[] = new byte[size];
                            dis.read(buffer);
                            String file_path = new String(buffer, StandardCharsets.UTF_8);
                            File f = new File(file_path);
                            Audio_player.play_song(f,logger);
                            String reply = Audio_player.PLAY_REQUEST_ACCEPTED;
                            buffer = reply.getBytes(StandardCharsets.UTF_8);
                            dos.writeInt(buffer.length);
                            dos.write(buffer);
                            logger.log("accepted file for playing");
                        }
                        catch (IOException e)
                        {
                            logger.log(Stack_trace_getter.get_stack_trace(""+e));
                        }

                    }

                    @Override
                    public String name() {
                        return "";
                    }
                };
            }
        };
        TCP_server tcp_server = new TCP_server(session_factory,new Aborter("audio",logger),logger);
        tcp_server.start(Audio_player.AUDIO_PLAYER_PORT);

    }
}
