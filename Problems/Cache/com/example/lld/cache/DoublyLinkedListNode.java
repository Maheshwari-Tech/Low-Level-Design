public class DoublyLinkedListNode<Key> {
    private Key key;
    DoublyLinkedListNode<Key> next;
    DoublyLinkedListNode<Key> prev;

    DoublyLinkedListNode(Key key) {
        this.key = key;
        this.next = null;
        this.prev = null;
    }

    Key getValue() {
        return key;
    }

    void updateNext(DoublyLinkedListNode<Key> node) {
        this.next = node;
    }

    void updatePrev(DoublyLinkedListNode<Key> node) {
        this.prev = node;
    }
}
