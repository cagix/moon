(ns cdq.create.assets
  (:require [clojure.gdx.assets :as assets]
            [clojure.gdx.files.file-handle :as fh]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)))

(defn create []
  (assets/create
   (let [folder "resources/"]
     (for [[asset-type extensions] {:sound   #{"wav"}
                                    :texture #{"png" "bmp"}}
           file (map #(str/replace-first % folder "")
                     (loop [[file & remaining] (fh/list (.internal Gdx/files folder))
                            result []]
                       (cond (nil? file)
                             result

                             (fh/directory? file)
                             (recur (concat remaining (fh/list file)) result)

                             (extensions (fh/extension file))
                             (recur remaining (conj result (fh/path file)))

                             :else
                             (recur remaining result))))]
       [file asset-type]))))
