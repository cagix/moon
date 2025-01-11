(ns cdq.context.assets
  (:require [gdl.assets :as assets]))

(defn create [context config]
  (assoc context
         :gdl.context/assets
         (assets/search-and-load (:clojure.gdx/files context)
                                 (:assets config))))
