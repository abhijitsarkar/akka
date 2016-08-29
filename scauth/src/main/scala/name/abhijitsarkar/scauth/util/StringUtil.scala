package name.abhijitsarkar.scauth.util

object StringUtil {
  def isNullOrEmpty(s: String) = (s == null || s.trim.isEmpty)
}