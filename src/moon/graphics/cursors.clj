(ns moon.graphics.cursors
  (:refer-clojure :exclude [set])
  (:require [gdl.graphics :as graphics]
            [gdl.utils :as utils :refer [safe-get mapvals]]))

(def ^:private config
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

(declare ^:private cursors)

(defn init []
  (bind-root #'cursors
             (mapvals (fn [[file hotspot]]
                        (graphics/cursor (str "cursors/" file ".png") hotspot))
                      config)))

(defn dispose []
  (run! utils/dispose (vals cursors)))

(defn set [cursor-key]
  (graphics/set-cursor (safe-get cursors cursor-key)))
