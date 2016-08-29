package name.abhijitsarkar.user.controller

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.StatusCode.int2StatusCode
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaTypes}
import name.abhijitsarkar.user.domain.User
import name.abhijitsarkar.user.repository.UserRepository._
import spray.json.{DefaultJsonProtocol, pimpAny}

object UserJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val userJsonFormat = jsonFormat5(User)

  implicit val findByNameRequestJsonFormat = jsonFormat2(FindByNameRequest)
  implicit val findByIdRequestJsonFormat = jsonFormat1(FindByIdRequest)
  implicit val userUpdateRequestJsonFormat = jsonFormat1(UserUpdateRequest)
  implicit val userCreateRequestJsonFormat = jsonFormat1(UserCreateRequest)
  implicit val userDeleteRequestJsonFormat = jsonFormat1(UserDeleteRequest)

  implicit def userMarshaller: ToResponseMarshaller[User] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { user =>
      HttpResponse(entity =
        HttpEntity(ContentType(`application/json`), user.toJson.compactPrint))
    })

  implicit def findByNameResponseMarshaller: ToResponseMarshaller[FindByNameResponse] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { response =>
      HttpResponse(status = response.statusCode.intValue, entity =
        HttpEntity(ContentType(`application/json`), response.body.toJson.compactPrint))
    })

  implicit def findByIdResponseMarshaller: ToResponseMarshaller[FindByIdResponse] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { response =>
      HttpResponse(status = response.statusCode.intValue, entity =
        HttpEntity(ContentType(`application/json`), response.body.toJson.compactPrint))
    })

  implicit def userModificationResponseMarshaller: ToResponseMarshaller[UserModificationResponse] = Marshaller.oneOf(
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { response =>
      HttpResponse(status = response.statusCode.intValue, entity =
        HttpEntity(ContentType(`application/json`), response.body.getOrElse("").toJson.compactPrint))
    })
}