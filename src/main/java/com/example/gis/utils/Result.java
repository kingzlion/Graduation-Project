package com.example.gis.utils;


import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zr
 *
 */
public final class Result {
	private Integer code=-1;
	private String message="";
	private List<Object> data=new ArrayList<Object>();
	protected Result() {}
	public Integer getCode() {
		return code;
	}
	public Result setCode(Integer code) {
		this.code = code;
		return this;
	}
	public String getMessage() {
		return message;
	}
	public Result setMessage(String message) {
		this.message = message;
		return this;
	}
	public Object getData() {
		if(this.data.size()==1){
			return this.getData(0);
		}
		return this.data;

	}
	public Result clearData() {
		data.clear();
		return this;
	}
	@SuppressWarnings("unchecked")
	public <T> T getData(Integer index) {
		if(index>=data.size()) {
			return null;
		}
		return (T) data.get(index);
	}
	public Result addData(Object...data) {
		for(Object obj:data) {
			this.data.add(obj);
		}
		return this;
	}
	public Result setData(Object...data) {
		this.addData(data);
		return this;
	}

	/**
	 * @deprecated
	 * @see Result#success(Object...)
	 */
	public static Result ok(Object...data) {
		return custom("success", 200, data);
	}
	public static Result success(Object...data) {
		return ok(data);
	}
	public static Result success(Integer code,Object...data) {
		return custom("success", code, data);
	}
	public static Result success(String msg,Object...data) {
		return custom(msg, 200, data);
	}
	public static Result custom(String msg,Integer code,Object...args) {
		Result result=new Result();
		result.setMessage(msg);
		result.setCode(code);
		result.setData(args);
		return result;
	}
}
