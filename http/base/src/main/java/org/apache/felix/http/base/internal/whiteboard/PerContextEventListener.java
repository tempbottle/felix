/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.whiteboard;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.felix.http.base.internal.runtime.HttpSessionAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionIdListenerInfo;
import org.apache.felix.http.base.internal.runtime.HttpSessionListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletContextListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestAttributeListenerInfo;
import org.apache.felix.http.base.internal.runtime.ServletRequestListenerInfo;
import org.apache.felix.http.base.internal.util.CollectionUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public final class PerContextEventListener implements
        HttpSessionListener,
        HttpSessionAttributeListener,
        ServletContextAttributeListener,
        ServletRequestListener,
        ServletRequestAttributeListener
{
    /** Servlet context listeners. */
    private final Map<ServiceReference<ServletContextListener>, ServletContextListener> contextListeners = new ConcurrentSkipListMap<ServiceReference<ServletContextListener>, ServletContextListener>();

    /** Servlet context attribute listeners. */
    private final Map<ServiceReference<ServletContextAttributeListener>, ServletContextAttributeListener> contextAttributeListeners = new ConcurrentSkipListMap<ServiceReference<ServletContextAttributeListener>, ServletContextAttributeListener>();

    /** Session attribute listeners. */
    private final Map<ServiceReference<HttpSessionAttributeListener>, HttpSessionAttributeListener> sessionAttributeListeners = new ConcurrentSkipListMap<ServiceReference<HttpSessionAttributeListener>, HttpSessionAttributeListener>();

    /** Session listeners. */
    private final Map<ServiceReference<HttpSessionListener>, HttpSessionListener> sessionListeners = new ConcurrentSkipListMap<ServiceReference<HttpSessionListener>, HttpSessionListener>();

    /** Session id listeners. */
    private final Map<ServiceReference<HttpSessionIdListener>, HttpSessionIdListener> sessionIdListeners = new ConcurrentSkipListMap<ServiceReference<HttpSessionIdListener>, HttpSessionIdListener>();

    /** Request listeners. */
    private final Map<ServiceReference<ServletRequestListener>, ServletRequestListener> requestListeners = new ConcurrentSkipListMap<ServiceReference<ServletRequestListener>, ServletRequestListener>();

    /** Request attribute listeners. */
    private final Map<ServiceReference<ServletRequestAttributeListener>, ServletRequestAttributeListener> requestAttributeListeners = new ConcurrentSkipListMap<ServiceReference<ServletRequestAttributeListener>, ServletRequestAttributeListener>();

    private final Bundle bundle;

    private final ContextHandler contextHandler;

    PerContextEventListener(final Bundle bundle, final ContextHandler contextHandler)
    {
        this.bundle = bundle;
        this.contextHandler = contextHandler;
    }

    void initialized(@Nonnull final ServletContextListenerInfo listenerInfo)
    {
        final ServletContextListener listener = listenerInfo.getService(bundle);
        if (listener != null)
        {
            this.contextListeners.put(listenerInfo.getServiceReference(), listener);

            final ServletContext context = contextHandler
                    .getServletContext(listenerInfo.getServiceReference()
                            .getBundle());

            listener.contextInitialized(new ServletContextEvent(context));
        }
    }

    void destroyed(@Nonnull final ServletContextListenerInfo listenerInfo)
    {
        final ServiceReference<ServletContextListener> listenerRef = listenerInfo
                .getServiceReference();
        final ServletContextListener listener = this.contextListeners
                .remove(listenerRef);
        if (listener != null)
        {
            final ServletContext context = contextHandler
                    .getServletContext(listenerRef.getBundle());
            listener.contextDestroyed(new ServletContextEvent(context));
            // call unget twice, once for the call in initialized and once for
            // the call in this method(!)
            contextHandler.ungetServletContext(listenerRef.getBundle());
            contextHandler.ungetServletContext(listenerRef.getBundle());
            listenerInfo.ungetService(bundle, listener);
        }
    }

    /**
     * Add servlet context attribute listener
     *
     * @param info
     */
    void addListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        final ServletContextAttributeListener service = info.getService(bundle);
        if (service != null)
        {
            this.contextAttributeListeners.put(info.getServiceReference(),
                    service);
        }
    }

    /**
     * Remove servlet context attribute listener
     *
     * @param info
     */
    void removeListener(@Nonnull final ServletContextAttributeListenerInfo info)
    {
        final ServletContextAttributeListener service = this.contextAttributeListeners
                .remove(info.getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add session attribute listener
     *
     * @param info
     */
    void addListener(@Nonnull final HttpSessionAttributeListenerInfo info)
    {
        final HttpSessionAttributeListener service = info.getService(bundle);
        if (service != null)
        {
            this.sessionAttributeListeners.put(info.getServiceReference(),
                    service);
        }
    }

    /**
     * Remove session attribute listener
     *
     * @param info
     */
    void removeListener(@Nonnull final HttpSessionAttributeListenerInfo info)
    {
        final HttpSessionAttributeListener service = this.sessionAttributeListeners
                .remove(info.getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add session listener
     *
     * @param info
     */
    void addListener(@Nonnull final HttpSessionListenerInfo info)
    {
        final HttpSessionListener service = info.getService(bundle);
        if (service != null)
        {
            this.sessionListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove session listener
     *
     * @param info
     */
    void removeListener(@Nonnull final HttpSessionListenerInfo info)
    {
        final HttpSessionListener service = this.sessionListeners.remove(info
                .getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add session id listener
     *
     * @param info
     */
    void addListener(@Nonnull final HttpSessionIdListenerInfo info)
    {
        final HttpSessionIdListener service = info.getService(bundle);
        if (service != null)
        {
            this.sessionIdListeners.put(info.getServiceReference(),
                    service);
        }
    }

    /**
     * Remove session id listener
     *
     * @param info
     */
    void removeListener(@Nonnull final HttpSessionIdListenerInfo info)
    {
        final HttpSessionIdListener service = this.sessionIdListeners.remove(info.getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add request listener
     *
     * @param info
     */
    void addListener(@Nonnull final ServletRequestListenerInfo info)
    {
        final ServletRequestListener service = info.getService(bundle);
        if (service != null)
        {
            this.requestListeners.put(info.getServiceReference(), service);
        }
    }

    /**
     * Remove request listener
     *
     * @param info
     */
    void removeListener(@Nonnull final ServletRequestListenerInfo info)
    {
        final ServletRequestListener service = this.requestListeners
                .remove(info.getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    /**
     * Add request attribute listener
     *
     * @param info
     */
    void addListener(@Nonnull final ServletRequestAttributeListenerInfo info)
    {
        final ServletRequestAttributeListener service = info.getService(bundle);
        if (service != null)
        {
            this.requestAttributeListeners.put(info.getServiceReference(),
                    service);
        }
    }

    /**
     * Remove request attribute listener
     *
     * @param info
     */
    void removeListener(@Nonnull final ServletRequestAttributeListenerInfo info)
    {
        final ServletRequestAttributeListener service = this.requestAttributeListeners
                .remove(info.getServiceReference());
        if (service != null)
        {
            info.ungetService(bundle, service);
        }
    }

    @Override
    public void attributeReplaced(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeRemoved(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeAdded(final HttpSessionBindingEvent event)
    {
        for (final HttpSessionAttributeListener l : sessionAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeReplaced(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeRemoved(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void attributeAdded(final ServletContextAttributeEvent event)
    {
        for (final ServletContextAttributeListener l : contextAttributeListeners
                .values())
        {
            l.attributeReplaced(event);
        }
    }

    @Override
    public void sessionCreated(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.values())
        {
            l.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se)
    {
        for (final HttpSessionListener l : sessionListeners.values())
        {
            l.sessionDestroyed(se);
        }
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.values())
        {
            l.requestDestroyed(sre);
        }
    }

    @Override
    public void requestInitialized(final ServletRequestEvent sre)
    {
        for (final ServletRequestListener l : requestListeners.values())
        {
            l.requestInitialized(sre);
        }
    }

    @Override
    public void attributeAdded(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners
                .values())
        {
            l.attributeAdded(srae);
        }
    }

    @Override
    public void attributeRemoved(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners
                .values())
        {
            l.attributeRemoved(srae);
        }
    }

    @Override
    public void attributeReplaced(final ServletRequestAttributeEvent srae)
    {
        for (final ServletRequestAttributeListener l : requestAttributeListeners
                .values())
        {
            l.attributeReplaced(srae);
        }
    }

    @SuppressWarnings("unchecked")
    Collection<ServiceReference<?>> getRuntime()
    {
        return CollectionUtils.<ServiceReference<?>>sortedUnion(
                Collections.<ServiceReference<?>>reverseOrder(),
                contextListeners.keySet(),
                contextAttributeListeners.keySet(),
                sessionAttributeListeners.keySet(),
                sessionListeners.keySet(),
                requestAttributeListeners.keySet(),
                requestListeners.keySet());
    }
}
