package com.mycompany.app.algorithms;

public class DoublyLinkedListNode<T> {
    T item;
    DoublyLinkedListNode<T> next;
    DoublyLinkedListNode<T> prev;

    DoublyLinkedListNode(T item){
        this.item = item;
        this.next = null;
        this.prev = null;
    }

    public T getItem(){
        return item;
    }
}
