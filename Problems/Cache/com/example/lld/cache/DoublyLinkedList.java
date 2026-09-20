public class DoublyLinkedList<Key> {

    DoublyLinkedListNode<Key> head;
    DoublyLinkedListNode<Key> last;

    boolean isEmpty(){
        return head == null;
    }

    DoublyLinkedListNode removeFirstNode(){
        if(head == null){
            return null;
        }
        DoublyLinkedListNode first = head;
        head = head.next;
        return first;
    }

    void insertInEnd(DoublyLinkedListNode node){
        if(head == null){
            head = node;
            last = node;
        }
        else{
            last.next = node;
            node.prev = head;
            last = node;
        }
    }
}
