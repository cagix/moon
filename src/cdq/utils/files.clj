(ns cdq.utils.files
  (:require [gdl.files :as files]
            [gdl.files.file-handle :as fh]))

(defn recursively-search [folder extensions]
  (loop [[file & remaining] (fh/list (files/internal folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))
