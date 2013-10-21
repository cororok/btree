package cororok.btree;

import java.util.HashMap;
import java.util.Random;

/**
 * 
 * @author songduk.park cororok@gmail.com
 * 
 */
public class BTreeMapTest {
	public static void main(String[] args) {
		HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>();
		BTreeMap<Integer, Integer> treeMap = new BTreeMap<Integer, Integer>(2);

		Random random = new Random();

		int testSize = 10000;
		int doubleTestSize = testSize * 2;

		for (int i = 0; i < testSize; i++) {
			Integer key = random.nextInt(doubleTestSize);
			Integer value = key.intValue() * 10;

			if (hashMap.containsKey(key) != treeMap.containsKey(key)) {
				System.out.println("contains wrong");
				return;
			}

			if (hashMap.put(key, value) != treeMap.put(key, value)) {
				System.out.println("add wrong");
				return;
			}

			if (hashMap.size() != treeMap.size()) {
				System.out.println("size wrong");
				return;
			}
		}
		System.out.println("size=" + hashMap.size());

		System.out.println("delete test");
		int delete = hashMap.size() / 2;
		while (hashMap.size() > delete) {
			Integer key = random.nextInt(doubleTestSize);

			if (hashMap.get(key) != treeMap.get(key)) {
				System.out.println("get wrong");
				return;
			}

			if (hashMap.remove(key) != treeMap.remove(key)) {
				System.out.println("get wrong");
				return;
			}

			if (hashMap.size() != treeMap.size()) {
				System.out.println("size2 wrong");
				return;
			}
		}
		System.out.println("after delete size=" + hashMap.size());

		System.out.println("all right, done");
	}
}
