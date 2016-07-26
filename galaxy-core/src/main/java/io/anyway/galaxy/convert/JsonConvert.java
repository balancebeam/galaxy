package io.anyway.galaxy.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.gson.Gson;

import io.anyway.galaxy.domain.TransactionInfo;

public class JsonConvert<T> implements Convert<T> {

	@Override
	public String toString(T t) {
		return new Gson().toJson(t);
		
		
	}

	@Override
	public T toTarget(String str) {
		
		//return (T) JSON.parseObject(str, getClass());
		
		Type t = getGenericParamType(this.getClass());
		return (T) new Gson().fromJson(str, t);
	}

	static Type getGenericParamType(Class<?> clazz) {
		Type mySuperClass = clazz.getGenericInterfaces()[0];
		return ((ParameterizedType) mySuperClass).getActualTypeArguments()[0];
	}
	
	
	public static void main(String args[]){
		
		Convert<TransactionInfo> c = new JsonConvert<TransactionInfo>();
		
		TransactionInfo t = new TransactionInfo();
		t.setBusinessId(1);
		t.setBusinessType("ddd");
		
		System.out.println(c.toString(t));
		TransactionInfo t1 =  (TransactionInfo) c.toTarget(c.toString(t));
				
		System.out.println(t1);
		
		
	}
}
