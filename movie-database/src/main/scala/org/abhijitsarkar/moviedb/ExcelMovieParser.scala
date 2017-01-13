package org.abhijitsarkar.moviedb

import java.net.URL

import org.apache.poi.ss.usermodel.{DataFormatter, Row}
import org.apache.poi.xssf.usermodel.{XSSFFormulaEvaluator, XSSFWorkbook}

/**
  * @author Abhijit Sarkar
  */
trait ExcelMovieParser extends MovieParser {
  override def parseMovies(url: URL): Seq[(String, String)] = {
    import scala.collection.JavaConverters._

    val is = url.openStream()

    val wb = new XSSFWorkbook(is)
    val m = (0 until wb.getNumberOfSheets)
      .map(wb.getSheetAt)
      .flatMap(_.rowIterator.asScala)
      .filter(_.getRowNum > 0)
      .map(r => {
        val title = stripExtension(getCellValue(wb, r, 0))
        (title, getCellValue(wb, r, 1))
      })

    wb.close
    is.close

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
