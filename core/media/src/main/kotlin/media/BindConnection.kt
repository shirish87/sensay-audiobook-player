package media

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import java.util.concurrent.atomic.AtomicBoolean


abstract class ServiceBinder<T : Service> : Binder() {
    abstract fun getService(): T?
}

class BindConnection<T : Service>(
    private val context: Context,
    private val serviceClazz: Class<T>,
) {

    private var service: T? = null
    private var serviceConnection: ServiceConnection? = null
    private var isServiceBound: AtomicBoolean = AtomicBoolean(false)

    companion object {

        fun proxyServiceConnection(listeners: List<ServiceConnection?>): ServiceConnection {
            val notifyListeners = listeners.filterNotNull()

            return object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    notifyListeners.forEach { it.onServiceConnected(name, service) }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    notifyListeners.forEach { it.onServiceDisconnected(name) }
                }

                override fun onBindingDied(name: ComponentName?) {
                    notifyListeners.forEach { it.onBindingDied(name) }
                }

                override fun onNullBinding(name: ComponentName?) {
                    notifyListeners.forEach { it.onNullBinding(name) }
                }
            }
        }
    }

    // Defines callbacks for service binding, passed to bindService()
    private val internalConnection = object : ServiceConnection {

        @Suppress("UNCHECKED_CAST")
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as? ServiceBinder<T>
            this@BindConnection.service = binder?.getService()
            isServiceBound.set(true)
        }

        override fun onServiceDisconnected(className: ComponentName) = resetService()

        override fun onBindingDied(name: ComponentName?) = resetService()

        override fun onNullBinding(name: ComponentName?) = resetService()

        private fun resetService() {
            service = null
            isServiceBound.set(false)
        }
    }

    fun connect(conn: ServiceConnection?) =
        Intent(context, serviceClazz).also { intent ->
            // https://groups.google.com/g/android-developers/c/UvolZ9g7ePw/m/-3PyYzrHiMAJ
            context.startService(intent)

            serviceConnection =
                proxyServiceConnection(listOf(internalConnection, conn)).also { conn ->
                    context.bindService(
                        intent,
                        conn,
                        Context.BIND_AUTO_CREATE,
                    )
                }
        }

    fun disconnect() {
        if (isServiceBound.get()) {
            serviceConnection?.let {
                try {
                    context.unbindService(it)
                } catch (e: IllegalArgumentException) {
                    // ignore
                }
            }
        }
    }

    fun getService(): T? {
        return if (isServiceBound.get()) {
            service
        } else {
            null
        }
    }
}
