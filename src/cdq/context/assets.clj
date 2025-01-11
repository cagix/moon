(ns cdq.context.assets
  (:require [clojure.string :as str]
            [clojure.utils.files :as files]
            [gdl.assets :as assets]))

(defn create [{:keys [clojure/files] :as context} config]
  (assoc context
         :gdl.context/assets
         (assets/create
          (let [folder (::folder config)]
            (for [[asset-type exts] {:sound   #{"wav"}
                                     :texture #{"png" "bmp"}}
                  file (map #(str/replace-first % folder "")
                            (files/search-by-extensions files folder exts))]
              [file asset-type])))))
