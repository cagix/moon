(ns cdq.create.cursors
  (:require cdq.graphics.pixmap
            cdq.utils
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]))

(def config
  {:cursors/bag                   ["bag001"       [0   0]]
   :cursors/black-x               ["black_x"      [0   0]]
   :cursors/default               ["default"      [0   0]]
   :cursors/denied                ["denied"       [16 16]]
   :cursors/hand-before-grab      ["hand004"      [4  16]]
   :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
   :cursors/hand-grab             ["hand003"      [4  16]]
   :cursors/move-window           ["move002"      [16 16]]
   :cursors/no-skill-selected     ["denied003"    [0   0]]
   :cursors/over-button           ["hand002"      [0   0]]
   :cursors/sandclock             ["sandclock"    [16 16]]
   :cursors/skill-not-usable      ["x007"         [0   0]]
   :cursors/use-skill             ["pointer004"   [0   0]]
   :cursors/walking               ["walking"      [16 16]]})

(defrecord Cursors []
  cdq.utils/Disposable
  (dispose [this]
    (run! cdq.utils/dispose (vals this))))

(defn create [_context]
  (map->Cursors
   (cdq.utils/mapvals
    (fn [[file [hotspot-x hotspot-y]]]
      (let [pixmap (cdq.graphics.pixmap/create (files/internal (str "cursors/" file ".png")))
            cursor (graphics/new-cursor pixmap hotspot-x hotspot-y)]
        (cdq.utils/dispose pixmap)
        cursor))
    config)))
