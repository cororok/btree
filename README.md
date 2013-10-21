btree
=====

B-Tree implementation with Java.


It implements B-Tree. See http://en.wikipedia.org/wiki/B-tree. It is compatible with standard {@link java.util.Set}. It uses an Array to reduce overhead of memory allocation of LinkedList that is easier to handle the overflow and join/merge operation. Because it uses an array when it adds a key it should shift all keys larger than the key. If node will be full it splits node first then adds the key later to reduce shift operation. It uses stacks to avoid recursive calls.
