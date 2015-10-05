/*
 * GNU GENERAL PUBLIC LICENSE
 Version 2, June 1991

 */
package cororok.btree;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * It implements B-Tree. See http://en.wikipedia.org/wiki/B-tree. It is compatible with standard {@link java.util.Set}.
 * It uses an Array to reduce overhead of memory allocation of LinkedList that is easier to handle the overflow and
 * join/merge operation. Because it uses an array when it adds a key it should shift all keys larger than the key. If
 * node will be full it splits node first then adds the key later to reduce shift operation. It uses stacks to avoid
 * recursive calls.
 * 
 * @author songduk.park cororok@gmail.com
 */
public class BTreeSet<K extends Comparable<K>> extends AbstractSet<K> {
	int count = 0;
	final int MAX_KEY;
	final int HALF_KEY;
	final int CENTER_KEY;
	final int CENTER_CHILDREN;
	final int MAX_CHILDREN;
	final int HALF_CHILDREN;

	Node root;
	int size;
	int height = 1;
	int changed = 0;

	Stack<Node> addStack = new Stack<Node>();
	Stack<WrappedNode> deleteStack = new Stack<WrappedNode>();
	Stack<WrappedNode> minStack = new Stack<WrappedNode>();
	Stack<WrappedNode> maxStack = new Stack<WrappedNode>();

	public BTreeSet(int maxSizeOfKeys) {
		this.MAX_KEY = maxSizeOfKeys;
		this.MAX_CHILDREN = maxSizeOfKeys + 1;
		this.HALF_CHILDREN = (MAX_CHILDREN + 1) / 2;
		this.HALF_KEY = MAX_KEY / 2;
		this.CENTER_KEY = maxSizeOfKeys / 2 - 1;
		this.CENTER_CHILDREN = CENTER_KEY + 1;

		this.root = new Node();
	}

	@Override
	public Iterator<K> iterator() {
		return new KeyIterator();
	}

	/**
	 * @param key
	 * @return returns value, null if it can't find the key.
	 */
	protected K get(final K key) {
		if (key == null)
			return null;

		Node node = root;
		int index = 0;
		while (true) {
			index = node.indexOfGreatestLessThan(key);
			if (index < 0)
				return node.keyAt(node.convertToRealIndex(index));
			else if (node.isLeaf())
				return null;
			else
				node = node.childAt(index);
		}
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public int size() {
		return size;
	}

	public int height() {
		return height;
	}

	@Override
	public boolean contains(final Object keyObj) {
		if (keyObj == null)
			return false;

		K key = (K) (keyObj);
		return findNode(this.root, key) != null;
	}

	@Override
	public void clear() {
		this.root = new Node();
		this.size = 0;
		this.height = 0;
		++changed;
	}

	/**
	 * search a node containing the key from the nod to sub leaves
	 * 
	 * @param fromNode
	 * @param key
	 * @return null if can't find the key or Node
	 */
	private Node findNode(Node fromNode, K key) {
		Node node = fromNode;
		int index = 0;
		while (true) {
			index = node.indexOfGreatestLessThan(key);
			if (index < 0)
				return node;
			else if (node.isLeaf())
				return null;
			else
				node = node.childAt(index);
		}
	}

	@Override
	public boolean add(K newKey) {
		return returnExistingKeyOrAdd(newKey) == null;
	}

	/**
	 * if the same key exists return the key and does not add a newKey. It is used to replace old key
	 * 
	 * @param newKey
	 * @return return old key if the key exists or add newKey and return null
	 */
	K returnExistingKeyOrAdd(K newKey) {
		addStack.reset();
		int indexOfGreatestLessThan = 0;
		Node currentNode = root;
		while (true) { // build a stack until leap
			indexOfGreatestLessThan = currentNode.indexOfGreatestLessThan(newKey);
			if (indexOfGreatestLessThan < 0) {
				// fond old one
				return currentNode.keyAt(currentNode.convertToRealIndex(indexOfGreatestLessThan));
			}
			addStack.add(currentNode);
			if (currentNode.isLeaf())
				break;
			currentNode = currentNode.childAt(indexOfGreatestLessThan);
		}

		++size;
		++changed;
		addFromTheBotton(newKey);
		return null;
	}

	private void addFromTheBotton(K newKey) {
		Node currentNode = null;
		WrappedNode wrappedNode = new WrappedNode();
		while (addStack.size() > 0) {
			currentNode = addStack.pop();
			wrappedNode = currentNode.add(newKey, wrappedNode.node);
			if (wrappedNode == null) // no overflow
				return;
			// was overflowed so need to add it to the parent.
			newKey = wrappedNode.key;
		}

		// if top has a node it has to create a new root
		createNewRoot(currentNode, wrappedNode);
	}

	private void createNewRoot(Node currentNode, WrappedNode wrappedNode) {
		Node newRoot = new Node();
		newRoot.setKeyAt(0, wrappedNode.key);
		newRoot.noOfKeys = 1;
		newRoot.initChildren();
		newRoot.setChildAt(0, currentNode);
		newRoot.setChildAt(1, wrappedNode.node);
		newRoot.noOfChildren = 2;

		this.root = newRoot;
		++height;
	}

	/**
	 * @param key
	 * @return true if it finds key or false if it doesn't find the key
	 */
	public boolean remove(K key) {
		deleteStack.reset();
		int indexOfGreatestLessThan = 0;
		Node currentNode = root;
		while (true) {
			indexOfGreatestLessThan = currentNode.indexOfGreatestLessThan(key);
			WrappedNode wrapper = new WrappedNode(currentNode);
			if (indexOfGreatestLessThan < 0) {// found
				wrapper.index = currentNode.convertToRealIndex(indexOfGreatestLessThan);
				deleteStack.add(wrapper);

				// if the found key is leaf, delete the key.
				if (currentNode.isLeaf()) {
					currentNode.removeKeyAt(wrapper.index);
				} else {
					overwriteWithLeaf(currentNode, wrapper.index);
				}
				--size;
				++changed;
				break;
			} else if (currentNode.isLeaf()) {
				return false; // no key found
			}
			wrapper.index = indexOfGreatestLessThan;
			deleteStack.add(wrapper);
			currentNode = currentNode.childAt(indexOfGreatestLessThan);
		}

		if (deleteStack.size() == 1) // root
			return true;

		merge();
		return true;
	}

	/**
	 * replace a key in currentNode with the least key or the largest key in the leaf. and delete the key in the leaf.
	 * 
	 * @param currentNode
	 * @param indexToDelete
	 */
	private void overwriteWithLeaf(Node currentNode, int indexToDelete) {
		minStack.reset();
		maxStack.reset();
		Node minNode = findMinNode(currentNode.childAt(indexToDelete + 1));
		Node maxNode = findMaxNode(currentNode.childAt(indexToDelete));

		// swap, use one which has more keys to reduce restructuring later.
		K swap = null;
		Stack<WrappedNode> target = null;
		if (minNode.noOfKeys <= maxNode.noOfKeys) {
			swap = maxNode.removeLastKey();
			target = maxStack;
		} else {
			swap = minNode.removeFirstKey();
			target = minStack;
		}
		currentNode.setKeyAt(indexToDelete, swap);
		deleteStack.addAll(target);
	}

	/**
	 * merges insufficient nodes from down to top. It doesn't use recursive call but uses a stack.
	 */
	private void merge() {
		WrappedNode current = deleteStack.pop();
		WrappedNode parent = null;
		while (deleteStack.size() > 0) {
			parent = deleteStack.pop();
			if (current.node.isInsufficientKey()) {
				WrappedNode borrow = getBiggerChild(parent.node, parent.index);
				boolean isRight = borrow.index == 1;

				if (canJoin(borrow, current)) {
					if (isRight) {
						join(current.node, parent.node, parent.index, borrow.node);
					} else {
						join(borrow.node, parent.node, parent.index - 1, current.node);
					}
				} else {
					borrow(current, parent, borrow, isRight);
					return;
				}
				current = parent;
			} else {
				break;
			}
		}

		// root
		if (parent.node.noOfKeys == 0 && parent.node.isLeaf() == false) {
			this.root = parent.node.childAt(0);
			--height;
		}
	}

	private boolean canJoin(WrappedNode borrow, WrappedNode node) {
		return borrow.node.noOfKeys + node.node.noOfKeys < MAX_KEY;
	}

	private void borrow(WrappedNode current, WrappedNode parent, WrappedNode borrow, boolean isRight) {
		if (isRight) {
			shrinkRightKeys(current, parent, borrow);
		} else {
			shrinkLeftKeys(current, parent, borrow);
		}
	}

	private void shrinkLeftKeys(WrappedNode current, WrappedNode parent, WrappedNode borrow) {
		int parentIndex = parent.index - 1;
		ArrayUtil.shiftRight(current.node.keys, 0, current.node.noOfKeys);
		current.node.setKeyAt(0, parent.node.keyAt(parentIndex));
		++current.node.noOfKeys;

		parent.node.setKeyAt(parentIndex, borrow.node.removeLastKey());
		if (borrow.node.isLeaf() == false) {
			ArrayUtil.shiftRight(current.node.children, 0, current.node.noOfChildren);
			current.node.setChildAt(0, borrow.node.removeLastChild());

			++current.node.noOfChildren;
		}
	}

	private void shrinkRightKeys(WrappedNode current, WrappedNode parent, WrappedNode borrow) {
		int parentIndex = parent.index;
		current.node.setKeyAt(current.node.noOfKeys, parent.node.keyAt(parentIndex));
		++current.node.noOfKeys;

		parent.node.setKeyAt(parentIndex, borrow.node.removeFirstKey());
		if (borrow.node.isLeaf() == false) {
			current.node.setChildAt(current.node.noOfChildren, borrow.node.removeFirstChild());
			++current.node.noOfChildren;
		}
	}

	/**
	 * debug purpose
	 */
	public void showInfo() {
		System.out.println("MAX_KEY=" + MAX_KEY);
		System.out.println("HALF_KEY=" + HALF_KEY);
		System.out.println("CENTER_KEY=" + CENTER_KEY);
		System.out.println("CENTER_CHILDREN=" + CENTER_CHILDREN);
		System.out.println("MAX_CHILDREN=" + MAX_CHILDREN);
		System.out.println("HALF_CHILDREN=" + HALF_CHILDREN);
	}

	/**
	 * choose a bigger child node between left and right child of key at index.
	 * 
	 * @param parent
	 * @param index index of the key to find its left or right child.
	 * @return left or right child node
	 */
	private WrappedNode getBiggerChild(Node parent, int index) {
		Node left = null;
		Node right = null;
		WrappedNode wrappedNode = new WrappedNode();
		if (index > 0)
			left = parent.childAt(index - 1);

		if (index + 1 < MAX_CHILDREN)
			right = parent.childAt(index + 1);

		if (left == null) {
			wrappedNode.node = right;
			wrappedNode.index = 1;
		} else if (right == null) {
			wrappedNode.node = left;
			wrappedNode.index = 0;
		} else {
			// choose bigger one
			if (left.noOfKeys >= right.noOfKeys) {
				wrappedNode.node = left;
				wrappedNode.index = 0;
			} else {
				wrappedNode.node = right;
				wrappedNode.index = 1;
			}
		}
		return wrappedNode;
	}

	/**
	 * appends right node to left node
	 * 
	 * @param left
	 * @param center parent node between left and right
	 * @param centerIndex
	 * @param right
	 */
	private void join(Node left, Node center, int centerIndex, Node right) {
		left.setKeyAt(left.noOfKeys, center.keyAt(centerIndex));
		++left.noOfKeys;

		joinKeys(left, right);
		joinChildren(left, right);

		// shrink parent
		center.shrink(centerIndex + 1);
	}

	private void joinChildren(Node left, Node right) {
		for (int i = 0; i < right.noOfChildren; i++) {
			left.setChildAt(i + left.noOfChildren, right.childAt(i));
		}
		left.noOfChildren += right.noOfChildren;
	}

	private void joinKeys(Node left, Node right) {
		for (int i = 0; i < right.noOfKeys; i++) {
			left.setKeyAt(i + left.noOfKeys, right.keyAt(i));
		}
		left.noOfKeys += right.noOfKeys;
	}

	/**
	 * find the maximum node from a node.
	 * 
	 * @param node
	 * @return the maximum node
	 */
	private Node findMaxNode(Node node) {
		while (true) {
			WrappedNode wrappedNode = new WrappedNode();
			wrappedNode.node = node;
			wrappedNode.index = node.noOfChildren - 1;
			maxStack.add(wrappedNode);
			if (node.isLeaf())
				return node;

			node = node.childAt(node.noOfChildren - 1); // It's the maximum node
														// in a node
		}
	}

	/**
	 * find the minimum node from a node.
	 * 
	 * @param node starting point
	 * @return the minimum node
	 */
	private Node findMinNode(Node node) {
		while (true) {
			WrappedNode wrapper = new WrappedNode();
			wrapper.node = node;
			wrapper.index = 0;
			minStack.add(wrapper);
			if (node.isLeaf())
				return node;

			node = node.childAt(0); // It's the minimum in a node
		}
	}

	/**
	 * debug purpose
	 */
	public void debug() {
		root.print();
	}

	/**
	 * Iterator that contains keys.
	 */
	private class KeyIterator implements Iterator<K> {
		WrappedNode wrappedNode;
		K currentKey = null;
		private Stack<WrappedNode> stack = new Stack<WrappedNode>();
		int indexOfPrinting = 0;
		final int changedAt = changed;

		KeyIterator() {
			wrappedNode = new WrappedNode(root);
			getNext();
		}

		@Override
		public boolean hasNext() {
			return currentKey != null;
		}

		/**
		 * find the next node
		 */
		private void getNext() {
			do {
				if (wrappedNode.node.isLeaf()) {
					if (wrappedNode.node.noOfKeys > indexOfPrinting) {
						currentKey = wrappedNode.node.keyAt(indexOfPrinting++);
						break;
					}
					indexOfPrinting = 0;
					if (stack.size() == 0) {
						currentKey = null;
						break;
					}
					wrappedNode = stack.pop();
					wrappedNode.check = true;
				} else if (wrappedNode.check) {
					// check it returned all node
					if (wrappedNode.index == wrappedNode.node.noOfKeys) {
						if (stack.size() == 0) {
							currentKey = null;
							break;
						}
						wrappedNode = stack.pop();
						wrappedNode.check = true;
					} else { // return one by one in the current node
						currentKey = wrappedNode.node.keyAt(wrappedNode.index++);
						stack.add(wrappedNode);
						WrappedNode temp = new WrappedNode(wrappedNode.node.childAt(wrappedNode.index));
						wrappedNode = temp;
						break;
					}
				} else {
					stack.add(wrappedNode);
					WrappedNode temp = new WrappedNode(wrappedNode.node.childAt(0));
					wrappedNode = temp;
				}
			} while (true);
		}

		@Override
		public K next() {
			if (changedAt != changed)
				throw new ConcurrentModificationException();

			if (currentKey == null)
				throw new NoSuchElementException();

			K temp = currentKey;
			getNext();
			return temp;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * It uses WrappedNode to reduce unnecessary members in Node.
	 */
	class WrappedNode {
		Node node;
		int index;
		K key;
		boolean check;

		public WrappedNode() {
		}

		public WrappedNode(Node node) {
			this.node = node;
		}

		public WrappedNode(Node node, int index) {
			this(node);
			this.index = index;
		}
	}

	/**
	 * Keeps keys in an array rather than linked list to reduce memory use. Number of children is one bigger than keys
	 * because child can be placed on the left and right side of the parent.
	 */
	class Node {
		int id;
		int noOfKeys = 0;
		int noOfChildren = 0;

		K[] keys;
		Node[] children;

		public Node() {
			this.keys = (K[]) new Comparable<?>[MAX_KEY];
			this.id = ++count;
		}

		public void initChildren() {
			this.children = (Node[]) Array.newInstance(this.getClass(), MAX_CHILDREN);
		}

		/**
		 * except root node should keep half or more than key
		 * 
		 * @return
		 */
		public boolean isInsufficientKey() {
			return noOfKeys < HALF_KEY;
		}

		boolean isValidToDeleteKey() {
			return noOfKeys != 0;
		}

		/**
		 * delete the key and return the old key.
		 * 
		 * @param indexToDelete
		 * @return null if the key does not exist.
		 */
		K removeKeyAt(int indexToDelete) {
			if (isLeaf() == false || noOfKeys <= indexToDelete)
				return null;

			K deletedKey = keys[indexToDelete];
			ArrayUtil.shiftLeft(keys, indexToDelete + 1, noOfKeys);

			keys[noOfKeys - 1] = null;
			--noOfKeys;
			return deletedKey;
		}

		/**
		 * @return null if there is no key.
		 */
		K removeFirstKey() {
			if (isValidToDeleteKey() == false)
				return null;

			K deletedKey = keys[0];
			ArrayUtil.shiftLeft(keys, 1, noOfKeys);
			--noOfKeys;
			return deletedKey;
		}

		/**
		 * @return null if there is no child
		 */
		Node removeFirstChild() {
			if (noOfChildren == 0)
				return null;

			Node deletedNode = children[0];
			ArrayUtil.shiftLeft(children, 1, noOfChildren);
			--noOfChildren;
			return deletedNode;
		}

		/**
		 * @return null if there is no key
		 */
		K removeLastKey() {
			if (isValidToDeleteKey() == false)
				return null;

			K deletedKey = keys[--noOfKeys];
			keys[noOfKeys] = null;
			return deletedKey;
		}

		/**
		 * @return null if there is no child
		 */
		Node removeLastChild() {
			if (noOfChildren == 0)
				return null;

			Node deletedNode = children[--noOfChildren];
			children[noOfChildren] = null;
			return deletedNode;
		}

		private Node childAt(int childIndex) {
			return children[childIndex];
		}

		private void setChildAt(int childIndex, Node child) {
			children[childIndex] = child;
		}

		private K keyAt(int keyIndex) {
			return keys[keyIndex];
		}

		private void setKeyAt(int keyIndex, K key) {
			keys[keyIndex] = key;
		}

		public boolean isLeaf() {
			return noOfChildren == 0;
		}

		public boolean isFull() {
			return noOfKeys == MAX_KEY;
		}

		/**
		 * see the method {@link java.util.List#add(index,element)}.
		 * 
		 * @param position
		 * @param newNode
		 */
		private void addChild(int position, Node newNode) {
			ArrayUtil.shiftRight(children, position, noOfChildren);

			children[position] = newNode;
			++noOfChildren;
		}

		/**
		 * @param key
		 * @param childNode
		 * @return null it it is not full or new right node separated because of insertion.
		 */
		public WrappedNode add(K key, Node childNode) {
			int indexOfNew = indexOfGreatestLessThan(key);
			if (isFull())
				return split(key, childNode, indexOfNew);

			if (childNode != null)
				addChild(indexOfNew + 1, childNode);

			// shift from idxGreatestLess
			ArrayUtil.shiftRight(keys, indexOfNew, noOfKeys);
			keys[indexOfNew] = key;
			++noOfKeys;
			return null;
		}

		/**
		 * split current node to left and right node which is created newly and add key and childNode to either left or
		 * right.
		 * 
		 * @param key
		 * @param childNode
		 * @param indexOfNew index where key and childNode will be placed.
		 * @return new right side node which will be added to the parent.
		 */
		private WrappedNode split(K key, Node childNode, int indexOfNew) {
			WrappedNode wrappedNode = splitKeys(key, indexOfNew);
			if (childNode != null) {
				splitChildren(indexOfNew, wrappedNode.node, childNode);
			}
			return wrappedNode;
		}

		/**
		 * If a key exists returns (index - size) that is negative value.
		 * 
		 * @param key
		 * @return negative value if there is the key or index of the greatest but smaller key than the key.
		 */
		private int indexOfGreatestLessThan(K key) {
			int left = 0;
			int right = noOfKeys - 1;
			// use binary search
			while (true) {
				if (left > right) {
					return left;
				}
				int middle = (left + right) / 2;
				int diff = key.compareTo(keys[middle]); // Arrays.binarySearch(keys, key) will throw NullPointException
				if (diff == 0)
					return middle - MAX_KEY; // exists
				else if (diff > 0)
					left = middle + 1;
				else
					right = middle - 1;
			}
		}

		/**
		 * see the method {@link #indexOfGreatestLessThan(K key)}.
		 * 
		 * @param indexOfGreatestLessThan
		 * @return
		 */
		int convertToRealIndex(int indexOfGreatestLessThan) {
			return indexOfGreatestLessThan + MAX_KEY;
		}

		/**
		 * split child before it adds a new key because it knows it will be full.
		 * 
		 * @param indexOfNew
		 * @param seperatedNode
		 * @param newChild
		 */
		private void splitChildren(int indexOfNew, Node seperatedNode, Node newChild) {
			seperatedNode.initChildren();
			if (indexOfNew == CENTER_CHILDREN) {
				// right
				seperatedNode.children[0] = newChild;
				ArrayUtil.moveTo(children, seperatedNode.children, CENTER_CHILDREN + 1, MAX_KEY + 1, 1);
			} else if (indexOfNew < CENTER_CHILDREN) {
				// right
				ArrayUtil.moveTo(children, seperatedNode.children, CENTER_CHILDREN, MAX_CHILDREN, 0);
				// left
				ArrayUtil.shiftRight(children, indexOfNew + 1, CENTER_CHILDREN);
				children[indexOfNew + 1] = newChild;
			} else {
				// right
				ArrayUtil.moveTo(children, seperatedNode.children, CENTER_CHILDREN + 1, indexOfNew + 1, 0);
				int index = indexOfNew - CENTER_CHILDREN;
				seperatedNode.children[index] = newChild;
				ArrayUtil.moveTo(children, seperatedNode.children, indexOfNew + 1, MAX_CHILDREN, ++index);
			}
			noOfChildren = seperatedNode.noOfChildren = HALF_CHILDREN;
		}

		/**
		 * @param key
		 * @param indexOfNew
		 * @return new node for right node which should be added to parent node
		 */
		private WrappedNode splitKeys(K key, int indexOfNew) {
			WrappedNode wrappedNode = new WrappedNode();
			wrappedNode.node = new Node();
			if (indexOfNew <= CENTER_KEY) {
				wrappedNode.key = keys[CENTER_KEY];
				// left - shift
				ArrayUtil.shiftRight(keys, indexOfNew, CENTER_KEY);
				keys[indexOfNew] = key;
				// right
				moveRightHalfToNewNode(wrappedNode.node.keys);
			} else if (indexOfNew == CENTER_CHILDREN) {
				wrappedNode.key = key;
				// right
				moveRightHalfToNewNode(wrappedNode.node.keys);
			} else {
				wrappedNode.key = keys[CENTER_CHILDREN];
				keys[CENTER_CHILDREN] = null;
				// right
				int size = (indexOfNew - CENTER_CHILDREN - 1);
				ArrayUtil.moveTo(keys, wrappedNode.node.keys, CENTER_CHILDREN + 1, indexOfNew, 0);
				wrappedNode.node.keys[size] = key;
				ArrayUtil.moveTo(keys, wrappedNode.node.keys, indexOfNew, noOfKeys, ++size);
			}
			noOfKeys = wrappedNode.node.noOfKeys = HALF_KEY;
			return wrappedNode;
		}

		void moveRightHalfToNewNode(K[] newKeys) {
			ArrayUtil.moveTo(this.keys, newKeys, CENTER_KEY + 1, MAX_KEY, 0);
		}

		/**
		 * move key and child one left from 'from' to the end and reduce one size.
		 * 
		 * @param from
		 */
		void shrink(int from) {
			for (; from < noOfKeys; from++) {
				keys[from - 1] = keys[from];
				children[from] = children[from + 1];
			}
			--noOfKeys;
			keys[noOfKeys] = null;

			--noOfChildren;
			children[noOfChildren] = null;
		}

		/**
		 * debug purpose
		 */
		void print() {
			print(0);
		}

		/**
		 * debug purpose
		 */
		void print(int depth) {
			String indent = makeIndent(depth);
			System.out.print(indent + "(Id: " + id + ")" + " keys=");
			for (int i = 0; i < noOfKeys; i++) {
				System.out.print(keys[i]);
				System.out.print(' ');
			}

			if (noOfChildren > 0) {
				System.out.println(indent + " , noOfChildren=" + noOfChildren);
				++depth;
				for (int i = 0; i < noOfChildren; i++) {
					children[i].print(depth);
				}
			} else
				System.out.println();
		}

		private String makeIndent(int depth) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < depth * 2; i++) {
				sb.append(' ');
			}
			String indent = sb.toString();
			return indent;
		}

		/**
		 * debug purpose
		 */
		void printKeys() {
			if (isLeaf()) {
				for (int i = 0; i < noOfKeys; i++) {
					System.out.print(keys[i]);
					System.out.print(' ');
				}
				System.out.println();
			}
		}

		/**
		 * debug purpose
		 */
		void printKeyAt(int i) {
			System.out.print(keys[i]);
			System.out.print(' ');
		}
	}
}
