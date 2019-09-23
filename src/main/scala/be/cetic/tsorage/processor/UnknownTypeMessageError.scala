package be.cetic.tsorage.processor

/**
  * Created by Mathieu Goeminne.
  */
case class UnknownTypeMessageError(message: Message[_]) extends Error
{

}
