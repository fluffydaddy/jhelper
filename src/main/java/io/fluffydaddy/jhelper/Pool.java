/*
 * Copyright © 2024 fluffydaddy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluffydaddy.jhelper;

import io.fluffydaddy.jutils.Array;

import java.util.List;

public class Pool<T> {
    public static final int DEFAULT_MAX_SIZE = 32;
    public static final int DEFAULT_CAPACITY = 16;
    
    protected final Array<T> _objects;
    
    private Factory<T> factory;
    private int maxSize;
    private int useSize;
    
    public Pool() {
        this(DEFAULT_CAPACITY);
    }
    
    public Pool(int sizeBuffer) {
        this(sizeBuffer, null);
    }
    
    public Pool(int sizeBuffer, Factory<T> fact) {
        _objects = new Array<>();
        maxSize = sizeBuffer;
        factory = fact;
    }
    
    public void set(int size, Factory<T> fact) {
        maxSize = size;
        factory = fact;
    }
    
    public void set(int size) {
        set(size, factory);
    }
    
    public void set(Factory<T> fact) {
        set(maxSize, fact);
    }
    
    public int getMaxSize() {
        return maxSize;
    }
    
    public int getUseSize() {
        return useSize;
    }
    
    public Factory<T> getFactory() {
        return factory;
    }
    
    /**
     * Создаем объект если нет доступных объектов для изменения
     * <p>
     *  Если есть то последний объект удаляется и вы получите его.
     * </p>
     **/
    
    public T obtain(Object... args) {
        if (args == null || args.length == 0) return _objects.isEmpty() ? factory.acquire() : _objects.pop();
        else return _objects.isEmpty() ? factory.acquire(args) : _objects.pop();
    }
    
    private boolean freeInternal(T object, int size) {
        if (object == null) return false;
        
        if (_objects.contains(object)) throw new IllegalStateException("Object already instance exists");
        
        if (object instanceof Poolable) ((Poolable) object).reset();
        
        if (size < maxSize) {
            _objects.add(object);
            useSize++;
            return true;
        }
        
        return false;
    }
    
    public boolean release(T object) {
        return freeInternal(object, _objects.size());
    }
    
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
     * Сбрасывает объект из памяти.
     */
    public void free() {
        for (int i = 0, cnt = _objects.size(); i < cnt; i++) {
            T object = _objects.get(i);
            if (object instanceof Poolable) ((Poolable) object).reset();
        }
    }
    
    /**
     * Сбрасывает и удаляет объект из памяти.
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
    
    public interface Poolable {
        void reset();
    }
    
    public interface Factory<T> {
        T acquire(Object... args);
        
        T acquire();
    }
}