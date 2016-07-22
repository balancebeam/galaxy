package io.anyway.galaxy.convert;

public interface Convert<T> {
	String toString( T t);
	T toTarget(String str);
}
