package peppo;

import java.util.Iterator;

class Node<T> implements Iterable<Node<T>> {
	private final T element;
	private Node<T> previous;
	private Node<T> next;
	
	
	
	/* BUILDERS */
	
	
	Node(T element, Node<T> previous, Node<T> next) {
		super();
		this.element = element;
		this.previous = previous;
		this.next = next;
	}
	
	
	/**
	 * Copies this node, and the next ones recursively. The element is not copied.
	 * @return a copy of this node, with "previous" set to null, pointing to copied "next" nodes.
	 */
	Node<T> copy(){
		Node<T> copyThis = new Node<T>(this.getElement(), null, null);
		Node<T> copyNext;
		
		if(this.getNext() == null) {
			return copyThis;
		}
		
		copyNext = this.getNext().copy();
		copyNext.setPrevious(copyThis);
		copyThis.setNext(copyNext);
		
		return copyThis;
	}
	
	
	/* MOVERS */
	
	
	/**
	 * Unhooks this node from the list it's in: lets its previous and its next node "bypass" it.
	 */
	void unhook() {
		if(this.previous != null) {
			this.previous.setNext(this.next);
		}
		if(this.previous != null) {
			this.next.setPrevious(this.previous);
		}
		
		this.setNext(null);
		this.setPrevious(null);
		
		return;
	}
	
	
	/**
	 * Inserts this node before the specified node.
	 * If node is null, this becomes the head of a 1-node list.
	 */
	void insertBefore(Node<T> node) {
		// If node is null, this becomes head of a 1-node list
		if(node == null) {
			this.setNext(null);
			this.setPrevious(null);
			return;
		}
		
		// Meet the neighbours
		this.setNext(node);
		this.setPrevious(node.previous);
		// NOW LOVE ME
		this.next.setPrevious(this);
		this.previous.setNext(this);
		
		return;
	}
	
	
	/**
	 * Inserts this node after the specified node.
	 * If node is null, this becomes the head of a 1-node list.
	 */
	void insertAfter(Node<T> node) {
		// If node is null, this becomes head of a 1-node list
		if(node == null) {
			this.setNext(null);
			this.setPrevious(null);
			return;
		}
		
		// Meet the neighbours
		this.setPrevious(node);
		this.setNext(node.next);
		// NOW LOVE ME
		this.next.setPrevious(this);
		this.previous.setNext(this);
		
		return;
	}
	
	
	/**
	 * Swaps this node with the next one.
	 * @return the (ex) next node
	 */
	Node<T> pushAhead(){
		Node<T> exNext = this.next;
		
		// Do nothing if this node is the last
		if(exNext == null) {
			return null;
		}
		
		// Swap with exNext
		this.unhook();
		this.insertAfter(exNext);
		
		return exNext;
	}
	
	
	/* ITERATOR */
	
	
	/**
	 * Inner iterator class
	 */
	class Traverser implements Iterator<Node<T>>{
		private Node<T> currentNode;
		
		Traverser(Node<T> startNode){
			this.currentNode = startNode;
		}

		@Override
		public boolean hasNext() {
			return currentNode != null;
		}

		@Override
		public Node<T> next() {
			Node<T> toReturn;
			
			if(!this.hasNext()) {
				return null;
			}
			
			toReturn = currentNode;
			currentNode = currentNode.next;
			
			return toReturn;
		}
		
	}
	
	
	@Override
	public Iterator<Node<T>> iterator() {
		return new Traverser(this);
	}
	
	
	/* GETTERS AND SETTERS */


	Node<T> getPrevious() {
		return previous;
	}


	void setPrevious(Node<T> previous) {
		this.previous = previous;
	}


	Node<T> getNext() {
		return next;
	}


	void setNext(Node<T> next) {
		this.next = next;
	}


	T getElement() {
		return element;
	}	

}
