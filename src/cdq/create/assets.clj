(ns cdq.create.assets
  (:require [clojure.gdx.assets :as assets]
            [clojure.string :as str])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.files FileHandle)))

(defn create []
  (assets/create
   (let [folder "resources/"]
     (for [[asset-type extensions] {:sound   #{"wav"}
                                    :texture #{"png" "bmp"}}
           file (map #(str/replace-first % folder "")
                     (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
                            result []]
                       (cond (nil? file)
                             result

                             (.isDirectory file)
                             (recur (concat remaining (.list file)) result)

                             (extensions (.extension file))
                             (recur remaining (conj result (.path file)))

                             :else
                             (recur remaining result))))]
       [file asset-type]))))
