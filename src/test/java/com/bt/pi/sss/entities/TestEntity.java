package com.bt.pi.sss.entities;

public class TestEntity {
	private String key;
	private String value;

	public TestEntity() {
	}

	public TestEntity(String aKey, String aValue) {
		this.key = aKey;
		this.value = aValue;
	}

	public TestEntity(TestEntity another) {
		this.key = another.key;
		this.value = another.value;
	}
	
	public String getKey(){
		return this.key;
	}

	public void setKey(String k) {
		this.key = k;
	}

	public void setValue(String v) {
		this.value = v;
	}
	
	public String getValue(){
		return this.value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestEntity other = (TestEntity) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "TestEntity [key=" + key + ", value=" + value + "]";
	}
}
