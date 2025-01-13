(ns cdq.assets
  (:require [clojure.gdx.assets.manager :as asset-manager]
            [clojure.gdx.files :as files]
            [clojure.string :as str])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn manager [_context _config]
  (asset-manager/create
   (let [folder "resources/"]
     (for [[asset-type extensions] {Sound   #{"wav"}
                                    Texture #{"png" "bmp"}}
           file (map #(str/replace-first % folder "")
                     (loop [[file & remaining] (.list (files/internal folder))
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
