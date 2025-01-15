(ns cdq.graphics
  (:require clojure.context
            [clojure.files :as files]
            [clojure.gdx.utils.viewport :as viewport]
            clojure.graphics
            clojure.graphics.color
            clojure.graphics.pixmap
            clojure.graphics.texture
            [clojure.utils :as utils]))

(defn white-pixel-texture [_context]
  (let [pixmap (doto (clojure.graphics.pixmap/create 1 1 clojure.graphics.pixmap/format-RGBA8888)
                 (clojure.graphics.pixmap/set-color clojure.graphics.color/white)
                 (clojure.graphics.pixmap/draw-pixel 0 0))
        texture (clojure.graphics.texture/create pixmap)]
    (clojure.utils/dispose pixmap)
    texture))

(defn resize-viewports [context width height]
  (viewport/update (:clojure.graphics/ui-viewport    context) width height :center-camera? true)
  (viewport/update (:clojure.graphics/world-viewport context) width height))

(defrecord Cursors []
  clojure.utils/Disposable
  (dispose [this]
    (run! clojure.utils/dispose (vals this))))

(defn cursors [config {:keys [clojure/files
                              clojure/graphics]}]
  (map->Cursors
   (clojure.utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (clojure.graphics.pixmap/create (files/internal files (str "cursors/" file ".png")))
            cursor (clojure.graphics/new-cursor graphics pixmap hotspot-x hotspot-y)]
        (clojure.utils/dispose pixmap)
        cursor))
    config)))

(defn draw-on-world-view [render-fns context]
  (clojure.context/draw-on-world-view context
                                      (fn [context]
                                        (doseq [f render-fns]
                                          (utils/req-resolve-call f context))))
  context)
