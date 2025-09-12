(ns clojure.gdx.files.file-handle
  (:import (com.badlogic.gdx.files FileHandle)))

; TODO _minimal api_ !
; can then re-implement myself

(defn recursively-search [^FileHandle folder extensions]
  (loop [[^FileHandle file & remaining] (.list folder)
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))
