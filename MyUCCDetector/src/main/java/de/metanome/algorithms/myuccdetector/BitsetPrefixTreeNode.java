package de.metanome.algorithms.myuccdetector;

import java.util.List;

import de.metanome.algorithm_helper.data_structures.ColumnCombinationBitset;

public class BitsetPrefixTreeNode {

	private int level;
	private int columnNumber;
	private ColumnCombinationBitset value;
	private BitsetPrefixTreeNode[] children;
	private BitsetPrefixTreeNode parent;
	
	public BitsetPrefixTreeNode(int level, int columnNumber){
		this(null, level, columnNumber);
	}
	
	public BitsetPrefixTreeNode(BitsetPrefixTreeNode parent, int level, int columnNumber){
		this.parent = parent;
		this.level = level;
		this.columnNumber = columnNumber;
		this.children = new BitsetPrefixTreeNode[columnNumber];
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getColumnNumber() {
		return columnNumber;
	}

	public void setColumnNumber(int columnNumber) {
		this.columnNumber = columnNumber;
	}

	public BitsetPrefixTreeNode[] getChildren() {
		return children;
	}

	public void setChildren(BitsetPrefixTreeNode[] children) {
		this.children = children;
	}

	public ColumnCombinationBitset getValue() {
		return value;
	}
	
	public void setValue(ColumnCombinationBitset value) {
		this.value = value;
	}
	
	public BitsetPrefixTreeNode getParent() {
		return parent;
	}
	
	public void setParent(BitsetPrefixTreeNode parent) {
		this.parent = parent;
	}
	
	public void addBitset(ColumnCombinationBitset bitset){
		addBitset(bitset, bitset.getSetBits());
	}
	
	protected void addBitset(ColumnCombinationBitset bitset, List<Integer> columnIndices){
		if(bitset.size() <= level){
			setValue(bitset);
		}
		else{
			if(children[columnIndices.get(level)] == null){
				children[columnIndices.get(level)] = new BitsetPrefixTreeNode(this, level + 1, columnNumber);
			}
			BitsetPrefixTreeNode child = children[columnIndices.get(level)];
			child.addBitset(bitset, columnIndices);
		}
	}
	
	public void addBitsets(Iterable<ColumnCombinationBitset> bitsets){
		for(ColumnCombinationBitset bitset:bitsets){
			addBitset(bitset);
		}
	}
	
	public boolean containsBitset(ColumnCombinationBitset bitset){
		return getContainingNode(bitset) != null;
	}
	
	public BitsetPrefixTreeNode getContainingNode(ColumnCombinationBitset bitset){
		return getContainingNode(bitset, bitset.getSetBits());
	}

	private BitsetPrefixTreeNode getContainingNode(ColumnCombinationBitset bitset, List<Integer> setBits) {
		if(bitset == null){
			return null;
		}
		else if(bitset.size() <= level && bitset.equals(value)){
			return this;
		}
		else{
			BitsetPrefixTreeNode child = children[setBits.get(level)];
			return child != null ? child.getContainingNode(bitset, setBits) : null;
		}
	}
	
}
