package de.leanovate.akka.testutil

import org.scalatest.mock.MockitoSugar
import scala.reflect.ClassTag
import org.mockito.ArgumentCaptor

trait RealMockitoSugar extends MockitoSugar {
  def is[A](v: A) = org.mockito.Matchers.eq(v)

  def any[A](implicit tag: ClassTag[A]): A = org.mockito.Matchers.any(tag.runtimeClass.asInstanceOf[Class[A]])

  def captor[A](implicit tag: ClassTag[A]) = ArgumentCaptor.forClass[A](tag.runtimeClass.asInstanceOf[Class[A]])
}
