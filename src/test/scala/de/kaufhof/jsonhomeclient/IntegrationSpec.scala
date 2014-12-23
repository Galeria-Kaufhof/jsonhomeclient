package de.kaufhof.jsonhomeclient

import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar

abstract class IntegrationSpec extends FunSpec with Matchers with BeforeAndAfterAll with MockitoSugar