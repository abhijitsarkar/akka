package org.abhijitsarkar.moviedb

import java.net.{HttpURLConnection, URL}

import org.apache.poi.ss.usermodel.{DataFormatter, Row}
import org.apache.poi.xssf.usermodel.{XSSFFormulaEvaluator, XSSFWorkbook}

/**
  * @author Abhijit Sarkar
  */
trait MovieParser {
  def parseMovies(url: URL): Seq[(String, String)]
}

object ExcelMovieParser extends MovieParser {
  override def parseMovies(url: URL): Seq[(String, String)] = {
    import scala.collection.JavaConverters._

    val is = (url.openConnection match {
      case conn: HttpURLConnection => {
        conn.setInstanceFollowRedirects(true)
        conn.setConnectTimeout(60000)
        conn.setReadTimeout(60000)
        conn
      }
      case connection => connection
    }).getInputStream

    val wb = new XSSFWorkbook(is)
    val movies = (0 until wb.getNumberOfSheets)
      .map(wb.getSheetAt)
      .flatMap(_.rowIterator.asScala)
      .filter(_.getRowNum > 0)
      .map(r => {
        // remove various dashes
        val title = takeUntil(getCellValue(wb, r, 0), List('.', '-')).trim
        val m = (title, getCellValue(wb, r, 1))

        logger.info(s"Parsed movie: $m")

        m
      })

    wb.close
    is.close

    movies
  }

  def takeUntil(title: String, i: List[Char]) = {
    i.foldLeft(title) { (acc, elem) =>
      val idx = acc.lastIndexOf(elem.toInt)
      if (idx >= 0) acc.take(idx) else acc
    }
  }

  private def getCellValue(wb: XSSFWorkbook, r: Row, i: Int) = {
    val dataFormatter = new DataFormatter
    val formulaEvaluator = new XSSFFormulaEvaluator(wb)

    val cell = r.getCell(i)
    formulaEvaluator.evaluate(cell)
    dataFormatter.formatCellValue(cell, formulaEvaluator)
  }
}