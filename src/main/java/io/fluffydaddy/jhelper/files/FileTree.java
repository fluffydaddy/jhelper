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

package io.fluffydaddy.jhelper.files;

import io.fluffydaddy.jutils.Array;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class FileTree extends Array<FileHandle> {
    public FileTree(Collection<FileHandle> cols) {
        super(cols);
    }
    
    public FileTree(FileHandle[] cols) {
        super(cols);
    }
    
    public FileTree() {
        super();
    }
    
    public static FileTree from(FileHandle dir) {
        return new FileTree(listOfFiles(dir));
    }
    
    public static FileTree from(File dir) {
        return new FileTree(listOfFiles(dir));
    }
    
    public static FileTree from(String dirPath) {
        return new FileTree(listOfFiles(dirPath));
    }
    
    public static FileTree createFrom(Iterable<FileHandle> classPath) {
        FileTree files = new FileTree();
        if (classPath != null) {
            for (FileHandle file : classPath) {
                files.add(file);
            }
        }
        return files;
    }
    
    public static Array<FileHandle> listOfFiles(Iterable<FileHandle> classPath, FileHandle root) {
        Array<FileHandle> files = createFrom(classPath);
        for (FileHandle handle : root.list()) {
            if (handle.isDirectory()) {
                files.insert(listOfFiles(handle));
            } else {
                files.add(handle);
            }
        }
        return files;
    }
    
    public static Array<FileHandle> listOfFiles(FileHandle root) {
        Array<FileHandle> files = new Array<>();
        for (FileHandle handle : root.list()) {
            if (handle.isDirectory()) {
                files.insert(listOfFiles(handle));
            } else {
                files.add(handle);
            }
        }
        return files;
    }
    
    public static Array<FileHandle> listOfFiles(File root) {
        return listOfFiles(new FileHandle(root));
    }
    
    public static Array<FileHandle> listOfFiles(String root) {
        return listOfFiles(new FileHandle(root));
    }
    
    public static boolean deleteAllInDir(FileHandle directory) {
        FileHandle[] list = directory.list();
        
        for (FileHandle file : list) {
            if (file.isDirectory()) {
                return delete(file);
            } else {
                return file.delete();
            }
        }
        
        return directory.exists();
    }
    
    public static boolean deleteAllInDir(File directory) {
        return deleteAllInDir(new FileHandle(directory));
    }
    
    public static boolean deleteAllInDir(String directory) {
        return deleteAllInDir(new FileHandle(directory));
    }
    
    public static boolean delete(FileHandle directory) {
        FileHandle[] files = directory.list();
        
        for (FileHandle file : files) {
            if (file.isDirectory()) {
                return deleteAllInDir(file);
            } else {
                return file.delete();
            }
        }
        
        return directory.delete();
    }
    
    public static boolean delete(File directory) {
        return delete(new FileHandle(directory));
    }
    
    public static boolean delete(String directory) {
        return delete(new FileHandle(directory));
    }
    
    public static boolean createOrExistsDir(FileHandle directory) {
        if (directory.exists()) {
            return false;
        }
        
        return directory.mkdirs();
    }
    
    public static boolean createOrExistsDir(File directory) {
        if (directory.exists()) {
            return false;
        }
        
        return directory.mkdirs();
    }
    
    public static boolean createOrExistsDir(String directory) {
        return createOrExistsDir(new FileHandle(directory));
    }
    
    public static void createFileByDeleteOldFile(FileHandle file) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        
        file.createNewFile();
    }
    
    public static void createFileByDeleteOldFile(File file) throws IOException {
        createFileByDeleteOldFile(new FileHandle(file));
    }
    
    public static void createFileByDeleteOldFile(String file) throws IOException {
        createFileByDeleteOldFile(new FileHandle(file));
    }
}
