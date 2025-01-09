(ns cdq.assets
  (:require [clojure.files :as files]
            [clojure.files.file-handle :as fh]
            [clojure.string :as str] ))

(defn- search-by-extensions [folder extensions]
  (loop [[file & remaining] (fh/list folder)
         result []]
    (cond (nil? file)
          result

          (fh/directory? file)
          (recur (concat remaining (fh/list file)) result)

          (extensions (fh/extension file))
          (recur remaining (conj result (fh/path file)))

          :else
          (recur remaining result))))

(defn search [files folder]
  (for [[asset-type exts] {:sound   #{"wav"}
                           :texture #{"png" "bmp"}}
        file (map #(str/replace-first % folder "")
                  (search-by-extensions (files/internal files folder)
                                        exts))]
    [file asset-type]))
