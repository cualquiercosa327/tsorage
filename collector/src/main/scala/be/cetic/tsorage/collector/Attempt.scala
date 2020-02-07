package be.cetic.tsorage.collector

/**
 * Similar to Try, but the original value is retrieved in both Success and Failure
 * @param value
 * @tparam T
 */
sealed class Attempt[T](val value: T)

class Success[T](value: T) extends Attempt(value)
class Failure[T](value: T) extends Attempt(value)
