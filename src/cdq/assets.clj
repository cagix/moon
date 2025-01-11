(ns cdq.assets
  (:require [clojure.gdx.assets :as assets]
            [clojure.string :as str]
            [gdl.utils.files :as files]))

(defn create [{:keys [clojure/files] :as context} config]
  (assoc context
         :gdl/assets
         (assets/blocking-load-all
          (let [folder (::folder config)]
            (for [[asset-type exts] {:sound   #{"wav"}
                                     :texture #{"png" "bmp"}}
                  file (map #(str/replace-first % folder "")
                            (files/search-by-extensions files folder exts))]
              [file asset-type])))))
