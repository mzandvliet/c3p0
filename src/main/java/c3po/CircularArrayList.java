package c3po;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.RandomAccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If you use this code, please retain this comment block.
 * @author Isak du Preez
 * isak at du-preez dot com
 * www.du-preez.com
 */

public class CircularArrayList<E> extends AbstractList<E> implements Queue<E>,RandomAccess {
	private static final Logger LOGGER = LoggerFactory.getLogger(CircularArrayList.class);
	
	// Some sanity testing
	public static void main(String[] args) {
		CircularArrayList<String> list = new CircularArrayList<String>(10);
		for (int i = 0; i < 20; i++) {
			list.add("" + i);
		}
		
		for (int i = 0; i < 10; i++) {
			LOGGER.debug("item: " + list.get(i) + ", size: " + list.size());
		}
		LOGGER.debug("done");
	}
	
	private final int n; // buffer length
	private final List<E> buf; // a List implementing RandomAccess
	private int head = 0;
	private int tail = 0;

	public CircularArrayList(int capacity) {
	    n = capacity + 1;
	    buf = new ArrayList<E>(Collections.nCopies(n, (E) null));
	}
	
	public int capacity() {
	    return n - 1;
	}
	
	private int wrapIndex(int i) {
	    int m = i % n;
	    if (m < 0) { // java modulus can be negative
	        m += n;
	    }
	    return m;
	}
	
	// This method is O(n) but will never be called if the
	// CircularArrayList is used in its typical/intended role.
	private void shiftBlock(int startIndex, int endIndex) {
	    assert (endIndex > startIndex);
	    for (int i = endIndex - 1; i >= startIndex; i--) {
	        set(i + 1, get(i));
	    }
	}
	
	@Override
	public int size() {
	    return tail - head + (tail < head ? n : 0);
	}
	
	@Override
	public E get(int i) {
	    if (i < 0 || i >= size()) {
	        throw new IndexOutOfBoundsException();
	    }
	    return buf.get(wrapIndex(head + i));
	}
	
	@Override
	public E set(int i, E e) {
	    if (i < 0 || i >= size()) {
	        throw new IndexOutOfBoundsException();
	    }
	    return buf.set(wrapIndex(head + i), e);
	}
	
	@Override
	public boolean add(E e) {
	    if (size() == n - 1) {
	        remove();
	    }
		add(size(), e);
		return true;
	}
	
	@Override
	public void add(int i, E e) {
	    int size = size();
	    
	    if (size == n - 1) {
	        throw new IllegalStateException("Cannot add element."
	                + " CircularArrayList is filled to capacity.");
	    }
	    
	    if (i < 0 || i > size) {
	        throw new IndexOutOfBoundsException();
	    }
	    
	    tail = wrapIndex(tail + 1);
	    
	    if (i < size) {
	        shiftBlock(i, size);
	    }
	    
	    set(i, e);
	}
	
	@Override
	public E remove(int i) {
	    int s = size();
	    
	    if (i < 0 || i >= s) {
	        throw new IndexOutOfBoundsException();
	    }
	    
	    E e = get(i);
	    
	    if (i > 0) {
	        shiftBlock(0, i);
	    }
	    
	    head = wrapIndex(head + 1);
	    
	    return e;
	}
	
	@Override
	public E element() throws IllegalStateException {
		if (size() == 0)
			throw new IllegalStateException("List is empty");
			
		return buf.get(head);
	}

	@Override
	public boolean offer(E e) {
		add(e);
		return true;
	}

	@Override
	public E peek() throws IllegalStateException {
		if (size() == 0)
			return null;
			
		return get(size()-1);
	}
	
	public E peekHead() throws IllegalStateException {
		if (size() == 0)
			throw new IllegalStateException("List is empty");
			
		return get(0);
	}

	@Override
	public E poll() {
		if (size() != 0)
			return buf.get(head);
		return null;
	}

	@Override
	public E remove() {
		return remove(0);
	}
	
	@Override
	public void clear() {
		buf.clear();
		buf.addAll(Collections.nCopies(n, (E) null));
		head = 0;
		tail = 0;
	}
}