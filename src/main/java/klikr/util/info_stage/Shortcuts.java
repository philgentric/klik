package klikr.util.info_stage;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import klikr.util.Kontext;


//**********************************************************
public class Shortcuts
//**********************************************************
{
    //**********************************************************
    public static void debug(Scene scene, Kontext context)
    //**********************************************************
    {
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            context.log("Key pressed: " + event.getCode() + ", " +
                    "isShortcutDown:" + event.isShortcutDown()+
                    ", isMetaDown:" + event.isMetaDown()+
                    ", isAltDown:" + event.isAltDown()+
                    ", isControlDown:" + event.isControlDown());
        });
    }

    //**********************************************************
    public static void smaller(Scene scene, Runnable runnable, Kontext context)
    //**********************************************************
    {
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.META_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.META_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }    
    }
    //**********************************************************
    public static void larger(Scene scene, Runnable runnable, Kontext context)
    //**********************************************************
    {
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.ADD, KeyCombination.SHORTCUT_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
        KeyCombination kc = new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN);
        scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.META_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.META_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
        {
            KeyCombination kc = new KeyCodeCombination(KeyCode.ADD, KeyCombination.META_DOWN);
            scene.getAccelerators().put(kc, runnable);
        }
    }
}
