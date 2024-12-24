(ns anvil.world.render.tiled-map
  (:require [anvil.world :as world]
            [anvil.world.render :as render]
            [clojure.gdx.graphics.color :as color]
            [gdl.context :refer [draw-tiled-map]]
            [gdl.graphics :as g]))

(def ^:private explored-tile-color (g/->color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter* [light-cache light-position]
  #_(reset! do-once false)
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @world/explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (world/ray-blocked? light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! world/explored-tile-corners assoc (mapv int position) true))
            color/white)))))

(defn tile-color-setter [light-position]
  (tile-color-setter* (atom {}) light-position))

(defn-impl render/tiled-map [tiled-map light-position]
  (draw-tiled-map tiled-map
                  (tile-color-setter light-position)))
