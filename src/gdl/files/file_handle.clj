(ns gdl.files.file-handle
  (:refer-clojure :exclude [list]))

(defprotocol FileHandle
  (list [_])
  (directory? [_])
  (path [_])
  (extension [_]))
