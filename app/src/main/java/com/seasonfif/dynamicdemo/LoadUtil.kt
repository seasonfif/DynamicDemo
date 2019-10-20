@file:Suppress("UNCHECKED_CAST")

package com.seasonfif.dynamicdemo

import android.content.Context
import android.os.Environment
import android.util.Log
import dalvik.system.DexClassLoader
import dalvik.system.PathClassLoader
import java.io.File
import java.io.File.pathSeparator


object LoadUtil {

    private val NAME_BASE_DEX_CLASS_LOADER = "dalvik.system.BaseDexClassLoader"
    private val FIELD_DEX_ELEMENTS = "dexElements"
    private val FIELD_PATH_LIST = "pathList"
    private val DEX_SUFFIX = ".dex"
    private val APK_SUFFIX = ".apk"
    private val JAR_SUFFIX = ".jar"
    private val ZIP_SUFFIX = ".zip"
    private val DEX_DIR = "patch"
    private val OPTIMIZE_DEX_DIR = "odex"

    fun <T> loadJar(context: Context) : T? {
        val sdcard = Environment.getExternalStorageDirectory().absolutePath
        val jarPath = "$sdcard/seasonfif/dex.jar"
        val tmpPath = context.applicationContext.getDir("Jar", 0).absolutePath
        val cl = DexClassLoader(jarPath, tmpPath, null, this.javaClass.classLoader)
        var instance: T? = null
        try {
            var libProviderCls = cl.loadClass("com.seasonfif.loadlib.Worker")

            instance = libProviderCls.newInstance() as T

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return instance
    }


    fun hotLoad(context: Context){
        if (context == null) {
            return
        }
        // 补丁存放目录为 /storage/emulated/0/Android/data/com.lxbnjupt.hotfixdemo/files/patch
        val dexFile = context.getExternalFilesDir(DEX_DIR)
        if (dexFile == null || !dexFile.exists()) {
            Log.e("hotLoad", "热更新补丁目录不存在")
            return
        }
        val odexFile = context.getDir(OPTIMIZE_DEX_DIR, Context.MODE_PRIVATE)
        if (!odexFile.exists()) {
            odexFile.mkdir()
        }
        val listFiles = dexFile.listFiles()
        /*if (listFiles == null || listFiles.size == 0) {
            return
        }*/

        val sdcard = Environment.getExternalStorageDirectory().absolutePath
        val dexPath = "$sdcard/seasonfif/dex.jar"
//        val dexPath = getPatchDexPath(listFiles)
        val odexPath = odexFile.absolutePath
        // 获取PathClassLoader
        val pathClassLoader = context.classLoader as PathClassLoader
        // 构建DexClassLoader，用于加载补丁dex
        val dexClassLoader = DexClassLoader(dexPath, odexPath, null, pathClassLoader)
        // 获取PathClassLoader的Element数组
        val pathElements = getDexElements(pathClassLoader)
        // 获取构建的DexClassLoader的Element数组
        val dexElements = getDexElements(dexClassLoader)
        // 合并Element数组
        val combineElementArray = combineElementArray(pathElements, dexElements)
        // 通过反射，将合并后的Element数组赋值给PathClassLoader中pathList里面的dexElements变量
        setDexElements(pathClassLoader, combineElementArray)
    }

    /**
     * 获取补丁dex文件路径集合
     * @param listFiles
     * @return
     */
    private fun getPatchDexPath(listFiles: Array<File>): String {
        val sb = StringBuilder()
        for (i in listFiles.indices) {
            // 遍历查找文件中.dex .jar .apk .zip结尾的文件
            val file = listFiles[i]
            if (file.name.endsWith(DEX_SUFFIX)
                    || file.name.endsWith(APK_SUFFIX)
                    || file.name.endsWith(JAR_SUFFIX)
                    || file.name.endsWith(ZIP_SUFFIX)) {
                if (i != 0 && i != listFiles.size - 1) {
                    // 多个dex路径 添加默认的:分隔符
                    sb.append(pathSeparator)
                }
                sb.append(file.absolutePath)
            }
        }
        return sb.toString()
    }


    /**
     * 获取Element数组
     * @param classLoader 类加载器
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Throws(ClassNotFoundException::class, NoSuchFieldException::class, IllegalAccessException::class)
    private fun getDexElements(classLoader: ClassLoader): Any {
        // 获取BaseDexClassLoader，是PathClassLoader以及DexClassLoader的父类
        val BaseDexClassLoaderClazz = Class.forName(NAME_BASE_DEX_CLASS_LOADER)
        // 获取pathList字段，并设置为可以访问
        val pathListField = BaseDexClassLoaderClazz.getDeclaredField(FIELD_PATH_LIST)
        pathListField.isAccessible = true
        // 获取DexPathList对象
        val dexPathList = pathListField.get(classLoader)
        // 获取dexElements字段，并设置为可以访问
        val dexElementsField = dexPathList.javaClass.getDeclaredField(FIELD_DEX_ELEMENTS)
        dexElementsField.isAccessible = true
        // 获取Element数组，并返回
        return dexElementsField.get(dexPathList)
    }

    /**
     * 合并Element数组，将补丁dex放在最前面
     * @param pathElements PathClassLoader中pathList里面的Element数组
     * @param dexElements 补丁dex数组
     * @return 合并之后的Element数组
     */
    private fun combineElementArray(pathElements: Any, dexElements: Any): Any {
        val componentType = pathElements.javaClass.componentType
        val i = java.lang.reflect.Array.getLength(pathElements)// 原dex数组长度
        val j = java.lang.reflect.Array.getLength(dexElements)// 补丁dex数组长度
        val k = i + j// 总数组长度（原dex数组长度 + 补丁dex数组长度)
        val result = java.lang.reflect.Array.newInstance(componentType, k)// 创建一个类型为componentType，长度为k的新数组
        System.arraycopy(dexElements, 0, result, 0, j)// 补丁dex数组在前
        System.arraycopy(pathElements, 0, result, j, i)// 原dex数组在后
        return result
    }

    /**
     * 通过反射，将合并后的Element数组赋值给PathClassLoader中pathList里面的dexElements变量
     * @param classLoader PathClassLoader类加载器
     * @param value 合并后的Element数组
     * @throws ClassNotFoundException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    @Throws(ClassNotFoundException::class, NoSuchFieldException::class, IllegalAccessException::class)
    private fun setDexElements(classLoader: ClassLoader, value: Any) {
        // 获取BaseDexClassLoader，是PathClassLoader以及DexClassLoader的父类
        val BaseDexClassLoaderClazz = Class.forName(NAME_BASE_DEX_CLASS_LOADER)
        // 获取pathList字段，并设置为可以访问
        val pathListField = BaseDexClassLoaderClazz.getDeclaredField(FIELD_PATH_LIST)
        pathListField.isAccessible = true
        // 获取DexPathList对象
        val dexPathList = pathListField.get(classLoader)
        // 获取dexElements字段，并设置为可以访问
        val dexElementsField = dexPathList.javaClass.getDeclaredField(FIELD_DEX_ELEMENTS)
        dexElementsField.isAccessible = true
        // 将合并后的Element数组赋值给dexElements变量
        dexElementsField.set(dexPathList, value)
    }
}