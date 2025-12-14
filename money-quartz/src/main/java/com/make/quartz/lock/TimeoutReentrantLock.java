package com.make.quartz.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 带超时机制的可重入锁实现类
 * <p>
 * 该类是对ReentrantLock的封装，增加了获取锁的超时控制功能，
 * 避免线程在获取锁时无限期等待，提高系统的稳定性和可预测性
 * </p>
 */
public class TimeoutReentrantLock implements Lock {
    /**
     * 内部ReentrantLock实例
     * <p>
     * 用于实际执行锁操作的核心锁对象
     * </p>
     */
    private final ReentrantLock internalLock;
    
    /**
     * 锁获取超时时间
     * <p>
     * 当尝试获取锁超过此时间仍未成功时，将抛出超时异常
     * </p>
     */
    private final long timeout;  
    
    /**
     * 超时时间单位
     * <p>
     * 用于指定超时时间的单位，如秒、毫秒等
     * </p>
     */
    private final TimeUnit unit; 

    /**
     * 构造函数：初始化时设置超时时间和时间单位
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     */
    public TimeoutReentrantLock(long timeout, TimeUnit unit) {
        // 初始化内部ReentrantLock实例
        this.internalLock = new ReentrantLock();
        // 设置锁获取超时时间
        this.timeout = timeout;
        // 设置超时时间单位
        this.unit = unit;
    }

    /**
     * 获取锁，如果超过指定时间仍未获取到锁则抛出超时异常
     * <p>
     * 该方法通过调用内部锁的tryLock方法实现带超时的锁获取
     * </p>
     *
     * @throws LockTimeoutException                  获取锁超时时抛出
     * @throws LockAcquisitionInterruptedException 获取锁过程中被中断时抛出
     */
    @Override
    public void lock() {
        try {
            // 尝试在指定时间内获取锁
            if (!internalLock.tryLock(timeout, unit)) {
                // 如果超时仍未获取到锁，抛出超时异常
                throw new LockTimeoutException("获取锁超时");
            }
        } catch (InterruptedException e) {
            // 设置当前线程的中断状态
            Thread.currentThread().interrupt();
            // 抛出锁获取被中断异常
            throw new LockAcquisitionInterruptedException("锁获取被中断", e);
        }
    }

    /**
     * 释放锁
     * <p>
     * 委托给内部ReentrantLock实例的unlock方法执行
     * </p>
     */
    @Override
    public void unlock() {
        // 调用内部锁的unlock方法释放锁
        internalLock.unlock();
    }

    /**
     * 尝试获取锁，立即返回结果不等待
     * <p>
     * 委托给内部ReentrantLock实例的tryLock方法执行
     * </p>
     *
     * @return 如果成功获取锁返回true，否则返回false
     */
    @Override
    public boolean tryLock() {
        // 调用内部锁的tryLock方法尝试获取锁
        return internalLock.tryLock();
    }

    /**
     * 在指定时间内尝试获取锁
     * <p>
     * 委托给内部ReentrantLock实例的tryLock方法执行
     * </p>
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 如果成功获取锁返回true，否则返回false
     * @throws InterruptedException 线程在等待锁过程中被中断时抛出
     */
    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        // 调用内部锁的tryLock方法在指定时间内尝试获取锁
        return internalLock.tryLock(timeout, unit);
    }

    /**
     * 可中断地获取锁
     * <p>
     * 委托给内部ReentrantLock实例的lockInterruptibly方法执行
     * </p>
     *
     * @throws InterruptedException 线程在等待锁过程中被中断时抛出
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        // 调用内部锁的lockInterruptibly方法可中断地获取锁
        internalLock.lockInterruptibly();
    }

    /**
     * 创建一个新的Condition实例
     * <p>
     * 委托给内部ReentrantLock实例的newCondition方法执行
     * </p>
     *
     * @return 新的Condition实例
     */
    @Override
    public Condition newCondition() {
        // 调用内部锁的newCondition方法创建新的Condition实例
        return internalLock.newCondition();
    }

    /**
     * 锁获取超时异常类
     * <p>
     * 当尝试获取锁超过指定时间仍未成功时抛出此异常
     * </p>
     */
    public static class LockTimeoutException extends RuntimeException {
        /**
         * 构造函数
         *
         * @param message 异常信息
         */
        public LockTimeoutException(String message) {
            // 调用父类构造函数初始化异常信息
            super(message);
        }
    }

    /**
     * 锁获取被中断异常类
     * <p>
     * 当线程在获取锁过程中被中断时抛出此异常
     * </p>
     */
    public static class LockAcquisitionInterruptedException extends RuntimeException {
        /**
         * 构造函数
         *
         * @param message 异常信息
         * @param cause   异常原因
         */
        public LockAcquisitionInterruptedException(String message, Throwable cause) {
            // 调用父类构造函数初始化异常信息和原因
            super(message, cause);
        }
    }
}