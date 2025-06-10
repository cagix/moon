(ns gdl.file
  (:refer-clojure :exclude [list]))

(defprotocol File
  (list [_])
  (directory? [_])
  (extension [_])
  (path [_]))

(defn recursively-search [folder extensions]
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
