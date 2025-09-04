package code.structural.flyweight.wordProcessor.good;

public class DocumentCharacter implements Letter {
    private final Character character;
    private final String fontType;
    private final int size;

    public DocumentCharacter(Character character, String fontType, int size) {
        this.character = character;
        this.fontType = fontType;
        this.size = size;
    }

    public void display(int x, int y){
        System.out.println("displaying " + character + " ,fontType: " + fontType + " with size: "
                + size + " at -(" + x+ " " +y + ")");
    }
}
