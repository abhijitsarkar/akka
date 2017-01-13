package org.abhijitsarkar.moviedb

import akka.event.LoggingAdapter
import akka.stream.scaladsl.Flow
import org.apache.poi.ss.usermodel.{DataFormatter, Row}
import org.apache.poi.xssf.usermodel.{XSSFFormulaEvaluator, XSSFWorkbook}

import scala.concurrent.Future

/**
  * @author Abhijit Sarkar
  */
trait MovieService extends MovieRepository with OMDbClient {
  val logger: LoggingAdapter

  def findMovie = Flow[(String, String)]
    .mapAsyncUnordered(10)(x => (movieInfo _).tupled(x))

  def persistMovie = Flow[Either[String, Movie]]
    .mapAsyncUnordered(5) {
      _ match {
        case Right(m) => logger.debug(s"Persisting movie with id: ${m.imdbId} and title: ${m.title}"); createMovie(m)
        case Left(msg) => logger.error(msg); Future.failed(new RuntimeException(msg))
      }
    }

  def parseMovies(file: String) = {
    import scala.collection.JavaConverters._

    val wb = new XSSFWorkbook(file)
    val m = (0 until wb.getNumberOfSheets)
      .map(wb.getSheetAt)
      .flatMap(_.rowIterator.asScala)
      .filter(_.getRowNum > 0)
      .map(r => {
        val title = stripExtension(getCellValue(wb, r, 0))
        (title, getCellValue(wb, r, 1))
      })

    wb.close

    logger.info(s"Parsed movie: $m")

    m
  }

  def stripExtension(title: String) = {
    val idx = title.lastIndexOf('.')
    if (idx >= 0) title.take(idx) else title
  }

  def getCellValue(wb: XSSFWorkbook, r: Row, i: Int) = {
    val dataFormatter = new DataFormatter
    val formulaEvaluator = new XSSFFormulaEvaluator(wb)

    val cell = r.getCell(i)
    formulaEvaluator.evaluate(cell)
    dataFormatter.formatCellValue(cell, formulaEvaluator)
  }
}
