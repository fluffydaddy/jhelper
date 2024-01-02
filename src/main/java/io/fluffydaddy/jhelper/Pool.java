/**
 * A generic object pool implementation.
 *
 * <p>This class allows efficient reuse of objects to reduce memory allocation and improve
 * performance. Objects can be obtained from the pool using the {@link #obtain(Object...)} method
 * and released back to the pool using the {@link #release(Object)} method.</p>
 *
 * <p>The pool can be configured with a maximum size using {@link #set(int)} or
 * {@link #set(int, Factory)}. The default maximum size is {@value DEFAULT_MAX_SIZE}.
 * When the pool reaches its maximum size, attempting to obtain more objects will create new
 * instances instead of reusing existing ones.</p>
 *
 * <p>Objects released to the pool must implement the {@link Poolable} interface if they need
 * to be reset before being reused. The pool will call the {@link Poolable#reset()} method on
 * each object before making it available for reuse.</p>
 *
 * <p>The factory used to create new objects for the pool is specified via the {@link Factory}
 * interface. The factory is responsible for creating new instances of the object type when
 * needed by the pool.</p>
 *
 * @param <T> The type of objects held by this pool.
 * @see Poolable
 * @see Factory
 */
package io.fluffydaddy.jhelper;

import io.fluffydaddy.jutils.collection.Array;

import java.util.List;

public class Pool<T> {
    
    /**
     * The default maximum size of the pool.
     */
    public static final int DEFAULT_MAX_SIZE = 32;
    
    /**
     * The default initial capacity of the pool.
     */
    public static final int DEFAULT_CAPACITY = 16;
    
    /**
     * The underlying array used to store the objects in the pool.
     */
    protected final Array<T> _objects;
    
    private Factory<T> factory;
    private int maxSize;
    private int useSize;
    
    /**
     * Creates a new object pool with the default initial capacity.
     */
    public Pool() {
        this(DEFAULT_CAPACITY);
    }
    
    /**
     * Creates a new object pool with the specified initial capacity.
     *
     * @param sizeBuffer The initial capacity of the pool.
     */
    public Pool(int sizeBuffer) {
        this(sizeBuffer, null);
    }
    
    /**
     * Creates a new object pool with the specified initial capacity and factory.
     *
     * @param sizeBuffer The initial capacity of the pool.
     * @param fact The factory to be used for creating new instances.
     */
    public Pool(int sizeBuffer, Factory<T> fact) {
        _objects = new Array<>();
        maxSize = sizeBuffer;
        factory = fact;
    }
    
    /**
     * Sets the maximum size of the pool and the factory used for creating new instances.
     *
     * @param size The maximum size of the pool.
     * @param fact The factory to be used for creating new instances.
     */
    public void set(int size, Factory<T> fact) {
        maxSize = size;
        factory = fact;
    }
    
    /**
     * Sets the maximum size of the pool.
     *
     * @param size The maximum size of the pool.
     */
    public void set(int size) {
        set(size, factory);
    }
    
    /**
     * Sets the factory used for creating new instances.
     *
     * @param fact The factory to be used for creating new instances.
     */
    public void set(Factory<T> fact) {
        set(maxSize, fact);
    }
    
    /**
     * Retrieves the maximum size of the pool.
     *
     * @return The maximum size of the pool.
     */
    public int getMaxSize() {
        return maxSize;
    }
    
    /**
     * Retrieves the current size of the pool.
     *
     * @return The current size of the pool.
     */
    public int getUseSize() {
        return useSize;
    }
    
    /**
     * Retrieves the factory used for creating new instances.
     *
     * @return The factory used for creating new instances.
     */
    public Factory<T> getFactory() {
        return factory;
    }
    
    /**
     * Obtains an object from the pool. If there are no available objects, a new instance
     * will be created using the factory.
     *
     * @param args Additional arguments to be passed to the factory during object creation.
     * @return The obtained object.
     * @throws IllegalStateException If the factory fails to acquire a new instance.
     */
    public T obtain(Object... args) {
        if (args == null || args.length == 0) return _objects.isEmpty() ? factory.acquire() : _objects.pop();
        else return _objects.isEmpty() ? factory.acquire(args) : _objects.pop();
    }
    
    /**
     * Releases an object back to the pool. The object must implement the {@link Poolable} interface
     * if it needs to be reset before reuse.
     *
     * @param object The object to be released.
     * @return True if the object was successfully released, false otherwise.
     * @throws IllegalArgumentException If the provided object is null.
     */
    public boolean release(T object) {
        return freeInternal(object, _objects.size());
    }
    
    /**
     * Releases a list of objects back to the pool. The objects must implement the {@link Poolable}
     * interface if they need to be reset before reuse.
     *
     * @param objects The list of objects to be released.
     * @return True if the objects were successfully released, false otherwise.
     * @throws IllegalArgumentException If the provided list of objects is null.
     */
    public boolean freeAll(List<T> objects) {
        if (objects == null) throw new IllegalArgumentException("objects cannot be null.");
        
        boolean changed = false;
        int len = objects.size();
        
        for (T it : objects) {
            if (!freeInternal(it, len)) return false;
            
            changed = true;
        }
        
        return changed;
    }
    
    /**
     * Resets all objects in the pool by calling their {@link Poolable#reset()} method.
     */
    public void free() {
        for (T object : _objects) {
            if (object instanceof Poolable) ((Poolable) object).reset();
        }
    }
    
    /**
     * Releases and resets all objects in the pool.
     *
     * @throws IllegalStateException If the reset operation fails for any object.
     */
    public void release() {
        for (int i = 0, cnt = _objects.size(); i < cnt; i++) {
            T object = _objects.get(i);
            
            if (object != null) {
                if (object instanceof Poolable) ((Poolable) object).reset();
                
                _objects.remove(object);
                useSize--;
            }
        }
        
        clear();
    }
    
    /**
     * Clears the pool by releasing and resetting all objects.
     */
    public void clear() {
        release();
        
        for (T object : _objects) {
            if (object != null) {
                _objects.remove(object);
                if (object instanceof Poolable) ((Poolable) object).reset();
            }
        }
        
        _objects.clear();
    }
    
    /**
     * Internal method to free an object by returning it to the pool. The object must implement
     * the {@link Poolable} interface if it needs to be reset before reuse.
     *
     * @param object The object to be freed.
     * @param size   The current size of the pool.
     * @return True if the object was successfully freed, false otherwise.
     * @throws IllegalArgumentException If the provided object is null.
     * @throws IllegalStateException    If the object is already present in the pool.
     */
    private boolean freeInternal(T object, int size) {
        if (object == null) throw new IllegalArgumentException("object cannot be null.");
        
        if (_objects.contains(object)) throw new IllegalStateException("Object already instance exists");
        
        if (object instanceof Poolable) ((Poolable) object).reset();
        
        if (size < maxSize) {
            _objects.add(object);
            useSize++;
            return true;
        }
        
        return false;
    }
    
    /**
     * An interface to be implemented by objects that need to be reset before being reused by the pool.
     */
    public interface Poolable {
        /**
         * Resets the object to its initial state.
         */
        void reset();
    }
    
    /**
     * An interface to be implemented by factories responsible for creating new instances for the pool.
     *
     * @param <T> The type of objects to be created by the factory.
     */
    public interface Factory<T> {
        /**
         * Acquires a new instance of the object type.
         *
         * @param args Additional arguments to be used during object creation.
         * @return The newly created instance.
         */
        T acquire(Object... args);
        
        /**
         * Acquires a new instance of the object type.
         *
         * @return The newly created instance.
         */
        T acquire();
    }
}
