package code.structural.flyweight.wordProcessor.good;

import java.util.HashMap;
import java.util.Map;

public class LetterFactory {
    private static Map<String, Letter> mp = new HashMap<>();

    public static Letter createLetter(Character ch, String fontType, int size){
        String key = createKey(ch, fontType, size);
        if(!mp.containsKey(key)){
           mp.put(key, new DocumentCharacter(ch, fontType, size));
        }
        return mp.get(key);
    }

   private static String createKey(Character ch, String fontType, int size){
       return ch + ":" + fontType + ":"+ size;
   }
}
