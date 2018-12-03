package devbox.common
import Util.permsetRw
import upickle.default.{ReadWriter, macroRW}
import java.security.MessageDigest
import scala.collection.mutable

/**
  * The minimal amount of metadata identifying something on the filesystem that
  * is necessary to perform efficient synchronization.
  */
sealed trait Signature

object Signature{
  /**
    * Computes the signature of a given path. Assumes the file exists.
    */
  def compute(p: os.Path, buffer: Array[Byte]): Option[Signature] = {
    val stat = os.stat(p, followLinks = false)
    stat.fileType match{
      case os.FileType.Other => None
      case os.FileType.SymLink => Some(Symlink(os.readLink(p).toString))
      case os.FileType.Dir => Some(Dir(os.perms(p).toInt()))
      case os.FileType.File =>
        val digest = MessageDigest.getInstance("MD5")
        val chunks = mutable.ArrayBuffer.empty[Bytes]
        var size = 0L
        for((buffer, n) <- os.read.chunks(p, buffer)){
          size += n
          digest.reset()
          digest.update(buffer, 0, n)

          chunks.append(new Bytes(digest.digest()))
        }
        Some(File(os.perms(p).toInt, chunks, size))
    }
  }

  case class File(perms: os.PermSet, blockHashes: Seq[Bytes], size: Long) extends Signature
  object File{ implicit val rw: ReadWriter[File] = macroRW }

  case class Dir(perms: os.PermSet) extends Signature
  object Dir{ implicit val rw: ReadWriter[Dir] = macroRW }

  case class Symlink(dest: String) extends Signature
  object Symlink{ implicit val rw: ReadWriter[Symlink] = macroRW }

  case class Other() extends Signature
  object Other{ implicit val rw: ReadWriter[Other] = macroRW }

  implicit val rw: ReadWriter[Signature] = macroRW
}
