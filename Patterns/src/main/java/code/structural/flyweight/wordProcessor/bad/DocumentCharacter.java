package code.structural.flyweight.wordProcessor.bad;

public class DocumentCharacter {
    private final char character;
    private final String fontType;
    private final int size;
    private final int row;
    private final int col;


    public DocumentCharacter(char character, String fontType, int size, int row, int col) {
        this.character = character;
        this.fontType = fontType;
        this.size = size;
        this.row = row;
        this.col = col;
    }

    public void display(int x, int y){
        System.out.println("displaying " + character + " ,fontType: " + fontType + " with size: "
                + size + " at -(" + x+ " " +y + ")");
    }
}
