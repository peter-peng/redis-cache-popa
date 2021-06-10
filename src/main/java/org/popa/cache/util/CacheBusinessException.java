package org.popa.cache.util;

/**
 * 自定义业务异常
 * 
 * @author peterpeng
 *
 */
public class CacheBusinessException extends Exception {

    private static final long serialVersionUID = -7845792148395957369L;

    public CacheBusinessException(String msg) {
	super(msg);
    }

}
