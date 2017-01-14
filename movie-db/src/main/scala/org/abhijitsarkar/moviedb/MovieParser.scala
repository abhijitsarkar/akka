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
    val movie = (0 until wb.getNumberOfSheets)
      .map(wb.getSheetAt)
      .flatMap(_.rowIterator.asScala)
      .filter(_.getRowNum > 0)
      .map(r => {
        val title = takeUntil(takeUntil(getCellValue(wb, r, 0), '.'), '-').trim
        val m = (title, getCellValue(wb, r, 1))

        logger.info(s"Parsed movie: $m")

        m
      })

    wb.close
    is.close

    movie
  }

  def takeUntil(title: String, i: Int) = {
    val idx = title.lastIndexOf(i)
    if (idx >= 0) title.take(idx) else title
  }

  private def getCellValue(wb: XSSFWorkbook, r: Row, i: Int) = {
    val dataFormatter = new DataFormatter
    val formulaEvaluator = new XSSFFormulaEvaluator(wb)

    val cell = r.getCell(i)
    formulaEvaluator.evaluate(cell)
    dataFormatter.formatCellValue(cell, formulaEvaluator)
  }
}