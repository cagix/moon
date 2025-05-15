(ns cdq.game.cursors
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Pixmap)))

(defn do! []
  (utils/bind-root #'ctx/cursors (utils/mapvals
                                  (fn [[file [hotspot-x hotspot-y]]]
                                    (let [pixmap (Pixmap. (.internal Gdx/files (str "cursors/" file ".png")))
                                          cursor (.newCursor Gdx/graphics pixmap hotspot-x hotspot-y)]
                                      (.dispose pixmap)
                                      cursor))
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
                                   :cursors/walking               ["walking"      [16 16]]})))
