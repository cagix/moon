(ns cdq.assets
  (:require [cdq.utils.files :as files]
            [clojure.string :as str]
            [gdl.assets :as assets]))

(defn create [{:keys [folder
                      asset-type-extensions]} _ctx]
  (assets/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (files/recursively-search folder extensions))]
     [file asset-type])))
