/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.entities;

import org.codehaus.jackson.annotate.JsonProperty;

public abstract class AbstractMetaData {
	@JsonProperty
	private String name;

	public AbstractMetaData() {
	}
	
	public AbstractMetaData(String aName) {
		this.name = aName;
	}

	public String getName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractMetaData other = (AbstractMetaData) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
}
