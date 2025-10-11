package klik.util.files_and_paths;

import klik.Shared_services;
import klik.util.Sys_init;
import klik.util.log.Logger;

import java.util.UUID;

//**********************************************************
public class Name_cleaner
//**********************************************************
{
    public static final String SP_EZ_IA_L = "_Copy_Made_By_Klik_";
    private static final boolean dbg = false;
    //**********************************************************
    public static String clean(String in, boolean check_extension, Logger logger)
    //**********************************************************
    {
        if ( in.endsWith(".JPG_original") )
        {
            logger.log(" detected "+in);
            String returned = UUID.randomUUID().toString()+".jpg";
            return returned;
        }
        if ( ( in.contains(".jpg.old."))||( in.contains("_Jpeg_Old.")))
        {
            logger.log(" detected "+in);
            String returned = UUID.randomUUID().toString()+".jpg";
            return returned;
        }
        String extension = "";
        String base_name = in;
        if ( check_extension)
        {
            extension = Extensions.get_extension(in);
            base_name = Extensions.get_base_name(in);
        }

        String new_str = base_name.trim();

        new_str = new_str.replaceAll("\\s",    "_");
        if ( dbg) logger.log("1 "+new_str);
        new_str = new_str.replaceAll("-",    "_");
        if ( dbg) logger.log("2 "+new_str);
        new_str = new_str.replaceAll("\\.",    "_");
        new_str = new_str.replaceAll(":",    "_");
        new_str = new_str.replaceAll("\\$",    "_");
        new_str = new_str.replaceAll("\\?",    "_");
        new_str = new_str.replaceAll("\\%",    "_");
        new_str = new_str.replaceAll("\\|",    "_");
        new_str = new_str.replaceAll("<",    "_");
        new_str = new_str.replaceAll(">",    "_");
        new_str = new_str.replaceAll(",",    "_");
        new_str = new_str.replaceAll(";",    "_");
        new_str = new_str.replaceAll("=",    "_");
        new_str = new_str.replaceAll("\\[",    "_");
        new_str = new_str.replaceAll("\\]",    "_");
        if ( dbg) logger.log("3 "+new_str);

        for(;;)
        {
            String tmp = new_str;
            new_str = tmp.replaceAll("__",    "_");
            if ( dbg) logger.log("4 "+new_str);
            if ( new_str.equals(tmp)) break;
        }
        String[] words = new_str.split("_");
        StringBuilder result = new StringBuilder();
        // capitalize
        for(int i=0;i<words.length;i++)
        {
            if ( dbg) logger.log("word->"+words[i]+"<-");
            String word = words[i];
            if ( word.isEmpty()) continue;
            String first = word.substring(0,1);
            String afterfirst = word.substring(1);
            result.append(first.toUpperCase()).append(afterfirst);
            if(i<words.length-1)
            {
                result.append("_");
            }
        }

        // try to remove SP_EZ_IA_L
        {
            int i = result.lastIndexOf(SP_EZ_IA_L);
            if ( i > 0 )
            {
                String pruned_name = base_name.substring(0, i);
                String remainer = base_name.substring(i + SP_EZ_IA_L.length());
                logger.log("remainer->" + remainer + "<-");
                try {
                    int N = Integer.parseInt(remainer);
                    result = new StringBuilder(pruned_name);
                } catch (NumberFormatException e) {
                    logger.log("remainer->" + remainer + "<- not a number, not pruned");
                }
            }
        }



        if ( !extension.isEmpty())
        {
          result.append(".").append(extension);
        }
        if ( dbg) logger.log(in+" ==> "+result);
        return result.toString();
    }

    //**********************************************************
    public static String attempt_to_simplify(String in, Logger logger)
    //**********************************************************
    {
        // trys to remove all SP_EZ_IA_L+N

        String extension = Extensions.get_extension(in);
        String base_name = Extensions.get_base_name(in);


        String result = base_name;
        for(;;)
        {
            int i = result.lastIndexOf(SP_EZ_IA_L);
            if (i < 0) break;
            String pruned_name = result.substring(0, i);
            if ( pruned_name.endsWith("_"))
            {
                pruned_name = pruned_name.substring(0,pruned_name.length()-1);
            }
            String remainer = result.substring(i + SP_EZ_IA_L.length());
            logger.log("remainer->" + remainer + "<-");
            logger.log("pruned_name->" + pruned_name + "<-");
            try {
                int N = Integer.valueOf(remainer);
                result = pruned_name;
                continue;
            }
            catch (NumberFormatException e)
            {
                logger.log("remainer->" + remainer + "<- not a number, not pruned");
            }
            break;

        }
        if ( !extension.isEmpty())
        {
            result = result + "." + extension;
        }
        return result;
    }

    //**********************************************************
    public static void main(String[] args)
    //**********************************************************
    {
        Sys_init.init("Name_cleaner test app",null);
        Logger logger = Shared_services.logger;


        {
            String test =" $fsd,de  :dfd- _hd<gd. ___---84565FTaMere dwedsds .jpg";
            logger.log(test+" => "+clean(test,true,logger));
        }
        {
            String test =" adf>sfsd-r=GDF 9857- fggf..java";
            logger.log(test+" => "+clean(test,true,logger));
        }
        {
            String test =" fd?fe- f;gr%tg d--.verylongextension";
            logger.log(test+" => "+clean(test,true,logger));
        }
        {
            String test =" fg.b--.-dsed1|dndl .Gif";
            logger.log(test+" => "+clean(test,true,logger));
        }
        {
            String test ="qsddsdffdfd.chic";
            logger.log(test+" => "+clean(test,false,logger));
        }

        logger.log("\n\n\n\n\n");

        {
            String test ="xxxxxxx"+ SP_EZ_IA_L+"0_"+ SP_EZ_IA_L+"1";

            logger.log(test+" ==> "+attempt_to_simplify(test,logger));

        }
    }
}
