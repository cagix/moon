(ns cdq.graphics.cursors
  (:require [clojure.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]
            [gdl.utils :refer [dispose mapvals]]))

(defrecord Cursors []
  gdl.utils/Disposable
  (dispose [this]
    (run! dispose (vals this))))

(defn create [{:keys [gdl/graphics]} config]
  (map->Cursors
   (mapvals (fn [[file [hotspot-x hotspot-y]]]
              (let [pixmap (pixmap/create (.internal com.badlogic.gdx.Gdx/files (str "cursors/" file ".png")))
                    cursor (graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
                (dispose pixmap)
                cursor))
            config)))
