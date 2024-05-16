package app.vitune.android.lib.core.data.utils

class RingBuffer<T>(val size: Int, init: (index: Int) -> T) {
    private val list = MutableList(size, init)

    private var index = 0

    operator fun get(index: Int) = list.getOrNull(index)
    operator fun plusAssign(element: T) { list[index++ % size] = element }
}
