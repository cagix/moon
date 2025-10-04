(ns clojure.files.file-handle
  (:refer-clojure :exclude [list]))

(defprotocol FileHandle
  (list [_])
  (directory? [_])
  (extension [_])
  (path [_]))

(defn recursively-search
  "Returns all files in the folder (a file-handle) which match the set of extensions e.g. `#{\"png\" \"bmp\"}`."
  [folder extensions]
  (loop [[file & remaining] (list folder)
         result []]
    (cond (nil? file)
          result

          (directory? file)
          (recur (concat remaining (list file)) result)

          (extensions (extension file))
          (recur remaining (conj result (path file)))

          :else
          (recur remaining result))))
