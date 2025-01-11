(ns cdq.graphics.cursors
  (:require [clojure.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [gdl.files :as files]
            [gdl.utils :refer [dispose mapvals]]))

(defrecord Cursors []
  gdl.utils/Disposable
  (dispose [this]
    (println "Disposing cursors")
    (run! dispose (vals this))))

(defn create [{:keys [gdl/files
                      gdl/graphics]
               :as context}
              config]
  (assoc context :gdl.graphics/cursors
         (map->Cursors
          (mapvals (fn [[file [hotspot-x hotspot-y]]]
                     (let [pixmap (pixmap/create (files/internal files (str "cursors/" file ".png")))
                           cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                       (dispose pixmap)
                       cursor))
                   (::data config)))))



