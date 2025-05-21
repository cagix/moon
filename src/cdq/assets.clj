(ns cdq.assets
  (:require [cdq.utils.files :as files]
            [clojure.string :as str]
            [gdl.assets :as assets]))

(defn create
  [_ctx
   {:keys [folder
           asset-type-extensions]}]
  (assets/create
   (for [[asset-type extensions] asset-type-extensions
         file (map #(str/replace-first % folder "")
                   (files/recursively-search folder extensions))]
     [file asset-type])))
