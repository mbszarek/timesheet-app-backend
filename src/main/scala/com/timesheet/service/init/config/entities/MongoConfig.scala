package com.timesheet.service.init.config.entities

import org.mongodb.scala.MongoClient

final case class MongoConfig(uri: String) {
  def client: MongoClient = MongoClient(s"mongodb://$uri")
}
