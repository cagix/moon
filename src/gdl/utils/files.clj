(ns gdl.utils.files
  (:require [gdl.files.file-handle :as fh]))

(defn search-by-extensions [folder extensions]
  (loop [[file & remaining] (fh/list (.internal com.badlogic.gdx.Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))
