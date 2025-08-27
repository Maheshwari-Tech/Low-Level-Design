package com.mycompany.app.algorithms;

public class DoublyLinkedList<T> {
    DoublyLinkedListNode<T> head;
    DoublyLinkedListNode<T> tail;

    public void addInBegin(T value){
        DoublyLinkedListNode<T> doublyLinkedListNode = new DoublyLinkedListNode<>(value);
        doublyLinkedListNode.next = head;
        head = doublyLinkedListNode;

        // adding first item
        if(tail == null){
            tail = doublyLinkedListNode;
        }
    }

    public void deleteNode(DoublyLinkedListNode<T> node){
        if(node.prev != null){
            node.prev.next = node.next;
        }
        else{
            head = head.next;
        }

        if(node.next != null){
            node.next.prev = node.prev;
        }
        else{
            tail = tail.prev;
        }
    }

    public DoublyLinkedListNode<T> getHead() {
        return head;
    }

    public DoublyLinkedListNode<T> getTail() {
        return tail;
    }

    public boolean isEmpty(){
        return head == null;
    }
}
