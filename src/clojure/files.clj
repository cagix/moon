(ns clojure.files
  "Provides standard access to the filesystem, classpath, Android app storage (internal and external), and Android assets directory.")

(defprotocol Files
  (internal [_ path]
            "Convenience method that returns a Files.FileType.Internal file handle."))
