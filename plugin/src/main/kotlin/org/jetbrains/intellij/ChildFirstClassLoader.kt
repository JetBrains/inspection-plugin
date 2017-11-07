package org.jetbrains.intellij

import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.Enumeration

class ChildFirstClassLoader(classpath: Array<URL>, parent: ClassLoader) : URLClassLoader(classpath, parent) {
    private val system: ClassLoader? = ClassLoader.getSystemClassLoader()

    @Synchronized
    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        // First, check if the class has already been loaded
        val clazz = findLoadedClass(name) ?: try {
            // checking local
            findClass(name)
        } catch (e: ClassNotFoundException) {
            loadClassFromParent(name, resolve)
        } catch (e: SecurityException) {
            loadClassFromParent(name, resolve)
        }
        if (resolve) {
            resolveClass(clazz)
        }
        return clazz
    }

    @Throws(ClassNotFoundException::class)
    private fun loadClassFromParent(name: String, resolve: Boolean): Class<*> =
            // checking parent
            // This call to loadClass may eventually call findClass
            // again, in case the parent doesn't find anything.
            try {
                super.loadClass(name, resolve)
            } catch (e: ClassNotFoundException) {
                loadClassFromSystem(name) ?: throw e
            } catch (e: SecurityException) {
                loadClassFromSystem(name) ?: throw ClassNotFoundException("", e)
            }

    @Throws(ClassNotFoundException::class)
    private fun loadClassFromSystem(name: String): Class<*>? =
            // checking system: jvm classes, endorsed, cmd classpath,
            // etc.
            system?.loadClass(name)

    override fun getResource(name: String): URL? =
            findResource(name) ?: super.getResource(name) ?: system?.getResource(name)

    @Throws(IOException::class)
    override fun getResources(name: String): Enumeration<URL> {
        /**
         * Similar to super, but local resources are enumerated before parent
         * resources
         */
        val systemUrls: Enumeration<URL>? = system?.getResources(name)
        val localUrls = findResources(name)
        val parentUrls: Enumeration<URL>? = parent?.getResources(name)

        val urls = mutableListOf<URL>()
        if (localUrls != null) {
            while (localUrls.hasMoreElements()) {
                val local = localUrls.nextElement()
                urls += local
            }
        }
        if (systemUrls != null) {
            while (systemUrls.hasMoreElements()) {
                urls += systemUrls.nextElement()
            }
        }
        if (parentUrls != null) {
            while (parentUrls.hasMoreElements()) {
                urls += parentUrls.nextElement()
            }
        }
        return object : Enumeration<URL> {
            internal var iterator = urls.iterator()

            override fun hasMoreElements(): Boolean = iterator.hasNext()

            override fun nextElement(): URL = iterator.next()
        }
    }

    override fun getResourceAsStream(name: String): InputStream? =
            try {
                getResource(name)?.openStream()
            } catch (e: IOException) {
                null
            }
}