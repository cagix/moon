(ns forge.graphics.cursors
  (:refer-clojure :exclude [set])
  (:require [clojure.gdx :as gdx]
            [forge.utils :refer [mapvals]])
  (:import (com.badlogic.gdx.graphics Pixmap)))

(def ^:private props
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

(defn ^:no-doc init []
  (bind-root
   #'cursors
   (mapvals (fn [[file hotspot]]
              (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                    cursor (gdx/new-cursor pixmap hotspot)]
                (.dispose pixmap)
                cursor))
            props)))

(defn ^:no-doc dispose []
  (run! gdx/dispose (vals cursors)))

(defn set [cursor-key]
  (gdx/set-cursor (safe-get cursors cursor-key)))
