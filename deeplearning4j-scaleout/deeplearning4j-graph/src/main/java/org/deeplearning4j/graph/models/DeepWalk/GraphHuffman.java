package org.deeplearning4j.graph.models.deepwalk;

import lombok.AllArgsConstructor;
import org.deeplearning4j.graph.models.BinaryTree;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * A Huffman tree specifically for graphs.
 * Vertices in graph are indexed by an integer, 0 to nVertices-1
 */
public class GraphHuffman implements BinaryTree {

    private static final int MAX_CODE_LENGTH = 64;
    private final long[] codes;
    private final byte[] codeLength;
    private final int[][] innerNodePathToLeaf;


    public GraphHuffman(int nVertices) {
        codes = new long[nVertices];
        codeLength = new byte[nVertices];
        innerNodePathToLeaf = new int[nVertices][0];
    }

    public void buildTree(int[] nodeDegree) {


        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (int i = 0; i < nodeDegree.length; i++) pq.add(new Node(i, nodeDegree[i], null, null));

        while (pq.size() > 1) {
            Node left = pq.remove();
            Node right = pq.remove();
            Node newNode = new Node(-1, left.count + right.count, left, right);
            pq.add(newNode);
        }

        //Eventually: only one node left -> full tree
        Node tree = pq.remove();

        //Now: convert tree into binary codes. Traverse tree (preorder traversal) -> record path (left/right) -> code
        int[] innerNodePath = new int[MAX_CODE_LENGTH];
        traverse(tree, 0L, (byte) 0, -1, innerNodePath, 0);
    }

    @AllArgsConstructor
    private static class Node implements Comparable<Node> {
        private final int vertexIdx;
        private final long count;
        private Node left;
        private Node right;

        @Override
        public int compareTo(Node o) {
            return Long.compare(count, o.count);
        }
    }

    private int traverse(Node node, long codeSoFar, byte codeLengthSoFar, int innerNodeCount, int[] innerNodePath,
                         int currDepth ) {
        if (codeLengthSoFar >= 64) throw new RuntimeException("Cannot generate code: code length exceeds 64 bits");
        if (node.left == null && node.right == null) {
            //Leaf node
            codes[node.vertexIdx] = codeSoFar;
            codeLength[node.vertexIdx] = codeLengthSoFar;
            innerNodePathToLeaf[node.vertexIdx] = Arrays.copyOf(innerNodePath,currDepth);
            return innerNodeCount;
        }

        //This is an inner node. It's index is 'innerNodeCount'
        innerNodeCount++;
        innerNodePath[currDepth] = innerNodeCount;

        long codeLeft = setBit(codeSoFar, codeLengthSoFar, false);
        innerNodeCount = traverse(node.left, codeLeft, (byte) (codeLengthSoFar + 1), innerNodeCount,
                innerNodePath, currDepth+1);

        long codeRight = setBit(codeSoFar, codeLengthSoFar, true);
        innerNodeCount = traverse(node.right, codeRight, (byte) (codeLengthSoFar + 1), innerNodeCount,
                innerNodePath, currDepth+1);
        return innerNodeCount;
    }

    private long setBit(long in, int bitNum, boolean value) {
        if (value) return (in | 1L << bitNum);  //Bit mask |: 00010000
        else return (in & ~(1 << bitNum));     //Bit mask &: 11101111
    }

    private boolean getBit(long in, int bitNum) {
        long mask = 1L << bitNum;
        return (in & mask) != 0L;
    }

    @Override
    public long getCode(int vertexNum) {
        return codes[vertexNum];
    }

    @Override
    public int getCodeLength(int vertexNum) {
        return codeLength[vertexNum];
    }

    @Override
    public String getCodeString(int vertexNum) {
        long code = codes[vertexNum];
        int len = codeLength[vertexNum];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(getBit(code, i) ? "1" : "0");

        return sb.toString();
    }

    @Override
    public int[] getPathInnerNodes(int vertexNum){
        return innerNodePathToLeaf[vertexNum];
    }
}
