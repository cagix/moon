(ns cdq.create.cursors
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.graphics.pixmap :as pixmap]))

(def path-format "cursors/%s.png")

(def data {:cursors/bag                   ["bag001"       [0   0]]
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

(defn do!
  [ctx]
  (assoc ctx :ctx/cursors (update-vals data
                                       (fn [[file [hotspot-x hotspot-y]]]
                                         (let [pixmap (pixmap/create (files/internal (gdx/files) (format path-format file)))
                                               cursor (graphics/cursor (gdx/graphics) pixmap hotspot-x hotspot-y)]
                                           (.dispose pixmap)
                                           cursor)))))
