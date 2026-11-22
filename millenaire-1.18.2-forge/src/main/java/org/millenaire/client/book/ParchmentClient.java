package org.millenaire.client.book;
import java.util.*;
import net.minecraft.world.entity.player.Player;
import org.millenaire.client.gui.DisplayActions;
import org.millenaire.common.forge.Mill;
import org.millenaire.common.utilities.LanguageUtilities;
public final class ParchmentClient {
    public static void open(Player player,int[] ids) {
        List<List<String>> pages=new ArrayList<>();
        for(int id:ids) {
            List<List<String>> text=LanguageUtilities.getParchment(id);
            if(text!=null)pages.addAll(text);
            else if(ids.length==1) { Mill.proxy.localTranslatedSentence(player,'6',"panels.notextfound",Integer.toString(id));return; }
        }
        DisplayActions.displayParchmentPanelGUI(player,TextBook.convertStringsToBook(pages),null,0,true);
    }
}
